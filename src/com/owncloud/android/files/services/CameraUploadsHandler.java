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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager;
import com.owncloud.android.datamodel.OCCameraUploadSync;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.Extras;

/**
 * Schedule the periodic job responsible for camera uploads and initialize the required
 * information, as long as it matches the configuration for camera uploads
 */
public class CameraUploadsHandler {

    private static final String TAG = CameraUploadsHandler.class.getSimpleName();
    private static final long MILLISECONDS_INTERVAL_CAMERA_UPLOAD = 900000;

    // It needs to be always the same so that the previous job is removed and replaced with a new one with the recent
    // configuration
    private static final int JOB_ID_CAMERA_UPLOAD = 1;

    private CameraUploadsConfiguration mCameraUploadsConfig; // Camera uploads configuration, set by the user

    public CameraUploadsHandler(CameraUploadsConfiguration cameraUploadsConfiguration) {
        mCameraUploadsConfig = cameraUploadsConfiguration;
    }

    /**
     * Schedule a periodic job to check pictures and videos to be uploaded
     */
    public void scheduleCameraUploadsSyncJob(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // DB Connection
            CameraUploadsSyncStorageManager cameraUploadsSyncStorageManager = new
                    CameraUploadsSyncStorageManager(context.getContentResolver());

            OCCameraUploadSync ocCameraUploadSync = cameraUploadsSyncStorageManager.
                    getCameraUploadSync(null, null, null);

            // Initialize synchronization timestamps for pictures/videos, if needed
            if (ocCameraUploadSync == null ||
                    ocCameraUploadSync.getPicturesLastSync() == 0 ||
                    ocCameraUploadSync.getVideosLastSync() == 0) {

                initializeCameraUploadSync(cameraUploadsSyncStorageManager, ocCameraUploadSync);
            }

            ComponentName serviceComponent = new ComponentName(context, CameraUploadsSyncJobService.class);
            JobInfo.Builder builder;

            builder = new JobInfo.Builder(JOB_ID_CAMERA_UPLOAD, serviceComponent);

            builder.setPersisted(true);

            // Execute job every 15 minutes
            builder.setPeriodic(MILLISECONDS_INTERVAL_CAMERA_UPLOAD);

            // Extra data
            PersistableBundle extras = new PersistableBundle();

            extras.putInt(Extras.EXTRA_CAMERA_UPLOADS_SYNC_JOB_ID, JOB_ID_CAMERA_UPLOAD);

            extras.putString(Extras.EXTRA_ACCOUNT_NAME, mCameraUploadsConfig.getUploadAccountName());

            if (mCameraUploadsConfig.isEnabledForPictures()) {
                extras.putString(Extras.EXTRA_CAMERA_UPLOADS_PICTURES_PATH, mCameraUploadsConfig.
                        getUploadPathForPictures());
            }

            if (mCameraUploadsConfig.isEnabledForVideos()) {
                extras.putString(Extras.EXTRA_CAMERA_UPLOADS_VIDEOS_PATH, mCameraUploadsConfig.
                        getUploadPathForVideos());
            }

            extras.putString(Extras.EXTRA_CAMERA_UPLOADS_SOURCE_PATH, mCameraUploadsConfig.getSourcePath());

            extras.putInt(Extras.EXTRA_CAMERA_UPLOADS_BEHAVIOR_AFTER_UPLOAD, mCameraUploadsConfig.
                    getBehaviourAfterUpload());

            builder.setExtras(extras);

            Log_OC.d(TAG, "Scheduling a CameraUploadsSyncJobService");

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.schedule(builder.build());
        }
    }

    /**
     * Initialize the timestamps for upload pictures/videos. These timestamps define the start of the
     * period in which to check the pictures/videos saved, discarding those created before enabling
     * Camera Uploads feature
     */
    private void initializeCameraUploadSync(CameraUploadsSyncStorageManager cameraUploadsSyncStorageManager,
                                            OCCameraUploadSync ocCameraUploadSync) {

        // Set synchronization timestamps not needed
        if (!mCameraUploadsConfig.isEnabledForPictures() && !mCameraUploadsConfig.isEnabledForVideos()) {
            return;
        }

        long timeStamp = System.currentTimeMillis();

        if (ocCameraUploadSync == null) { // No synchronization timestamp for pictures/videos yet

            long firstPicturesTimeStamp = mCameraUploadsConfig.isEnabledForPictures() ? timeStamp : 0;
            long firstVideosTimeStamp = mCameraUploadsConfig.isEnabledForVideos() ? timeStamp : 0;

            // Initialize synchronization timestamp for pictures or videos in database
            OCCameraUploadSync firstOcCameraUploadSync = new OCCameraUploadSync(firstPicturesTimeStamp,
                    firstVideosTimeStamp);

            Log_OC.d(TAG, "Storing synchronization timestamp in database");

            cameraUploadsSyncStorageManager.storeCameraUploadSync(firstOcCameraUploadSync);

        } else {

            if (ocCameraUploadSync.getPicturesLastSync() == 0 && mCameraUploadsConfig.isEnabledForPictures()) {
                // Pictures synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setPicturesLastSync(timeStamp);
            }

            if (ocCameraUploadSync.getVideosLastSync() == 0 && mCameraUploadsConfig.isEnabledForVideos()) {
                // Videos synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setVideosLastSync(timeStamp);
            }

            cameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }

    /**
     * Update timestamp (in milliseconds) from which to start checking pictures to upload
     * @param lastSyncTimestamp
     */
    public void updatePicturesLastSync(Context context, long lastSyncTimestamp){
        // DB connection
        CameraUploadsSyncStorageManager cameraUploadsSyncStorageManager = new
                CameraUploadsSyncStorageManager(context.getContentResolver());

        OCCameraUploadSync ocCameraUploadSync = cameraUploadsSyncStorageManager.
                getCameraUploadSync(null, null, null);

        if (ocCameraUploadSync == null) {
            return;
        } else {
            ocCameraUploadSync.setPicturesLastSync(lastSyncTimestamp);
            cameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }

    /**
     * Update timestamp (in milliseconds) from which to start checking videos to upload
     * @param lastSyncTimestamp
     */
    public void updateVideosLastSync(Context context, long lastSyncTimestamp){
        // DB connection
        CameraUploadsSyncStorageManager cameraUploadsSyncStorageManager = new
                CameraUploadsSyncStorageManager(context.getContentResolver());

        OCCameraUploadSync ocCameraUploadSync = cameraUploadsSyncStorageManager.
                getCameraUploadSync(null, null, null);

        if (ocCameraUploadSync == null) {
            return;
        } else {
            ocCameraUploadSync.setVideosLastSync(lastSyncTimestamp);
            cameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }

    public void setCameraUploadsConfig(CameraUploadsConfiguration mCameraUploadsConfig) {
        this.mCameraUploadsConfig = mCameraUploadsConfig;
    }
}