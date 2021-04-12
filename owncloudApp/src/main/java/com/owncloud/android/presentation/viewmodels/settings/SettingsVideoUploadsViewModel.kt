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

package com.owncloud.android.presentation.viewmodels.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.providers.CameraUploadsHandlerProvider
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
import com.owncloud.android.ui.activity.UploadPathActivity
import java.io.File

class SettingsVideoUploadsViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val cameraUploadsHandlerProvider: CameraUploadsHandlerProvider
) : ViewModel() {

    private var uploadPath: String? = null
    private var uploadSourcePath: String? = null

    fun isVideoUploadEnabled() =
        preferencesProvider.getBoolean(PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_ENABLED, false)

    fun setEnableVideoUpload(value: Boolean) =
        preferencesProvider.putBoolean(PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_ENABLED, value)

    fun updateVideosLastSync() = cameraUploadsHandlerProvider.updateVideosLastSync(0)

    fun loadVideoUploadsPath() {
        uploadPath = preferencesProvider.getString(
            PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_PATH,
            PreferenceManager.PREF__CAMERA_UPLOADS_DEFAULT_PATH
        )
    }

    fun getVideoUploadsPath() = uploadPath

    fun loadVideoUploadsSourcePath() {
        uploadSourcePath = preferencesProvider.getString(
            PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_SOURCE,
            PreferenceManager.CameraUploadsConfiguration.getDefaultSourcePath()
        )
    }

    fun getVideoUploadsSourcePath() = uploadSourcePath

    fun handleSelectVideoUploadsPath(data: Intent?) {
        val folderToUpload = data?.getParcelableExtra<OCFile>(UploadPathActivity.EXTRA_FOLDER)
        uploadPath = folderToUpload?.remotePath
        preferencesProvider.putString(PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_PATH, uploadPath!!)
    }

    fun handleSelectVideoUploadsSourcePath(data: Intent?) {
        // If the source path has changed, update camera uploads last sync
        var previousSourcePath = uploadSourcePath

        if (previousSourcePath?.endsWith(File.separator) == true) {
            previousSourcePath = previousSourcePath.substring(0, previousSourcePath.length - 1)
        }

        if (previousSourcePath != data?.getStringExtra(LocalFolderPickerActivity.EXTRA_PATH)) {
            val currentTimeStamp = System.currentTimeMillis()
            cameraUploadsHandlerProvider.updateVideosLastSync(currentTimeStamp)
        }

        uploadSourcePath = data?.getStringExtra(LocalFolderPickerActivity.EXTRA_PATH)
        preferencesProvider.putString(PreferenceManager.PREF__CAMERA_VIDEO_UPLOADS_SOURCE, uploadSourcePath!!)
    }

    fun scheduleVideoUploadsSyncJob() = cameraUploadsHandlerProvider.scheduleVideoUploadsSyncJob()
}
