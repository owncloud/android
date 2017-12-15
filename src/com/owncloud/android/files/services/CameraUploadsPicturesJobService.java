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
import com.owncloud.android.datamodel.CameraUploadPicturesStorageManager;
import com.owncloud.android.db.OCCameraUploadPicturesSync;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraUploadsPicturesJobService extends JobService {

    private static final String TAG = CameraUploadsPicturesJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Log_OC.d(TAG, "Starting job to sync pictures from camera folder");

        new CameraUploadsSyncJobTask(this).execute(jobParameters);

        return true; // True because we have a thread still running in background
    }

    private static class CameraUploadsSyncJobTask extends AsyncTask<JobParameters, Void,
            JobParameters> {

        private final JobService mCameraUploadsPicturesJobService;

        private Account mAccount;
        private CameraUploadPicturesStorageManager mCameraUploadPicturesStorageManager;
        private OCCameraUploadPicturesSync mOCCameraUploadPicturesSync;
        private String mCameraUploadsPicturesPath;
        private String mCameraUploadsSourcePath;
        private int mCameraUploadsBehaviorAfterUpload;


        public CameraUploadsSyncJobTask(JobService mCameraUploadsPicturesJobService) {
            this.mCameraUploadsPicturesJobService = mCameraUploadsPicturesJobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParams) {

            String accountName = jobParams[0].getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);
            mAccount = AccountUtils.getOwnCloudAccountByName(mCameraUploadsPicturesJobService, accountName);
            mCameraUploadPicturesStorageManager = new CameraUploadPicturesStorageManager(
                    mCameraUploadsPicturesJobService.getContentResolver());

            mCameraUploadsPicturesPath = jobParams[0].getExtras().getString(Extras.EXTRA_CAMERA_UPLOADS_PICTURES_PATH);
            mCameraUploadsSourcePath = jobParams[0].getExtras().getString(Extras.EXTRA_CAMERA_UPLOADS_SOURCE_PATH);
            mCameraUploadsBehaviorAfterUpload = jobParams[0].getExtras().
                    getInt(Extras.EXTRA_CAMERA_UPLOADS_BEHAVIOR_AFTER_UPLOAD);

            syncFiles();

            CameraUploadsConfiguration mCameraUploadsConfiguration = PreferenceManager.
                    getCameraUploadsConfiguration(mCameraUploadsPicturesJobService);

            if (!mCameraUploadsConfiguration.isEnabledForPictures()) {
                cancelPeriodicJob(jobParams[0].getJobId());
            }

            return jobParams[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            mCameraUploadsPicturesJobService.jobFinished(jobParameters, false);
        }

        /**
         * Get local files and start handling them
         */
        private void syncFiles() {

            //Get local files included in camera folder
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
         * @param localFile file to upload to the server
         */
        private synchronized void handleFile(File localFile) {

            String fileName = localFile.getName();

            String mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName);
            boolean isImage = mimeType.startsWith("image/");

            if (!isImage) {
                Log_OC.d(TAG, "Ignoring " + fileName);
                return;
            }

            if (mCameraUploadsPicturesPath == null) {
                Log_OC.d(TAG, "Camera uploads disabled for images, ignoring " + fileName);
                return;
            }

            String remotePath = mCameraUploadsPicturesPath + fileName;

            int createdBy = UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE;

            String localPath = mCameraUploadsSourcePath + File.separator + fileName;

            mOCCameraUploadPicturesSync = mCameraUploadPicturesStorageManager.getCameraUploadPicturesSync(null, null,
                    null);

            if (mOCCameraUploadPicturesSync == null) {
                Log_OC.d(TAG, "There's no timestamp to compare with in database yet, not continue");
            }

            // Check if the file was created before period to check
            if (localFile.lastModified() <= mOCCameraUploadPicturesSync.getStartPicturesSyncMs()) {
                Log_OC.i(TAG, "Picture " + localPath + " created before period to check, ignoring");
                return;
            }

            // Check if the file was created after period to check
            if (mOCCameraUploadPicturesSync.getFinishPicturesSyncMs() > 0 && localFile.lastModified() >=
                    mOCCameraUploadPicturesSync.getFinishPicturesSyncMs()) {
                Log_OC.i(TAG, "Picture " + localPath + " created after period to check, ignoring");
                updatePictureTimestamp(localFile.lastModified());
                return;
            }

            TransferRequester requester = new TransferRequester();
            requester.uploadNewFile(
                    mCameraUploadsPicturesJobService,
                    mAccount,
                    localPath,
                    remotePath,
                    mCameraUploadsBehaviorAfterUpload,
                    mimeType,
                    true,           // create parent folder if not existent
                    createdBy
            );

            // Update start synchronization timestamps once the first picture has been enqueued
            updatePictureTimestamp(localFile.lastModified());

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
         * Update pictures timestamps to upload only the pictures taken later than those timestamps
         */
        private void updatePictureTimestamp(long fileTimestamp) {

            long startPicturesSyncMs = mOCCameraUploadPicturesSync.getStartPicturesSyncMs();
            long finishPicturesSyncMs = mOCCameraUploadPicturesSync.getFinishPicturesSyncMs();

            Log_OC.d(TAG, "Updating timestamp for pictures");

            startPicturesSyncMs = fileTimestamp;

            OCCameraUploadPicturesSync newOCCameraUploadPicturesSync = new OCCameraUploadPicturesSync(startPicturesSyncMs);

            newOCCameraUploadPicturesSync.setId(mOCCameraUploadPicturesSync.getId());

            newOCCameraUploadPicturesSync.setFinishPicturesSyncMs(finishPicturesSyncMs);

            mCameraUploadPicturesStorageManager.updateCameraUploadPicturesSync(newOCCameraUploadPicturesSync);
        }

        /**
         * Cancel the periodic job
         * @param jobId id of the job to cancel
         */
        private void cancelPeriodicJob(int jobId) {

            Log_OC.d(TAG, "Camera uploads for pictures disabled, cancelling the periodic job");

            JobScheduler jobScheduler = (JobScheduler) mCameraUploadsPicturesJobService.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.cancel(jobId);
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