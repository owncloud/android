/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.camerauploads.model.CameraUploadsConfiguration
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetPictureUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetVideoUploadsUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch

class RemoveAccountDialogViewModel(
    private val getCameraUploadsConfigurationUseCase: GetCameraUploadsConfigurationUseCase,
    private val resetPictureUploadsUseCase: ResetPictureUploadsUseCase,
    private val resetVideoUploadsUseCase: ResetVideoUploadsUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    init {
        initCameraUploadsConfiguration()
    }

    private var cameraUploadsConfiguration: CameraUploadsConfiguration? = null

    private fun initCameraUploadsConfiguration() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            cameraUploadsConfiguration = getCameraUploadsConfigurationUseCase.execute(Unit).getDataOrNull()
        }
    }

    fun hasCameraUploadsAttached(accountName: String): Boolean {
        return accountName == cameraUploadsConfiguration?.pictureUploadsConfiguration?.accountName ||
                accountName == cameraUploadsConfiguration?.videoUploadsConfiguration?.accountName
    }

    fun resetCameraUploadsForAccount(accountName: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val cameraUploadsConfiguration = getCameraUploadsConfigurationUseCase.execute(Unit)

            if (accountName == cameraUploadsConfiguration.getDataOrNull()?.pictureUploadsConfiguration?.accountName) {
                resetPictureUploadsUseCase.execute(Unit)
            }
            if (accountName == cameraUploadsConfiguration.getDataOrNull()?.videoUploadsConfiguration?.accountName) {
                resetVideoUploadsUseCase.execute(Unit)
            }
        }
    }
}
