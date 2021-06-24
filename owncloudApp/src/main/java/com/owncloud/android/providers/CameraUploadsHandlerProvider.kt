/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.providers

import android.content.Context
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.files.services.CameraUploadsHandler

class CameraUploadsHandlerProvider(
    private val context: Context
) {
    private val cameraUploadsHandler = CameraUploadsHandler(PreferenceManager.getCameraUploadsConfiguration())

    fun updatePicturesLastSync(timestamp: Long) = cameraUploadsHandler.updatePicturesLastSync(context, timestamp)

    fun updateVideosLastSync(timestamp: Long) = cameraUploadsHandler.updateVideosLastSync(context, timestamp)

    fun schedulePictureUploadsSyncJob() {
        val configuration = PreferenceManager.getCameraUploadsConfiguration()
        if (configuration.pictureUploadsConfiguration != null) {
            cameraUploadsHandler.setCameraUploadsConfig(configuration)
            cameraUploadsHandler.scheduleCameraUploadsSyncJob(context)
        }
    }

    fun scheduleVideoUploadsSyncJob() {
        val configuration = PreferenceManager.getCameraUploadsConfiguration()
        if (configuration.videoUploadsConfiguration != null) {
            cameraUploadsHandler.setCameraUploadsConfig(configuration)
            cameraUploadsHandler.scheduleCameraUploadsSyncJob(context)
        }
    }

    fun hasCameraUploadsAttached(accountName: String): Boolean {
        val cameraUploadsConfiguration = PreferenceManager.getCameraUploadsConfiguration()

        return accountName == cameraUploadsConfiguration.pictureUploadsConfiguration?.accountName ||
                accountName == cameraUploadsConfiguration.videoUploadsConfiguration?.accountName
    }

    fun resetCameraUploadsForAccount(accountName: String) {
        val preferencesProvider = SharedPreferencesProviderImpl(context)
        val cameraUploadsConfiguration = PreferenceManager.getCameraUploadsConfiguration()

        if (accountName == cameraUploadsConfiguration.pictureUploadsConfiguration?.accountName) {
            preferencesProvider.putBoolean(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
            preferencesProvider.removePreference(key = PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH)
            preferencesProvider.removePreference(key = PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME)
        }
        if (accountName == cameraUploadsConfiguration.videoUploadsConfiguration?.accountName) {
            preferencesProvider.putBoolean(PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_ENABLED, false)
            preferencesProvider.removePreference(key = PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_PATH)
            preferencesProvider.removePreference(key = PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_ACCOUNT_NAME)
        }
    }
}
