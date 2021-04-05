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

import androidx.lifecycle.ViewModel
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.files.services.CameraUploadsHandler
import com.owncloud.android.providers.ContextProvider

class SettingsPictureUploadsViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider
) : ViewModel() {

    private val configuration = PreferenceManager.getCameraUploadsConfiguration(contextProvider.getContext())
    private val cameraUploadsHandler = CameraUploadsHandler(configuration)

    private var uploadPath: String? = null
    private var uploadSourcePath: String? = null

    fun isPictureUploadEnabled() =
        preferencesProvider.getBoolean(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)

    fun setEnablePictureUpload(value: Boolean) =
        preferencesProvider.putBoolean(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED, value)

    fun updatePicturesLastSync() = cameraUploadsHandler.updatePicturesLastSync(contextProvider.getContext(), 0)

    fun loadPictureUploadsPath() {
        uploadPath = preferencesProvider.getString(
            PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH,
            PreferenceManager.PREF__CAMERA_UPLOADS_DEFAULT_PATH
        )
    }

    fun getPictureUploadsPath() = uploadPath

    fun loadPictureUploadsSourcePath() {
        uploadSourcePath = preferencesProvider.getString(
            PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_SOURCE,
            PreferenceManager.CameraUploadsConfiguration.DEFAULT_SOURCE_PATH
        )
    }

    fun getPictureUploadsSourcePath() = uploadSourcePath
}
