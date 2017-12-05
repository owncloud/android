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
public class CameraUploadsHandler {

    private static final String TAG = CameraUploadsHandler.class.getSimpleName();
    private static IndexedForest<CameraUploadsHandler> mPendingCameraUploads = new IndexedForest<>();
    private static final long MILLISECONDS_INTERVAL_CAMERA_UPLOAD = 900000;

    private CameraUploadsConfiguration mCameraUploadsConfiguration; // Camera uploads configuration, set by the user
    private Context mContext;

    public CameraUploadsHandler(CameraUploadsConfiguration cameraUploadsConfiguration, Context context) {
        mCameraUploadsConfiguration = cameraUploadsConfiguration;
        mContext = context;
    }

    /**
     * Schedule a periodic job to check pictures and videos to be uploaded
     */
    public void scheduleCameraUploadsSyncJob() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                (mCameraUploadsConfiguration.isEnabledForPictures() || mCameraUploadsConfiguration.
                        isEnabledForVideos())) {

            // Initialize synchronization timestamps for pictures/videos, if needed
            initializeCameraUploadSync();

            ComponentName serviceComponent = new ComponentName(mContext,
                    CameraUploadsSyncJobService.class);
            JobInfo.Builder builder;

            int jobId = mPendingCameraUploads.buildKey(mCameraUploadsConfiguration.getUploadAccountName(),
                    mCameraUploadsConfiguration.getSourcePath()).hashCode();

            builder = new JobInfo.Builder(jobId, serviceComponent);

            builder.setPersisted(true);

            // Execute job every 15 minutes
            builder.setPeriodic(MILLISECONDS_INTERVAL_CAMERA_UPLOAD);

            // Extra data
            PersistableBundle extras = new PersistableBundle();

            extras.putInt(Extras.EXTRA_SYNC_CAMERA_FOLDER_JOB_ID, jobId);

            builder.setExtras(extras);

            Log_OC.d(TAG, "Scheduling a CameraUploadsSyncJobService");

            JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.
                    JOB_SCHEDULER_SERVICE);

            jobScheduler.schedule(builder.build());

        }
    }

    /**
     * Initialize the timestamps for upload pictures/videos. These timestamps define the start of the
     * period in which to check the pictures/videos saved, discarding those created before enabling
     * Camera Uploads feature
     */
    private void initializeCameraUploadSync() {

        // Set synchronization timestamps not needed
        if (!mCameraUploadsConfiguration.isEnabledForPictures() && !mCameraUploadsConfiguration.isEnabledForVideos()) {
            return;
        }

        // DB connection
        CameraUploadsSyncStorageManager mCameraUploadsSyncStorageManager = new
                CameraUploadsSyncStorageManager(mContext.getContentResolver());

        OCCameraUploadSync ocCameraUploadSync = mCameraUploadsSyncStorageManager.
                getCameraUploadSync(null, null, null);

        long timeStamp = System.currentTimeMillis();

        if (ocCameraUploadSync == null) { // No synchronization timestamp for pictures/videos yet

            long firstPicturesTimeStamp = mCameraUploadsConfiguration.isEnabledForPictures() ? timeStamp : 0;
            long firstVideosTimeStamp = mCameraUploadsConfiguration.isEnabledForVideos() ? timeStamp : 0;

            // Initialize synchronization timestamp for pictures or videos in database
            OCCameraUploadSync firstOcCameraUploadSync = new OCCameraUploadSync(firstPicturesTimeStamp,
                    firstVideosTimeStamp);

            Log_OC.d(TAG, "Storing synchronization timestamp in database");

            mCameraUploadsSyncStorageManager.storeCameraUploadSync(firstOcCameraUploadSync);

        } else {

            if (ocCameraUploadSync.getPicturesLastSync() != 0 &&
                    ocCameraUploadSync.getVideosLastSync() != 0) {

                // Synchronization timestamps already initialized
                return;
            }

            if (ocCameraUploadSync.getPicturesLastSync() == 0 && mCameraUploadsConfiguration.isEnabledForPictures()) {

                // Pictures synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setPicturesLastSync(timeStamp);
            }

            if (ocCameraUploadSync.getVideosLastSync() == 0 && mCameraUploadsConfiguration.isEnabledForVideos()) {

                // Videos synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setVideosLastSync(timeStamp);
            }

            mCameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }
}