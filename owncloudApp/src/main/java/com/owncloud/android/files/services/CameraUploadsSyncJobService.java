/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.files.services;

import android.accounts.Account;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager;
import com.owncloud.android.datamodel.OCCameraUploadSync;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraUploadsSyncJobService extends JobService {

    private static final String TAG = CameraUploadsSyncJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Log_OC.d(TAG, "Starting job to sync camera folder");

        new CameraUploadsSyncJobTask(this).execute(jobParameters);

        return true; // True because we have a thread still running in background
    }

    private static class CameraUploadsSyncJobTask extends AsyncTask<JobParameters, Void,
            JobParameters> {

        private final JobService mCameraUploadsSyncJobService;

        private Account mAccount;
        private CameraUploadsSyncStorageManager mCameraUploadsSyncStorageManager;
        private OCCameraUploadSync mOCCameraUploadSync;
        private String mCameraUploadsPicturesPath;
        private String mCameraUploadsVideosPath;
        private String mCameraUploadsSourcePath;
        private int mCameraUploadsBehaviorAfterUpload;


        public CameraUploadsSyncJobTask(JobService mCameraUploadsSyncJobService) {
            this.mCameraUploadsSyncJobService = mCameraUploadsSyncJobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParams) {
            // Cancel periodic job if feature is disabled
            CameraUploadsConfiguration cameraUploadsConfiguration = PreferenceManager.
                    getCameraUploadsConfiguration(mCameraUploadsSyncJobService);

            if (!cameraUploadsConfiguration.isEnabledForPictures() &&
                    !cameraUploadsConfiguration.isEnabledForVideos()) {
                cancelPeriodicJob(jobParams[0].getJobId());

                return jobParams[0];
            }

            String accountName = jobParams[0].getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);
            mAccount = AccountUtils.getOwnCloudAccountByName(mCameraUploadsSyncJobService, accountName);
            mCameraUploadsSyncStorageManager = new CameraUploadsSyncStorageManager(
                    mCameraUploadsSyncJobService.getContentResolver());

            mCameraUploadsPicturesPath = jobParams[0].getExtras().getString(Extras.EXTRA_CAMERA_UPLOADS_PICTURES_PATH);
            mCameraUploadsVideosPath = jobParams[0].getExtras().getString(Extras.EXTRA_CAMERA_UPLOADS_VIDEOS_PATH);
            mCameraUploadsSourcePath = jobParams[0].getExtras().getString(Extras.EXTRA_CAMERA_UPLOADS_SOURCE_PATH);
            mCameraUploadsBehaviorAfterUpload = jobParams[0].getExtras().
                    getInt(Extras.EXTRA_CAMERA_UPLOADS_BEHAVIOR_AFTER_UPLOAD);

            syncFiles();

            return jobParams[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            mCameraUploadsSyncJobService.jobFinished(jobParameters, false);
        }

        /**
         * Get local images and videos and start handling them
         */
        private void syncFiles() {

            //Get local images and videos
            String localCameraPath = mCameraUploadsSourcePath;

            File localFiles[] = new File[0];

            if (localCameraPath != null) {
                File cameraFolder = new File(localCameraPath);
                localFiles = cameraFolder.listFiles();
            }

            if (localFiles != null) {
                localFiles = orderFilesByCreationTimestamp(localFiles);

                for (File localFile : localFiles) {
                    handleFile(localFile);
                }
            }

            Log_OC.d(TAG, "All files synced, finishing job");
        }

        private File[] orderFilesByCreationTimestamp(File[] localFiles) {
            Arrays.sort(localFiles, (file1, file2) -> Long.compare(file1.lastModified(), file2.lastModified()));

            return localFiles;
        }

        /**
         * Request the upload of a file just created if matches the criteria of the current
         * configuration for camera uploads.
         *
         * @param localFile image or video to upload to the server
         */
        private synchronized void handleFile(File localFile) {

            String fileName = localFile.getName();

            String mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName);
            boolean isImage = mimeType.startsWith("image/");
            boolean isVideo = mimeType.startsWith("video/");

            if (!isImage && !isVideo) {
                Log_OC.d(TAG, "Ignoring " + fileName);
                return;
            }

            if (isImage && mCameraUploadsPicturesPath == null) {
                Log_OC.d(TAG, "Camera uploads disabled for images, ignoring " + fileName);
                return;
            }

            if (isVideo && mCameraUploadsVideosPath == null) {
                Log_OC.d(TAG, "Camera uploads disabled for videos, ignoring " + fileName);
                return;
            }

            String remotePath = (isImage ? mCameraUploadsPicturesPath : mCameraUploadsVideosPath) + fileName;

            int createdBy = isImage ? UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE :
                    UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_VIDEO;

            String localPath = mCameraUploadsSourcePath + File.separator + fileName;

            mOCCameraUploadSync = mCameraUploadsSyncStorageManager.getCameraUploadSync(null, null,
                    null);

            if (mOCCameraUploadSync == null) {
                Log_OC.d(TAG, "There's no timestamp to compare with in database yet, not continue");
            }

            // Check file timestamp
            if (isImage && localFile.lastModified() <= mOCCameraUploadSync.getPicturesLastSync() ||
                    isVideo && localFile.lastModified() <= mOCCameraUploadSync.getVideosLastSync()) {
                Log_OC.i(TAG, "File " + localPath + " created before period to check, ignoring");
                return;
            }

            TransferRequester requester = new TransferRequester();
            requester.uploadNewFile(
                    mCameraUploadsSyncJobService,
                    mAccount,
                    localPath,
                    remotePath,
                    mCameraUploadsBehaviorAfterUpload,
                    mimeType,
                    true,           // create parent folder if not existent
                    createdBy
            );

            // Update timestamps once the first picture/video has been enqueued
            updateTimestamps(isImage, isVideo, localFile.lastModified());

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
         * Update pictures and videos timestamps to upload only the pictures and videos taken later
         * than those timestamps
         * @param isImage true if file is an image, false otherwise
         * @param isVideo true if file is a video, false otherwise
         */
        private void updateTimestamps(boolean isImage, boolean isVideo, long fileTimestamp) {

            long picturesTimestamp = mOCCameraUploadSync.getPicturesLastSync();
            long videosTimestamp = mOCCameraUploadSync.getVideosLastSync();

            if (isImage) {

                Log_OC.d(TAG, "Updating timestamp for pictures");

                picturesTimestamp = fileTimestamp;
            }

            if (isVideo) {

                Log_OC.d(TAG, "Updating timestamp for videos");

                videosTimestamp = fileTimestamp;
            }

            OCCameraUploadSync newOCCameraUploadSync = new OCCameraUploadSync(picturesTimestamp,
                    videosTimestamp);

            newOCCameraUploadSync.setId(mOCCameraUploadSync.getId());

            mCameraUploadsSyncStorageManager.updateCameraUploadSync(newOCCameraUploadSync);
        }

        /**
         * Cancel the periodic job
         * @param jobId id of the job to cancel
         */
        private void cancelPeriodicJob(int jobId) {

            JobScheduler jobScheduler = (JobScheduler)mCameraUploadsSyncJobService.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.cancel(jobId);

            Log_OC.d(TAG, "Camera uploads disabled, cancelling the periodic job");
        }
    }

    @Override
    /**
     * Called by the system if the job is cancelled before being finished
     */
    public boolean onStopJob(JobParameters jobParameters) {

        Log_OC.d(TAG, "Job " + TAG + " was cancelled before finishing.");

        return true;
    }
}