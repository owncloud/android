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
import com.owncloud.android.db.OCCameraUploadSync;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.Extras;

/**
 * Schedule the periodic job responsible for camera uploads and initialize the required
 * information, as long as it matches the configuration for camera uploads
 */
public class CameraUploadsSyncHandler {

    private static final String TAG = CameraUploadsSyncHandler.class.getSimpleName();
    private static IndexedForest<CameraUploadsSyncHandler> mPendingCameraUploads = new IndexedForest<>();
    private static final long MILLISECONDS_INTERVAL_CAMERA_UPLOAD = 900000;

    private CameraUploadsConfiguration mCameraUploadsConfig; // Camera uploads configuration, set by the user
    private Context mContext;

    public CameraUploadsSyncHandler(CameraUploadsConfiguration cameraUploadsConfiguration, Context context) {
        mCameraUploadsConfig = cameraUploadsConfiguration;
        mContext = context;
    }

    /**
     * Schedule a periodic job to check pictures and videos to be uploaded
     */
    public void scheduleCameraUploadsSyncJob() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            updateFinishSyncTimeStamps();

            // Job scheduling not needed if camera uploads feature is disabled
            if (!mCameraUploadsConfig.isEnabledForPictures() && !mCameraUploadsConfig.isEnabledForVideos()) {
                return;
            }

            initStartSyncTimeStamps();

            ComponentName serviceComponent = new ComponentName(mContext,
                    CameraUploadsSyncJobService.class);
            JobInfo.Builder builder;

            int jobId = mPendingCameraUploads.buildKey(mCameraUploadsConfig.getUploadAccountName(),
                    mCameraUploadsConfig.getSourcePath()).hashCode();

            builder = new JobInfo.Builder(jobId, serviceComponent);

            builder.setPersisted(true);

            // Execute job every 15 minutes
            builder.setPeriodic(MILLISECONDS_INTERVAL_CAMERA_UPLOAD);

            // Extra data
            PersistableBundle extras = new PersistableBundle();

            extras.putInt(Extras.EXTRA_CAMERA_UPLOADS_SYNC_JOB_ID, jobId);

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

            JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.
                    JOB_SCHEDULER_SERVICE);

            jobScheduler.schedule(builder.build());
        }
    }

    /**
     * The timestamps initialized here define the beginning and of the period in which to upload the local
     * pictures/videos, discarding those created before enabling Camera Uploads feature
     */
    private void initStartSyncTimeStamps() {

        // DB connection
        CameraUploadsSyncStorageManager mCameraUploadsSyncStorageManager = new
                CameraUploadsSyncStorageManager(mContext.getContentResolver());

        OCCameraUploadSync ocCameraUploadSync = mCameraUploadsSyncStorageManager.
                getCameraUploadSync(null, null, null);

        long unixTimeStamp = System.currentTimeMillis();

        if (ocCameraUploadSync == null) { // No synchronization timestamps for pictures/videos yet

            long startPicturesSyncMs = mCameraUploadsConfig.isEnabledForPictures() ? unixTimeStamp : 0;
            long startVideosSyncMs = mCameraUploadsConfig.isEnabledForVideos() ? unixTimeStamp : 0;

            // Initialize synchronization timestamp for pictures or videos in database
            OCCameraUploadSync firstOcCameraUploadSync = new OCCameraUploadSync(startPicturesSyncMs, startVideosSyncMs);

            Log_OC.d(TAG, "Initializing start sync timestamp for camera uploads in database");

            mCameraUploadsSyncStorageManager.storeCameraUploadSync(firstOcCameraUploadSync);

        } else {

            if (mCameraUploadsConfig.isEnabledForPictures()) {

                // Start pictures synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setStartPicturesSyncMs(unixTimeStamp);

                Log_OC.d(TAG, "Initializing start sync timestamp for picture uploads in database");
            }

            if (mCameraUploadsConfig.isEnabledForVideos()) {

                // Start videos synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setStartVideosSyncMs(unixTimeStamp);

                Log_OC.d(TAG, "Initializing start sync timestamp for video uploads in database");
            }

            mCameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }

    /**
     * The timestamps updated here define the end of the period in which to upload the local pictures/videos,
     * discarding those created after disabling Camera Uploads feature
     */
    private void updateFinishSyncTimeStamps() {

        // DB connection
        CameraUploadsSyncStorageManager mCameraUploadsSyncStorageManager = new
                CameraUploadsSyncStorageManager(mContext.getContentResolver());

        OCCameraUploadSync ocCameraUploadSync = mCameraUploadsSyncStorageManager.
                getCameraUploadSync(null, null, null);

        long unixTimeStamp = System.currentTimeMillis();

        if (ocCameraUploadSync != null) {

            // If camera uploads have been disabled
            if (ocCameraUploadSync.getStartPicturesSyncMs() != 0 && !mCameraUploadsConfig.isEnabledForPictures()) {

                Log_OC.d(TAG, "Updating finish sync timestamp for picture uploads in database");

                ocCameraUploadSync.setFinishPicturesSyncMs(unixTimeStamp);
            }

            if (ocCameraUploadSync.getStartVideosSyncMs() != 0 && !mCameraUploadsConfig.isEnabledForVideos())  {

                Log_OC.d(TAG, "Updating finish sync timestamp for video uploads in database");

                ocCameraUploadSync.setFinishVideosSyncMs(unixTimeStamp);
            }

            // If camera uploads have been enabled again, reset finish sync timestamp
            if (ocCameraUploadSync.getFinishPicturesSyncMs() != 0 && mCameraUploadsConfig.isEnabledForPictures()) {

                Log_OC.d(TAG, "Resetting finish sync timestamp for picture uploads in database");

                ocCameraUploadSync.setFinishPicturesSyncMs(0);
            }

            if (ocCameraUploadSync.getFinishPicturesSyncMs() != 0 && mCameraUploadsConfig.isEnabledForVideos()) {

                Log_OC.d(TAG, "Resetting finish sync timestamp for video uploads in database");

                ocCameraUploadSync.setFinishVideosSyncMs(0);
            }

            mCameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }
}