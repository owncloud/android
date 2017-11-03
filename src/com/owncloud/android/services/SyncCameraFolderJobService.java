/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.services;

import android.accounts.Account;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SyncCameraFolderJobService extends JobService implements OnRemoteOperationListener {

    private static final String TAG = SyncCameraFolderJobService.class.getName();

    // To enqueue an action to be performed on a different thread than the current one
    private final Handler mHandler = new Handler();
    private ServiceConnection mOperationsServiceConnection = null;
    private OperationsService.OperationsServiceBinder mOperationsServiceBinder = null;

    // Identifier of operation in progress which result shouldn't be lost
    private long mWaitingForOpId = Long.MAX_VALUE;

    private JobParameters mJobParameters;
    private Account mAccount;

    PreferenceManager.CameraUploadsConfiguration mConfig;

    private int mPerformedOperationsCounter = 0;

    private static int MAX_RECENTS = 30;
    private static Set<String> sRecentlyUploadedFilePaths = new HashSet<>(MAX_RECENTS);

    // DB connection
    private CameraUploadsSyncStorageManager mCameraUploadsSyncStorageManager = null;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Log_OC.d(TAG, "Starting job to sync camera folder");

        mJobParameters = jobParameters;

        mConfig = PreferenceManager.getCameraUploadsConfiguration(this);

        // Check if camera uploads have been disabled
        if (!mConfig.isEnabledForPictures() && !mConfig.isEnabledForVideos()) {

            cancelPeriodicJob();

            return false;
        }

        mAccount = AccountUtils.getOwnCloudAccountByName(this, mConfig.getUploadAccountName());

        // Bind to Operations Service
        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE);

        mCameraUploadsSyncStorageManager = new CameraUploadsSyncStorageManager(getContentResolver());

        return true; // True because we have a thread still running and requesting stuff to the server
    }

    /**
     * Implements callback methods for service binding.
     */
    private class OperationsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(
                    new ComponentName(SyncCameraFolderJobService.this, OperationsService.class)
            )) {
                mOperationsServiceBinder = (OperationsService.OperationsServiceBinder) service;

                getPicturesVideosFromServer();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(
                    new ComponentName(SyncCameraFolderJobService.this, OperationsService.class)
            )) {
                Log_OC.e(TAG, "Operations service crashed");

                mOperationsServiceBinder = null;
            }
        }
    }

    /**
     * Get remote pictures and videos contained in the server using
     * {@link OperationsService.OperationsServiceBinder}
     **/
    private void getPicturesVideosFromServer() {

        // Registering to listen for operation callbacks
        mOperationsServiceBinder.addOperationListener(this, mHandler);

        if (mWaitingForOpId <= Integer.MAX_VALUE) {
            mOperationsServiceBinder.dispatchResultIfFinished((int) mWaitingForOpId, this);
        }

        // Camera uploads enabled for pictures
        if (mConfig.isEnabledForPictures()) {
            // Get remote pictures
            Intent getUploadedPicturesIntent = new Intent();
            getUploadedPicturesIntent.setAction(OperationsService.ACTION_GET_FOLDER_FILES);
            getUploadedPicturesIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH,
                    mConfig.getUploadPathForPictures());
            getUploadedPicturesIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mAccount);
            mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getUploadedPicturesIntent);
        }

        // Camera uploads enabled for videos,
        // Note: does not try to get remote videos if the upload path for videos is the same as the
        // upload path for images. In this case, remote videos have been retrieved above
        if (mConfig.isEnabledForVideos() && !mConfig.getUploadPathForPictures()
                .equals(mConfig.getUploadPathForVideos())) {
            // Get remote videos
            Intent getUploadedVideosIntent = new Intent();
            getUploadedVideosIntent.setAction(OperationsService.ACTION_GET_FOLDER_FILES);
            getUploadedVideosIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH,
                    mConfig.getUploadPathForVideos());
            getUploadedVideosIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mAccount);
            mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getUploadedVideosIntent);
        }
    }

    @Override
    // Called once for pictures (if enabled) and again for videos (if enabled)
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {

        //Get local folder images
        String localCameraPath = mConfig.getSourcePath();

        File localFiles[] = new File[0];

        if (localCameraPath != null) {
            File cameraFolder = new File(localCameraPath);
            localFiles = cameraFolder.listFiles();
        }

        if (!result.isSuccess()) {

            Log_OC.d(TAG, "Remote folder does not exist yet, uploading the files for the " +
                    "first time, if any");

            // Remote folder doesn't exist yet, first local files upload
            if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {

                for (File localFile : localFiles) {
                    handleNewFile(localFile);
                }
            }

        } else {

            ArrayList<Object> remoteObjects = result.getData();

            compareFiles(localFiles, FileStorageUtils.castObjectsIntoRemoteFiles(remoteObjects));
        }

        // We have to unbind the service to get remote images/videos and finish the job when
        // requested operations finish

        mPerformedOperationsCounter++;

        // User only requested to upload pictures
        boolean mOnlyPictures = mConfig.isEnabledForPictures() && !mConfig.isEnabledForVideos();

        // User only requested to upload videos
        boolean mOnlyVideos = mConfig.isEnabledForVideos() && !mConfig.isEnabledForPictures();

        // User requested upload both pictures and videos
        boolean mPicturesAndVideos = mConfig.isEnabledForPictures() && mConfig.isEnabledForVideos();

        // Check if requested operations have been performed
        if (mOnlyPictures && mPerformedOperationsCounter == 1 ||
                mOnlyVideos && mPerformedOperationsCounter == 1 ||
                mPicturesAndVideos && mConfig.getUploadPathForPictures().
                        equals(mConfig.getUploadPathForVideos()) &&
                        mPerformedOperationsCounter == 1 ||
                mPicturesAndVideos && mPerformedOperationsCounter == 2) {

            finish();
        }
    }

    /**
     * Compare files (images or videos) contained in local camera folder with the ones already
     * uploaded to the server and decide which files need to be uploaded
     *
     * @param localFiles  images or videos contained in local camera folder
     * @param remoteFiles images or videos already uploaded to the server
     */
    private void compareFiles(File[] localFiles, ArrayList<RemoteFile> remoteFiles) {

        Log_OC.d(TAG, "Comparing local files with already uploaded ones");

        ArrayList<OCFile> ocFiles = FileStorageUtils.
                createOCFilesFromRemoteFilesList(remoteFiles);

        for (File localFile : localFiles) {

            boolean isAlreadyUpdated = false;

            for (OCFile ocFile : ocFiles) {

                if (localFile.getName().equals(ocFile.getFileName())) {

                    isAlreadyUpdated = true;

                    break;
                }
            }

            if (!isAlreadyUpdated) {

                // Upload file
                handleNewFile(localFile);

            }
        }
    }

    /**
     * Request the upload of a file just created if matches the criteria of the current
     * configuration for camera uploads.
     *
     * @param localFile image or video to upload to the server
     */
    private synchronized void handleNewFile(File localFile) {

        String fileName = localFile.getName();

        String mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName);
        boolean isImage = mimeType.startsWith("image/");
        boolean isVideo = mimeType.startsWith("video/");

        if (!isImage && !isVideo) {
            Log_OC.d(TAG, "Ignoring " + fileName);
            return;
        }

        if (isImage && !mConfig.isEnabledForPictures()) {
            Log_OC.d(TAG, "Camera uploads disabled for images, ignoring " + fileName);
            return;
        }

        if (isVideo && !mConfig.isEnabledForVideos()) {
            Log_OC.d(TAG, "Camera uploads disabled for videos, ignoring " + fileName);
            return;
        }

        String remotePath = (isImage ? mConfig.getUploadPathForPictures() :
                mConfig.getUploadPathForVideos()) + fileName;

        int createdBy = isImage ? UploadFileOperation.CREATED_AS_PICTURE :
                UploadFileOperation.CREATED_AS_VIDEO;

        String localPath = mConfig.getSourcePath() + File.separator + fileName;

        // Check duplicated detection
        if (sRecentlyUploadedFilePaths.contains(localPath)) {
            Log_OC.i(TAG, "Duplicate detection of " + localPath + ", ignoring");
            return;
        }

        TransferRequester requester = new TransferRequester();
        requester.uploadNewFile(
                this,
                mAccount,
                localPath,
                remotePath,
                mConfig.getBehaviourAfterUpload(),
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
                        mAccount.name
                )
        );
    }

    /**
     * Unbind the service used for getting the pictures and videos from the server and notify the
     * system that the job has finished
     */
    private void finish() {

        Log_OC.d(TAG, "Finishing camera folder sync job");

        if (mOperationsServiceBinder != null) {
            mOperationsServiceBinder.removeOperationListener(this);
        }

        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }

        jobFinished(mJobParameters, false);
    }

    /**
     * Cancel the current periodic job
     */
    private void cancelPeriodicJob() {

        int jobId = mJobParameters.getExtras().getInt(Extras.EXTRA_SYNC_CAMERA_FOLDER_JOB_ID);

        JobScheduler jobScheduler = (JobScheduler)this.getSystemService(Context.
                JOB_SCHEDULER_SERVICE);

        jobScheduler.cancel(jobId);

        Log_OC.d(TAG, "Camera uploads disabled, cancelling the periodic job");
    }

    @Override
    /**
     * Called by the system if the job is cancelled before being finished
     */
    public boolean onStopJob(JobParameters jobParameters) {

        Log_OC.d(TAG, "Job " + TAG + " was cancelled before finishing.");

        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }

        return true;
    }
}