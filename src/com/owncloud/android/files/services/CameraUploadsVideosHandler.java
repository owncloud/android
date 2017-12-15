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

import com.owncloud.android.datamodel.CameraUploadVideosStorageManager;
import com.owncloud.android.db.OCCameraUploadVideosSync;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.Extras;

/**
 * Schedule the periodic job responsible for camera uploads for videos and initialize the required information, as
 * long as it matches the configuration for camera uploads
 */
public class CameraUploadsVideosHandler {

    private static final String TAG = CameraUploadsVideosHandler.class.getSimpleName();
    private static IndexedForest<CameraUploadsVideosHandler> mPendingCameraUploads = new IndexedForest<>();
    private static final long MILLISECONDS_INTERVAL_CAMERA_UPLOAD = 120000;
    private static final int CAMERA_UPLOADS_VIDEOS_JOB_ID = 2;

    private CameraUploadsConfiguration mCameraUploadsConfig; // Camera uploads configuration, set by the user
    private Context mContext;

    public CameraUploadsVideosHandler(CameraUploadsConfiguration cameraUploadsConfiguration, Context context) {
        mCameraUploadsConfig = cameraUploadsConfiguration;
        mContext = context;
    }

    /**
     * Schedule a periodic job to check videos to be uploaded
     */
    public void scheduleCameraUploadsVideosSyncJob() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Log_OC.d(TAG, "Scheduling a CameraUploadsVideosJobService");

            initStartVideosSyncTimeStamps();

            JobInfo.Builder builder;

            JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.
                    JOB_SCHEDULER_SERVICE);

            ComponentName serviceComponent = new ComponentName(mContext,
                    CameraUploadsVideosJobService.class);

            builder = new JobInfo.Builder(CAMERA_UPLOADS_VIDEOS_JOB_ID, serviceComponent);

            builder.setPersisted(true);

            // Execute job every 15 minutes
            builder.setPeriodic(MILLISECONDS_INTERVAL_CAMERA_UPLOAD);

            // Extra data
            PersistableBundle extras = new PersistableBundle();

            extras.putInt(Extras.EXTRA_CAMERA_UPLOADS_SYNC_JOB_ID, CAMERA_UPLOADS_VIDEOS_JOB_ID);

            extras.putString(Extras.EXTRA_ACCOUNT_NAME, mCameraUploadsConfig.getUploadAccountName());

            if (mCameraUploadsConfig.isEnabledForVideos()) {
                extras.putString(Extras.EXTRA_CAMERA_UPLOADS_VIDEOS_PATH, mCameraUploadsConfig.
                        getUploadPathForVideos());
            }

            extras.putString(Extras.EXTRA_CAMERA_UPLOADS_SOURCE_PATH, mCameraUploadsConfig.getSourcePath());

            extras.putInt(Extras.EXTRA_CAMERA_UPLOADS_BEHAVIOR_AFTER_UPLOAD, mCameraUploadsConfig.
                    getBehaviourAfterUpload());

            builder.setExtras(extras);

            jobScheduler.schedule(builder.build());
        }
    }

    /**
     * The timestamps initialized here define the beginning and of the period in which to upload the local videos,
     * discarding those created before enabling Camera Uploads feature
     */
    private void initStartVideosSyncTimeStamps() {

        // DB connection
        CameraUploadVideosStorageManager mCameraUploadVideosStorageManager = new
                CameraUploadVideosStorageManager(mContext.getContentResolver());

        OCCameraUploadVideosSync ocCameraUploadVideosSync = mCameraUploadVideosStorageManager.
                getCameraUploadVideosSync(null, null, null);

        long unixTimeStamp = System.currentTimeMillis();

        if (ocCameraUploadVideosSync == null) { // No synchronization timestamps for videos yet, initialize it

            long startVideosSyncMs = mCameraUploadsConfig.isEnabledForVideos() ? unixTimeStamp : 0;

            // Initialize synchronization timestamp for videos in database
            OCCameraUploadVideosSync firstOcCameraUploadVideosSync = new OCCameraUploadVideosSync(
                    startVideosSyncMs);

            Log_OC.d(TAG, "Initializing start sync timestamp for camera uploads videos in database");

            mCameraUploadVideosStorageManager.storeCameraUploadVideosSync(firstOcCameraUploadVideosSync);

        } else {

            // Update videos synchronization timestamp
            ocCameraUploadVideosSync.setStartVideosSyncMs(unixTimeStamp);

            Log_OC.d(TAG, "Initializing start sync timestamp for video uploads in database");

            mCameraUploadVideosStorageManager.updateCameraUploadVideosSync(ocCameraUploadVideosSync);
        }
    }

    /**
     * The timestamps updated here define the end of the period in which to upload the local videos, discarding those
     * created after disabling Camera Uploads feature
     */
    public void updateFinishVideosSyncTimeStamps(long timeStamp) {

        // DB connection
        CameraUploadVideosStorageManager mCameraUploadVideosStorageManager = new
                CameraUploadVideosStorageManager(mContext.getContentResolver());

        OCCameraUploadVideosSync ocCameraUploadVideosSync = mCameraUploadVideosStorageManager.
                getCameraUploadVideosSync(null, null, null);

        ocCameraUploadVideosSync.setFinishVideosSyncMs(timeStamp);

        mCameraUploadVideosStorageManager.updateCameraUploadVideosSync(ocCameraUploadVideosSync);
    }
}