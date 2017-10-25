/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.services.observer;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.db.PreferenceManager.InstantUploadsConfiguration;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Request the upload of a possible new picture or video taken by the camera app
 * if matches the criteria of the received configuration for instant uploads.
 */
public class InstantUploadsHandler {

    private static String TAG = InstantUploadsHandler.class.getName();

    /**
     * Because new pictures or videos are detected both from NEW_xxx_ACTION broadcast intents and
     * from a FileObserver watching the camera folder, a single picture or video might produce two
     * calls to a handleXXX method in two different instances of {@link InstantUploadsHandler}.
     *
     * {@link FileUploader} filters upload requests that are already in the queue of files to be uploaded.
     * This is enough to prevent duplications of instant uploads when network is available, since the first
     * upload request will be still in the queue or being uploaded when the second one arrives.
     *
     * Nevertheless, when network is not available, the first upload request might be retired from the
     * queue and archived as failed/delayed before the second upload request arrives to the service.
     * That will lead to two failed uploads for the same file, and this will be uploaded twice when
     * the network is recovered. Real case detected in
     * https://github.com/owncloud/android/issues/1795#issuecomment-245263247.
     *
     * Even worse: due to multithreading scheduling, it's not granted that the two events corresponding
     * to a single picture/video are received one after the other. If some pictures/videos are taken in a row,
     * FileObserver thread can call 'handleNewMediaFile(...)' action for some of them before than
     * InstantUploadsBroadcastReceiver calls 'handleNewXXXAction(...)' for the first of them. Real case
     * detected while fixing https://github.com/owncloud/android/issues/1795#issuecomment-245263247.
     *
     * Due to that, remembering just the last file detected is not enough to correctly filter out duplicated
     * detections. Next static field saves last recent requests to filter duplicated detections for a while.
     */
    private static int MAX_RECENTS = 30;
    private static Set<String> sRecentlyUploadedFilePaths = new HashSet<>(MAX_RECENTS);

    private static final int HANDLE_DELAY_IN_MS = 200;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public boolean handleNewPictureAction(
        Intent intent,
        InstantUploadsConfiguration configuration,
        Context context
    ) {
        Log_OC.i(TAG, "New photo received");

        if (!configuration.isEnabledForPictures()) {
            Log_OC.d(TAG, "Instant upload disabled for images, ignoring new picture");
            return false;
        }

        /// retrieve file data from MediaStore
        String[] CONTENT_PROJECTION = {
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE
        };

        Cursor c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);
        if (c == null || !c.moveToFirst()) {
            Log_OC.e(TAG, "Couldn't resolve given uri: " + intent.getDataString());
            if (c != null) {
                c.close();
            }
            return false;
        }
        String localPath = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA));
        String fileName = c.getString(c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
        String mimeType = c.getString(c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
        c.close();

        Log_OC.d(TAG, "Local path: " + localPath);
        return handleNewMediaFile(fileName, localPath, mimeType, true, configuration, context);

    }


    public boolean handleNewVideoAction(
        Intent intent,
        InstantUploadsConfiguration configuration,
        Context context
    ) {
        Log_OC.i(TAG, "New video received");

        if (!configuration.isEnabledForVideos()) {
            Log_OC.d(TAG, "Instant upload disabled for videos, ignoring new video");
            return false;
        }

        /// retrieve file data from MediaStore
        String[] CONTENT_PROJECTION = {
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE
        };

        Cursor c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);
        if (c == null || !c.moveToFirst()) {
            Log_OC.e(TAG, "Couldn't resolve given uri: " + intent.getDataString());
            if (c != null) {
                c.close();
            }
            return false;
        }
        String localPath = c.getString(c.getColumnIndex(MediaStore.Video.Media.DATA));
        String fileName = c.getString(c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
        String mimeType = c.getString(c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE));
        c.close();

        Log_OC.d(TAG, "Local path: " + localPath);
        return handleNewMediaFile(fileName, localPath, mimeType, false, configuration, context);
    }


    /**
     * Request the upload of a file just created if matches the criteria of the current
     * configuration for instant uploads.
     *
     * Not run immediately - it delays the actual work to a private method, so that if the camera app saves
     * a file and then renames it, the detection of the 'save' action can be silently ignored to prevent
     * a failed upload.
     *
     * @param fileName          Name of the file just created.
     * @param configuration     User configuration for instant uploads.
     * @param context           Valid Context, used to request to uploads service.
     */
    public void handleNewFile(
        final String fileName, final InstantUploadsConfiguration configuration, final Context context
    ) {

        /// delay a bit the execution to deal with possible renames of files (for instance: Google Camera);
        /// method 'handleNewFileAfterDelay' checks if the file exists in disk before starting un upload,
        /// and silently ignores files that do not exist
        mHandler.postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    handleNewFileAfterDelay(fileName, configuration, context);
                }
            },
            HANDLE_DELAY_IN_MS
        );

    }


    /**
     * Request the upload of a file just created if matches the criteria of the current
     * configuration for instant uploads.
     *
     * Discards the file is doesn't fit details of instant uploads configuration.
     *
     * @param fileName          Name of the file just created.
     * @param configuration     User configuration for instant uploads.
     * @param context           Valid Context, used to request to uploads service.
     */
    private void handleNewFileAfterDelay(
        String fileName, InstantUploadsConfiguration configuration, Context context
    ) {
        Log_OC.d(TAG, "New file " + fileName);

        /// check file type
        String mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName);
        boolean isImage = mimeType.startsWith("image/");
        boolean isVideo = mimeType.startsWith("video/");

        if (!isImage && !isVideo) {
            Log_OC.d(TAG, "Ignoring " + fileName);
            return;
        }

        if (isImage && !configuration.isEnabledForPictures()) {
            Log_OC.d(TAG, "Instant upload disabled for images, ignoring " + fileName);
            return;
        }

        if (isVideo && !configuration.isEnabledForVideos()) {
            Log_OC.d(TAG, "Instant upload disabled for videos, ignoring " + fileName);
            return;
        }

        String localPath = configuration.getSourcePath() + File.separator + fileName;
        Log_OC.d(TAG, "Local path: " + localPath);

        handleNewMediaFile(fileName, localPath, mimeType, isImage, configuration, context);
    }


    /**
     * Request the upload of a file just created if matches the criteria of the current
     * configuration for instant uploads.
     *
     * Actual work.
     *
     * @param fileName          Name of the file just created.
     * @param configuration     User configuration for instant uploads.
     * @param context           Valid Context, used to request to uploads service.
     * @return                  'True' if an upload was requested, 'false' otherwise.
     */
    private synchronized boolean handleNewMediaFile(
        String fileName,
        String localPath,
        String mimeType,
        boolean isImage,
        InstantUploadsConfiguration configuration,
        Context context
    ) {

        /// check duplicated detection
        if (sRecentlyUploadedFilePaths.contains(localPath)) {
            Log_OC.i(TAG, "Duplicate detection of " + localPath + ", ignoring");
            return false;
        }

        /// check permission to read
        int permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if (android.content.pm.PackageManager.PERMISSION_GRANTED != permissionCheck) {
            Log_OC.w(TAG, "Read external storage permission isn't granted, aborting");
            return false;
        }

        /// check the file is **still** there and really has something inside (*)
        File localFile = new File(localPath);
        if (!localFile.exists() || localFile.length() <= 0) {
            Log_OC.w(
                TAG,
                "Camera app saved an empty or temporary file, ignoring " + fileName
            );
            // Google Camera renames video files right after stop and save
            // the recording; uploading the video upload with the original
            // name would fail; this prevents it
            return false;
        }

        /// check existence of target account
        Account account = AccountUtils.getOwnCloudAccountByName(
            context,
            configuration.getUploadAccountName()
        );
        if (account == null) {
            Log_OC.w(TAG, "No account found for instant upload, aborting upload");
            return false;
        }

        /// upload!
        String remotePath =
            (isImage ?
                configuration.getUploadPathForPictures() :
                configuration.getUploadPathForVideos()
            ) +
                fileName
            ;
        int createdBy =
            isImage ?
                UploadFileOperation.CREATED_AS_INSTANT_PICTURE :
                UploadFileOperation.CREATED_AS_INSTANT_VIDEO
            ;

        TransferRequester requester = new TransferRequester();
        requester.uploadNewFile(
            context,
            account,
            localPath,
            remotePath,
            configuration.getBehaviourAfterUpload(),
            mimeType,
            true,           // create parent folder if not existent
            createdBy
        );

        if (sRecentlyUploadedFilePaths.size() >= MAX_RECENTS) {
            // remove first path inserted
            sRecentlyUploadedFilePaths.remove(sRecentlyUploadedFilePaths.iterator().next());
        }
        sRecentlyUploadedFilePaths.add(localPath);

        Log_OC.i(
            TAG,
            String.format(
                "Requested upload of %1s to %2s in %3s",
                localPath,
                remotePath,
                account.name
            )
        );
        return true;
    }
}