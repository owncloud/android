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

import com.owncloud.android.datamodel.CameraUploadPicturesStorageManager;
import com.owncloud.android.db.OCCameraUploadPicturesSync;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.Extras;

/**
 * Schedule the periodic job responsible for camera uploads for pictures and initialize the required information,
 * as long as it matches the configuration for camera uploads
 */
public class CameraUploadsPicturesHandler {

    private static final String TAG = CameraUploadsPicturesHandler.class.getSimpleName();
    private static final long MILLISECONDS_INTERVAL_CAMERA_UPLOADS_PICTURES = 120000;
    private static final int CAMERA_UPLOADS_PICTURES_JOB_ID = 1;

    private CameraUploadsConfiguration mCameraUploadsConfig; // Camera uploads configuration, set by the user
    private Context mContext;

    public CameraUploadsPicturesHandler(CameraUploadsConfiguration cameraUploadsConfiguration, Context context) {
        mCameraUploadsConfig = cameraUploadsConfiguration;
        mContext = context;
    }

    /**
     * Schedule a periodic job to check pictures to be uploaded
     */
    public void scheduleCameraUploadsPicturesJob() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Log_OC.d(TAG, "Scheduling a CameraUploadsPicturesJobService");

            initStartPicturesSyncTimeStamps();

            JobInfo.Builder builder;

            JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.
                    JOB_SCHEDULER_SERVICE);

            ComponentName serviceComponent = new ComponentName(mContext,
                    CameraUploadsPicturesJobService.class);

            builder = new JobInfo.Builder(CAMERA_UPLOADS_PICTURES_JOB_ID, serviceComponent);

            builder.setPersisted(true);

            // Execute job every 15 minutes
            builder.setPeriodic(MILLISECONDS_INTERVAL_CAMERA_UPLOADS_PICTURES);

            // Extra data
            PersistableBundle extras = new PersistableBundle();

            extras.putInt(Extras.EXTRA_CAMERA_UPLOADS_SYNC_JOB_ID, CAMERA_UPLOADS_PICTURES_JOB_ID);

            extras.putString(Extras.EXTRA_ACCOUNT_NAME, mCameraUploadsConfig.getUploadAccountName());

            extras.putString(Extras.EXTRA_CAMERA_UPLOADS_PICTURES_PATH, mCameraUploadsConfig.
                    getUploadPathForPictures());

            extras.putString(Extras.EXTRA_CAMERA_UPLOADS_SOURCE_PATH, mCameraUploadsConfig.getSourcePath());

            extras.putInt(Extras.EXTRA_CAMERA_UPLOADS_BEHAVIOR_AFTER_UPLOAD, mCameraUploadsConfig.
                    getBehaviourAfterUpload());

            builder.setExtras(extras);

            jobScheduler.schedule(builder.build());
        }
    }

    /**
     * The timestamps initialized here define the beginning and of the period in which to upload the local
     * pictures, discarding those created before enabling Camera Uploads feature for pictures
     */
    private void initStartPicturesSyncTimeStamps() {

        // DB connection
        CameraUploadPicturesStorageManager mCameraUploadPicturesStorageManager = new
                CameraUploadPicturesStorageManager(mContext.getContentResolver());

        OCCameraUploadPicturesSync ocCameraUploadPicturesSync = mCameraUploadPicturesStorageManager.
                getCameraUploadPicturesSync(null, null, null);

        long unixTimeStamp = System.currentTimeMillis();

        if (ocCameraUploadPicturesSync == null) { // No synchronization timestamps for pictures yet, initialize it

            long startPicturesSyncMs = mCameraUploadsConfig.isEnabledForPictures() ? unixTimeStamp : 0;

            // Initialize synchronization timestamp for pictures in database
            OCCameraUploadPicturesSync firstOcCameraUploadPicturesSync = new OCCameraUploadPicturesSync(
                    startPicturesSyncMs);

            Log_OC.d(TAG, "Initializing start sync timestamp for camera uploads pictures in database");

            mCameraUploadPicturesStorageManager.storeCameraUploadPicturesSync(firstOcCameraUploadPicturesSync);

        } else {

            // Update pictures synchronization timestamp
            ocCameraUploadPicturesSync.setStartPicturesSyncMs(unixTimeStamp);

            Log_OC.d(TAG, "Initializing start sync timestamp for picture uploads in database");

            mCameraUploadPicturesStorageManager.updateCameraUploadPicturesSync(ocCameraUploadPicturesSync);
        }
    }

    /**
     * The timestamps updated here define the end of the period in which to upload the local pictures, discarding
     * those created after disabling Camera Uploads feature
     */
    public void updateFinishPicturesSyncTimeStamps(long timeStamp) {

        // DB connection
        CameraUploadPicturesStorageManager mCameraUploadPicturesStorageManager = new
                CameraUploadPicturesStorageManager(mContext.getContentResolver());

        OCCameraUploadPicturesSync ocCameraUploadPicturesSync = mCameraUploadPicturesStorageManager.
                getCameraUploadPicturesSync(null, null, null);

        ocCameraUploadPicturesSync.setFinishPicturesSyncMs(timeStamp);

        mCameraUploadPicturesStorageManager.updateCameraUploadPicturesSync(ocCameraUploadPicturesSync);
    }
}