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
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager;
import com.owncloud.android.db.OCCameraUploadSync;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraUploadsSyncJobService extends JobService {

    private static final String TAG = CameraUploadsSyncJobService.class.getName();

    private JobParameters mJobParameters;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Log_OC.d(TAG, "Starting job to sync camera folder");

        new CameraUploadsSyncJobTask(this).execute(jobParameters);

        return true; // True because we have a thread still running in background
    }

    private static class CameraUploadsSyncJobTask extends AsyncTask<JobParameters, Void,
            JobParameters> {

        private final JobService mCameraUploadsSyncJobService;

        private PreferenceManager.CameraUploadsConfiguration mConfig;
        private Account mAccount;
        private CameraUploadsSyncStorageManager mCameraUploadsSyncStorageManager;
        private OCCameraUploadSync mOOCCameraUploadSync;


        public CameraUploadsSyncJobTask(JobService mCameraUploadsSyncJobService) {
            this.mCameraUploadsSyncJobService = mCameraUploadsSyncJobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParams) {

            mConfig = PreferenceManager.getCameraUploadsConfiguration(mCameraUploadsSyncJobService);
            mAccount = AccountUtils.getOwnCloudAccountByName(mCameraUploadsSyncJobService, mConfig.
                    getUploadAccountName());
            mCameraUploadsSyncStorageManager = new CameraUploadsSyncStorageManager(
                    mCameraUploadsSyncJobService.getContentResolver());

            // Check if camera uploads have been disabled
            if (!mConfig.isEnabledForPictures() && !mConfig.isEnabledForVideos()) {

                cancelPeriodicJob(jobParams);

                return jobParams[0];
            }

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
            String localCameraPath = mConfig.getSourcePath();

            File localFiles[] = new File[0];

            if (localCameraPath != null) {
                File cameraFolder = new File(localCameraPath);
                localFiles = cameraFolder.listFiles();
            }

            localFiles = orderFilesByCreationTimestamp(localFiles);

            for (File localFile : localFiles) {

                handleFile(localFile);
            }

            Log_OC.d(TAG, "All files synced, finishing job");
        }

        private File[] orderFilesByCreationTimestamp(File[] localFiles) {

            Arrays.sort(localFiles, new Comparator<File>() {
                public int compare(File file1, File file2) {
                    return Long.compare(file1.lastModified(), file2.lastModified());
                }
            });

            return localFiles;
        };

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

            mOOCCameraUploadSync = mCameraUploadsSyncStorageManager.getCameraUploadSync(null, null,
                    null);

            if (mOOCCameraUploadSync == null) {
                Log_OC.d(TAG, "There's no timestamp to compare with in database yet, not continue");
            }

            // Check file timestamp
            if (isImage && localFile.lastModified() <= mOOCCameraUploadSync.getPicturesLastSync() ||
                    isVideo && localFile.lastModified() <= mOOCCameraUploadSync.getVideosLastSync()) {
                Log_OC.i(TAG, "File " + localPath + " created before period to check, ignoring");
                return;
            }

            TransferRequester requester = new TransferRequester();
            requester.uploadNewFile(
                    mCameraUploadsSyncJobService,
                    mAccount,
                    localPath,
                    remotePath,
                    mConfig.getBehaviourAfterUpload(),
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

            long picturesTimestamp = mOOCCameraUploadSync.getPicturesLastSync();
            long videosTimestamp = mOOCCameraUploadSync.getVideosLastSync();

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

            newOCCameraUploadSync.setId(mOOCCameraUploadSync.getId());

            mCameraUploadsSyncStorageManager.updateCameraUploadSync(newOCCameraUploadSync);
        }

        /**
         * Cancel the current periodic job
         * @param jobParams
         */
        private void cancelPeriodicJob(JobParameters[] jobParams) {

            int jobId = jobParams[0].getExtras().getInt(Extras.EXTRA_SYNC_CAMERA_FOLDER_JOB_ID);

            JobScheduler jobScheduler = (JobScheduler)mCameraUploadsSyncJobService.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.cancel(jobId);

            Log_OC.d(TAG, "Camera uploads disabled, cancelling the periodic job");
        };
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