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
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.pictureUploadsName
import com.owncloud.android.domain.camerauploads.usecases.GetPictureUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetPictureUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.SavePictureUploadsConfigurationUseCase
import com.owncloud.android.providers.AccountProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.ui.activity.UploadPathActivity
import com.owncloud.android.utils.FileStorageUtils.getDefaultCameraSourcePath
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class SettingsPictureUploadsViewModel(
    private val accountProvider: AccountProvider,
    private val savePictureUploadsConfigurationUseCase: SavePictureUploadsConfigurationUseCase,
    private val getPictureUploadsConfigurationStreamUseCase: GetPictureUploadsConfigurationStreamUseCase,
    private val resetPictureUploadsUseCase: ResetPictureUploadsUseCase,
    private val workManagerProvider: WorkManagerProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _pictureUploads: MutableLiveData<FolderBackUpConfiguration?> = MutableLiveData()
    val pictureUploads: LiveData<FolderBackUpConfiguration?> = _pictureUploads

    init {
        initPictureUploads()
    }

    private fun initPictureUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getPictureUploadsConfigurationStreamUseCase.execute(Unit).collect() { pictureUploadsConfiguration ->
                _pictureUploads.postValue(pictureUploadsConfiguration)
            }
        }
    }

    fun enablePictureUploads() {
        // Use current account as default. It should never be null. If no accounts are attached, picture uploads are hidden
        accountProvider.getCurrentOwnCloudAccount()?.name?.let { name ->
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                savePictureUploadsConfigurationUseCase.execute(
                    SavePictureUploadsConfigurationUseCase.Params(composePictureUploadsConfiguration(accountName = name))
                )
            }
        }
    }

    fun disablePictureUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            resetPictureUploadsUseCase.execute(Unit)
        }
    }

    fun useWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase.execute(
                SavePictureUploadsConfigurationUseCase.Params(
                    composePictureUploadsConfiguration(wifiOnly = wifiOnly)
                )
            )
        }
    }

    fun getPictureUploadsAccount() = _pictureUploads.value?.accountName

    fun getLoggedAccountNames(): Array<String> = accountProvider.getLoggedAccounts().map { it.name }.toTypedArray()

    fun getPictureUploadsPath() = _pictureUploads.value?.uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH

    fun getPictureUploadsSourcePath(): String? = _pictureUploads.value?.sourcePath

    fun getDefaultSourcePath(): String = getDefaultCameraSourcePath()

    fun handleSelectPictureUploadsPath(data: Intent?) {
        val folderToUpload = data?.getParcelableExtra<OCFile>(UploadPathActivity.EXTRA_FOLDER)
        folderToUpload?.remotePath?.let {
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                savePictureUploadsConfigurationUseCase.execute(
                    SavePictureUploadsConfigurationUseCase.Params(composePictureUploadsConfiguration(uploadPath = it))
                )
            }
        }
    }

    fun handleSelectAccount(accountName: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase.execute(
                SavePictureUploadsConfigurationUseCase.Params(composePictureUploadsConfiguration(accountName = accountName))
            )
        }
    }

    fun handleSelectBehaviour(behaviorString: String) {
        val behavior = FolderBackUpConfiguration.Behavior.fromString(behaviorString)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase.execute(
                SavePictureUploadsConfigurationUseCase.Params(composePictureUploadsConfiguration(behavior = behavior))
            )
        }
    }

    fun handleSelectPictureUploadsSourcePath(contentUriForTree: Uri) {
        // If the source path has changed, update camera uploads last sync
        var previousSourcePath = _pictureUploads.value?.sourcePath ?: getDefaultCameraSourcePath()

        previousSourcePath = previousSourcePath.trimEnd(File.separatorChar)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase.execute(
                SavePictureUploadsConfigurationUseCase.Params(
                    composePictureUploadsConfiguration(
                        sourcePath = contentUriForTree.toString(),
                        timestamp = System.currentTimeMillis().takeIf { previousSourcePath != contentUriForTree.encodedPath }
                    )
                )
            )
        }
    }

    fun schedulePictureUploads() {
        workManagerProvider.enqueueCameraUploadsWorker()
    }

    private fun composePictureUploadsConfiguration(
        accountName: String? = _pictureUploads.value?.accountName,
        uploadPath: String? = _pictureUploads.value?.uploadPath,
        wifiOnly: Boolean? = _pictureUploads.value?.wifiOnly,
        sourcePath: String? = _pictureUploads.value?.sourcePath,
        behavior: FolderBackUpConfiguration.Behavior? = _pictureUploads.value?.behavior,
        timestamp: Long? = _pictureUploads.value?.lastSyncTimestamp
    ): FolderBackUpConfiguration =
        FolderBackUpConfiguration(
            accountName = accountName ?: accountProvider.getCurrentOwnCloudAccount()!!.name,
            behavior = behavior ?: FolderBackUpConfiguration.Behavior.COPY,
            sourcePath = sourcePath.orEmpty(),
            uploadPath = uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH,
            wifiOnly = wifiOnly ?: false,
            lastSyncTimestamp = timestamp ?: System.currentTimeMillis(),
            name = _pictureUploads.value?.name ?: pictureUploadsName
        )
}
