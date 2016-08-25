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
import android.os.FileObserver;
import android.support.v4.content.ContextCompat;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.db.PreferenceManager.InstantUploadsConfiguration;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.HashMap;

/**
 * Observer watching a folder to request the upload of new pictures or videos inside it.
 */
public class InstantUploadsObserver extends FileObserver {

    private static String TAG = InstantUploadsObserver.class.getSimpleName();

    private static int UPDATE_MASK = (
            FileObserver.CREATE | FileObserver.MODIFY | FileObserver.CLOSE_WRITE |
            FileObserver.MOVED_TO
    );

    /*
    private final static int ALL_EVENTS_EVEN_THOSE_NOT_DOCUMENTED = 0x7fffffff;   // NEVER use 0xffffffff
    */
    private final static int IN_IGNORE = 32768;

    private final Object mLock = new Object();  // to sync mConfiguration, mainly

    private InstantUploadsConfiguration mConfiguration;
    private Context mContext;
    private HashMap<String, Boolean> mObservedChildren;


    /**
     * Constructor.
     *
     * Initializes the observer to receive events about files created in the source folder
     * included in parameter 'configuration'.
     *
     *
     * @param configuration     Full configuration for instant uploads to apply, including folder to watch.
     * @param context           Used to start an operation to upload a file, when needed.
     */
    public InstantUploadsObserver(InstantUploadsConfiguration configuration, Context context) {
        super(configuration.getSourcePath(), UPDATE_MASK);

        if (context == null) {
            throw new IllegalArgumentException("NULL context argument received");
        }

        // TODO - work if camera folder doesn't exist, but is created later?

        mConfiguration = configuration;
        mContext = context;
        mObservedChildren = new HashMap<>();
    }

    /**
     * Receives and processes events about updates of the monitored folder.
     *
     * This is almost heuristic. Do no expect it works magically with any camera.
     *
     * For instance, Google Camera creates a new video file when the user enters in "video mode", before
     * start to record, and saves it empty if the user leaves recording nothing. True store. Life is magic.
     *
     * @param event     Kind of event occurred.
     * @param path      Relative path of the file referred by the event.
     */
    @Override
    public void onEvent(int event, String path) {
        Log_OC.d(TAG, "Got event " + event + " on FOLDER " + mConfiguration.getSourcePath() + " about "
            + ((path != null) ? path : "") + " (in thread '" + Thread.currentThread().getName() + "')");

        if (path != null && path.length() > 0) {
            synchronized (mLock) {
                if ((event & FileObserver.CREATE) != 0) {
                    // new file created, let's watch it; false -> not modified yet
                    mObservedChildren.put(path, false);
                }
                if ((   (event & FileObserver.MODIFY) != 0) &&
                        mObservedChildren.containsKey(path) &&
                        !mObservedChildren.get(path)
                    ) {
                    // watched file was written for the first time after creation
                    mObservedChildren.put(path, true);
                }
                if (   (event & FileObserver.CLOSE_WRITE) != 0 &&
                        mObservedChildren.containsKey(path)    &&
                        mObservedChildren.get(path)
                    ) {
                    // a file that was previously created and written has been closed;
                    // testing for FileObserver.MODIFY is needed because some apps close the video file
                    // right after creating it when the recording is started, and reopen it to write with
                    // the first chunk of video to save; for instance, Camera MX does so.
                    handleNewFile(path);
                }
                if ((event & FileObserver.MOVED_TO) != 0) {
                    // a file has been moved or renamed into the folder;
                    // for instance, Google Camera does so right after saving a video recording
                    handleNewFile(path);
                }
            }
        }

        if ((event & IN_IGNORE) != 0 &&
            (path == null || path.length() == 0)) {
            Log_OC.d(TAG, "Stopping the observance on " + mConfiguration.getSourcePath());
        }
    }


    /**
     * Request the upload of a file just created if matches the criteria of the current
     * configuration for instant uploads.
     *
     * @param fileName      Name of the file just created
     */
    private void handleNewFile(String fileName) {
        Log_OC.d(TAG, "New file " + fileName);

        /// check file type
        String mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName);
        boolean isImage = mimeType.startsWith("image/");
        boolean isVideo = mimeType.startsWith("video/");

        if (!isImage && !isVideo) {
            Log_OC.d(TAG, "Ignoring " + fileName);
        }

        if (isImage && !mConfiguration.isEnabledForPictures()) {
            Log_OC.d(TAG, "Instant upload disabled for images, ignoring " + fileName);
            return;
        }

        if (isVideo && !mConfiguration.isEnabledForVideos()) {
            Log_OC.d(TAG, "Instant upload disabled for videos, ignoring " + fileName);
            return;
        }

        /// check permission to read
        int permissionCheck = ContextCompat.checkSelfPermission(
            mContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if (android.content.pm.PackageManager.PERMISSION_GRANTED != permissionCheck) {
            Log_OC.w(TAG, "Read external storage permission isn't granted, aborting");
            return;
        }

        /// check the file is **still** there and really has something inside (*)
        String localPath = mConfiguration.getSourcePath() + File.separator + fileName;
        File localFile = new File(localPath);
        if (!localFile.exists() || localFile.length() <= 0) {
            Log_OC.w(TAG, "Camera app saved an empty or temporary file, ignoring " + fileName);
            // Google Camera renames video files right after stop and save the recording; video
            // upload with the original named will fail;  here we try to avoid it
            return;
        }

        /// check existence of target account
        Account account = AccountUtils.getOwnCloudAccountByName(
            mContext,
            mConfiguration.getUploadAccountName()
        );
        if (account == null) {
            Log_OC.w(TAG, "No account found for instant upload, aborting upload");
            return;
        }

        /// upload!
        String remotePath =
            (isImage ?
                mConfiguration.getUploadPathForPictures() :
                mConfiguration.getUploadPathForVideos()
            ) +
            File.separator + fileName
        ;
        int createdBy =
            isImage ?
                UploadFileOperation.CREATED_AS_INSTANT_PICTURE :
                UploadFileOperation.CREATED_AS_INSTANT_VIDEO
        ;

        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
            mContext,
            account,
            localPath,
            remotePath,
            mConfiguration.getBehaviourAfterUpload(),
            mimeType,
            true,           // create parent folder if not existent
            createdBy
        );
        Log_OC.i(
            TAG,
            String.format("Requested upload of %1s to %2s in %3s", localPath, remotePath,  account.name)
        );
    }

    /**
     * Returns the absolute path to the folder observed
     *
     * @return      Absolute path to folder observed
     */
    public String getSourcePath() {
        synchronized (mLock) {
            return mConfiguration.getSourcePath();
        }
    }

    /**
     * Updates the configuration for instant uploads with the one received.
     *
     * Source path of both the new and the current configurations must be the same.
     *
     * @param configuration     New configuration for instant uploads to replace the current one.
     */
    public void updateConfiguration(InstantUploadsConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("NULL configuration argument received");
        }
        synchronized (mLock) {
            if (!mConfiguration.getSourcePath().equals(configuration.getSourcePath())) {
                throw new IllegalArgumentException(
                    "Source path in new configuration must match source path in the current one"
                );
            }
            mConfiguration = configuration;
        }
    }
}
