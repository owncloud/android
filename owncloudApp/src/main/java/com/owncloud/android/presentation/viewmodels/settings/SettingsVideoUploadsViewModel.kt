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
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_UPLOADS_DEFAULT_PATH
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.videoUploadsName
import com.owncloud.android.domain.camerauploads.usecases.GetVideoUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetVideoUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.SaveVideoUploadsConfigurationUseCase
import com.owncloud.android.providers.AccountProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.ui.activity.UploadPathActivity
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class SettingsVideoUploadsViewModel(
    private val accountProvider: AccountProvider,
    private val saveVideoUploadsConfigurationUseCase: SaveVideoUploadsConfigurationUseCase,
    private val getVideoUploadsConfigurationStreamUseCase: GetVideoUploadsConfigurationStreamUseCase,
    private val resetVideoUploadsUseCase: ResetVideoUploadsUseCase,
    private val workManagerProvider: WorkManagerProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _videoUploads: MutableLiveData<FolderBackUpConfiguration?> = MutableLiveData()
    val videoUploads: LiveData<FolderBackUpConfiguration?> = _videoUploads

    init {
        initVideoUploads()
    }

    private fun initVideoUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getVideoUploadsConfigurationStreamUseCase.execute(Unit).collect { videoUploadsConfiguration ->
                _videoUploads.postValue(videoUploadsConfiguration)
            }
        }
    }

    fun enableVideoUploads() {
        // Use current account as default. It should never be null. If no accounts are attached, video uploads are hidden
        accountProvider.getCurrentOwnCloudAccount()?.name?.let { name ->
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                saveVideoUploadsConfigurationUseCase.execute(
                    SaveVideoUploadsConfigurationUseCase.Params(composeVideoUploadsConfiguration(accountName = name))
                )
            }
        }
    }

    fun disableVideoUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            resetVideoUploadsUseCase.execute(Unit)
        }
    }

    fun useWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase.execute(
                SaveVideoUploadsConfigurationUseCase.Params(
                    composeVideoUploadsConfiguration(wifiOnly = wifiOnly)
                )
            )
        }
    }

    fun getVideoUploadsAccount() = _videoUploads.value?.accountName

    fun getLoggedAccountNames(): Array<String> = accountProvider.getLoggedAccounts().map { it.name }.toTypedArray()

    fun getVideoUploadsPath() = _videoUploads.value?.uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH

    fun getVideoUploadsSourcePath(): String? = _videoUploads.value?.sourcePath

    fun getDefaultCameraSourcePath(): String = FileStorageUtils.getDefaultCameraSourcePath()

    fun handleSelectVideoUploadsPath(data: Intent?) {
        val folderToUpload = data?.getParcelableExtra<OCFile>(UploadPathActivity.EXTRA_FOLDER)
        folderToUpload?.remotePath?.let {
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                saveVideoUploadsConfigurationUseCase.execute(
                    SaveVideoUploadsConfigurationUseCase.Params(composeVideoUploadsConfiguration(uploadPath = it))
                )
            }
        }
    }

    fun handleSelectAccount(accountName: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase.execute(
                SaveVideoUploadsConfigurationUseCase.Params(composeVideoUploadsConfiguration(accountName = accountName))
            )
        }
    }

    fun handleSelectBehaviour(behaviorString: String) {
        val behavior = FolderBackUpConfiguration.Behavior.fromString(behaviorString)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase.execute(
                SaveVideoUploadsConfigurationUseCase.Params(composeVideoUploadsConfiguration(behavior = behavior))
            )
        }
    }

    fun handleSelectVideoUploadsSourcePath(contentUriForTree: Uri) {
        // If the source path has changed, update camera uploads last sync
        var previousSourcePath = _videoUploads.value?.sourcePath ?: getDefaultCameraSourcePath()

        previousSourcePath = previousSourcePath.trimEnd(File.separatorChar)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase.execute(
                SaveVideoUploadsConfigurationUseCase.Params(
                    composeVideoUploadsConfiguration(
                        sourcePath = contentUriForTree.toString(),
                        timestamp = System.currentTimeMillis().takeIf { previousSourcePath != contentUriForTree.encodedPath }
                    )
                )
            )
        }
    }

    fun scheduleVideoUploads() {
        workManagerProvider.enqueueCameraUploadsWorker()
    }

    private fun composeVideoUploadsConfiguration(
        accountName: String? = _videoUploads.value?.accountName,
        uploadPath: String? = _videoUploads.value?.uploadPath,
        wifiOnly: Boolean? = _videoUploads.value?.wifiOnly,
        sourcePath: String? = _videoUploads.value?.sourcePath,
        behavior: FolderBackUpConfiguration.Behavior? = _videoUploads.value?.behavior,
        timestamp: Long? = _videoUploads.value?.lastSyncTimestamp
    ): FolderBackUpConfiguration =
        FolderBackUpConfiguration(
            accountName = accountName ?: accountProvider.getCurrentOwnCloudAccount()!!.name,
            behavior = behavior ?: FolderBackUpConfiguration.Behavior.COPY,
            sourcePath = sourcePath.orEmpty(),
            uploadPath = uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH,
            wifiOnly = wifiOnly ?: false,
            lastSyncTimestamp = timestamp ?: System.currentTimeMillis(),
            name = _videoUploads.value?.name ?: videoUploadsName
        )
}
