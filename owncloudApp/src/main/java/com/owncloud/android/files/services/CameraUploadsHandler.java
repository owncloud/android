/*
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2017 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.files.services;

import android.content.Context;

import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager;
import com.owncloud.android.datamodel.OCCameraUploadSync;
import com.owncloud.android.domain.camerauploads.model.CameraUploadsConfiguration;
import timber.log.Timber;

/**
 * Schedule the periodic job responsible for camera uploads and initialize the required
 * information, as long as it matches the configuration for camera uploads
 */
public class CameraUploadsHandler {

    private CameraUploadsConfiguration mCameraUploadsConfig; // Camera uploads configuration, set by the user

    public CameraUploadsHandler(CameraUploadsConfiguration cameraUploadsConfiguration) {
        mCameraUploadsConfig = cameraUploadsConfiguration;
    }

    /**
     * Schedule a periodic job to check pictures and videos to be uploaded
     */
    public void scheduleCameraUploadsSyncJob(Context context) {
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
    }

    /**
     * Initialize the timestamps for upload pictures/videos. These timestamps define the start of the
     * period in which to check the pictures/videos saved, discarding those created before enabling
     * Camera Uploads feature
     */
    private void initializeCameraUploadSync(CameraUploadsSyncStorageManager cameraUploadsSyncStorageManager,
                                            OCCameraUploadSync ocCameraUploadSync) {

        boolean pictureUploadsEnabled = mCameraUploadsConfig.getPictureUploadsConfiguration() != null;
        boolean videoUploadsEnabled = mCameraUploadsConfig.getVideoUploadsConfiguration() != null;

        // Set synchronization timestamps not needed
        if (!pictureUploadsEnabled && !videoUploadsEnabled) {
            return;
        }

        long timeStamp = System.currentTimeMillis();

        if (ocCameraUploadSync == null) { // No synchronization timestamp for pictures/videos yet

            long firstPicturesTimeStamp = pictureUploadsEnabled ? timeStamp : 0;
            long firstVideosTimeStamp = videoUploadsEnabled ? timeStamp : 0;

            // Initialize synchronization timestamp for pictures or videos in database
            OCCameraUploadSync firstOcCameraUploadSync = new OCCameraUploadSync(firstPicturesTimeStamp,
                    firstVideosTimeStamp);

            Timber.d("Storing synchronization timestamp in database");

            cameraUploadsSyncStorageManager.storeCameraUploadSync(firstOcCameraUploadSync);

        } else {

            if (ocCameraUploadSync.getPicturesLastSync() == 0 && pictureUploadsEnabled) {
                // Pictures synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setPicturesLastSync(timeStamp);
            }

            if (ocCameraUploadSync.getVideosLastSync() == 0 && videoUploadsEnabled) {
                // Videos synchronization timestamp not initialized yet, initialize it
                ocCameraUploadSync.setVideosLastSync(timeStamp);
            }

            cameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }

    /**
     * Update timestamp (in milliseconds) from which to start checking pictures to upload
     *
     * @param lastSyncTimestamp
     */
    public void updatePicturesLastSync(Context context, long lastSyncTimestamp) {
        // DB connection
        CameraUploadsSyncStorageManager cameraUploadsSyncStorageManager = new
                CameraUploadsSyncStorageManager(context.getContentResolver());

        OCCameraUploadSync ocCameraUploadSync = cameraUploadsSyncStorageManager.
                getCameraUploadSync(null, null, null);

        if (ocCameraUploadSync != null) {
            ocCameraUploadSync.setPicturesLastSync(lastSyncTimestamp);
            cameraUploadsSyncStorageManager.updateCameraUploadSync(ocCameraUploadSync);
        }
    }

    /**
     * Update timestamp (in milliseconds) from which to start checking videos to upload
     *
     * @param lastSyncTimestamp
     */
    public void updateVideosLastSync(Context context, long lastSyncTimestamp) {
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
