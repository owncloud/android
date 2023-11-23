/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.settings.autouploads

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_UPLOADS_DEFAULT_PATH
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.videoUploadsName
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.camerauploads.usecases.GetVideoUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetVideoUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.SaveVideoUploadsConfigurationUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.usecases.GetPersonalSpaceForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.GetSpaceByIdForAccountUseCase
import com.owncloud.android.providers.AccountProvider
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.ui.activity.FolderPickerActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class SettingsVideoUploadsViewModel(
    private val accountProvider: AccountProvider,
    private val saveVideoUploadsConfigurationUseCase: SaveVideoUploadsConfigurationUseCase,
    private val getVideoUploadsConfigurationStreamUseCase: GetVideoUploadsConfigurationStreamUseCase,
    private val resetVideoUploadsUseCase: ResetVideoUploadsUseCase,
    private val getPersonalSpaceForAccountUseCase: GetPersonalSpaceForAccountUseCase,
    private val getSpaceByIdForAccountUseCase: GetSpaceByIdForAccountUseCase,
    private val workManagerProvider: WorkManagerProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val contextProvider: ContextProvider,
) : ViewModel() {

    private val _videoUploads: MutableStateFlow<FolderBackUpConfiguration?> = MutableStateFlow(null)
    val videoUploads: StateFlow<FolderBackUpConfiguration?> = _videoUploads

    private var videoUploadsSpace: OCSpace? = null

    init {
        initVideoUploads()
    }

    private fun initVideoUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getVideoUploadsConfigurationStreamUseCase(Unit).collect { videoUploadsConfiguration ->
                videoUploadsConfiguration?.accountName?.let {
                    getSpaceById(spaceId = videoUploadsConfiguration.spaceId, accountName = it)
                }
                _videoUploads.update { videoUploadsConfiguration }
            }
        }
    }

    fun enableVideoUploads() {
        // Use current account as default. It should never be null. If no accounts are attached, video uploads are hidden
        accountProvider.getCurrentOwnCloudAccount()?.name?.let { name ->
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                getPersonalSpaceForAccount(name)
                saveVideoUploadsConfigurationUseCase(
                    SaveVideoUploadsConfigurationUseCase.Params(
                        composeVideoUploadsConfiguration(
                            accountName = name,
                            spaceId = videoUploadsSpace?.id,
                        )
                    )
                )
            }
        }
    }

    fun disableVideoUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            resetVideoUploadsUseCase(Unit)
        }
    }

    fun useWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase(
                SaveVideoUploadsConfigurationUseCase.Params(composeVideoUploadsConfiguration(wifiOnly = wifiOnly))
            )
        }
    }

    fun useChargingOnly(chargingOnly: Boolean) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase(
                SaveVideoUploadsConfigurationUseCase.Params(
                    composeVideoUploadsConfiguration(chargingOnly = chargingOnly)
                )
            )
        }
    }

    fun getVideoUploadsAccount() = _videoUploads.value?.accountName

    fun getLoggedAccountNames(): Array<String> = accountProvider.getLoggedAccounts().map { it.name }.toTypedArray()

    fun getVideoUploadsPath() = _videoUploads.value?.uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH

    fun getVideoUploadsSourcePath(): String? = _videoUploads.value?.sourcePath

    fun handleSelectVideoUploadsPath(data: Intent?) {
        val folderToUpload = data?.getParcelableExtra<OCFile>(FolderPickerActivity.EXTRA_FOLDER)
        folderToUpload?.remotePath?.let {
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                getSpaceById(spaceId = folderToUpload.spaceId, accountName = folderToUpload.owner)
                saveVideoUploadsConfigurationUseCase(
                    SaveVideoUploadsConfigurationUseCase.Params(
                        composeVideoUploadsConfiguration(
                            uploadPath = it,
                            spaceId = videoUploadsSpace?.id,
                        )
                    )
                )
            }
        }
    }

    fun handleSelectAccount(accountName: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getPersonalSpaceForAccount(accountName)
            saveVideoUploadsConfigurationUseCase(
                SaveVideoUploadsConfigurationUseCase.Params(
                    composeVideoUploadsConfiguration(
                        accountName = accountName,
                        uploadPath = null,
                        spaceId = videoUploadsSpace?.id,
                    )
                )
            )
        }
    }

    fun handleSelectBehaviour(behaviorString: String) {
        val behavior = UploadBehavior.fromString(behaviorString)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase(
                SaveVideoUploadsConfigurationUseCase.Params(composeVideoUploadsConfiguration(behavior = behavior))
            )
        }
    }

    fun handleSelectVideoUploadsSourcePath(contentUriForTree: Uri) {
        // If the source path has changed, update camera uploads last sync
        val previousSourcePath = _videoUploads.value?.sourcePath?.trimEnd(File.separatorChar)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            saveVideoUploadsConfigurationUseCase(
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
        chargingOnly: Boolean? = _videoUploads.value?.chargingOnly,
        sourcePath: String? = _videoUploads.value?.sourcePath,
        behavior: UploadBehavior? = _videoUploads.value?.behavior,
        timestamp: Long? = _videoUploads.value?.lastSyncTimestamp,
        spaceId: String? = _videoUploads.value?.spaceId,
    ): FolderBackUpConfiguration =
        FolderBackUpConfiguration(
            accountName = accountName ?: accountProvider.getCurrentOwnCloudAccount()!!.name,
            behavior = behavior ?: UploadBehavior.COPY,
            sourcePath = sourcePath.orEmpty(),
            uploadPath = uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH,
            wifiOnly = wifiOnly ?: false,
            chargingOnly = chargingOnly ?: false,
            lastSyncTimestamp = timestamp ?: System.currentTimeMillis(),
            name = _videoUploads.value?.name ?: videoUploadsName,
            spaceId = spaceId,
        ).also {
            Timber.d("Video uploads configuration updated. New configuration: $it")
        }

    private fun handleSpaceName(spaceName: String?): String? {
        return if (videoUploadsSpace?.isPersonal == true) {
            contextProvider.getString(R.string.bottom_nav_personal)
        } else {
            spaceName
        }
    }

    fun getUploadPathString(): String {

        val spaceName = handleSpaceName(videoUploadsSpace?.name)
        val uploadPath = videoUploads.value?.uploadPath
        val spaceId = videoUploads.value?.spaceId

        return if (uploadPath != null) {
            if (spaceId != null) {
                "$spaceName: $uploadPath"
            } else {
                uploadPath
            }
        } else {
            if (spaceId != null) {
                "$spaceName: $PREF__CAMERA_UPLOADS_DEFAULT_PATH"
            } else {
                PREF__CAMERA_UPLOADS_DEFAULT_PATH
            }
        }
    }

    private fun getPersonalSpaceForAccount(accountName: String) {
        val result = getPersonalSpaceForAccountUseCase(
            GetPersonalSpaceForAccountUseCase.Params(
                accountName = accountName
            )
        )
        videoUploadsSpace = result
    }

    private fun getSpaceById(spaceId: String?, accountName: String) {
        val result = getSpaceByIdForAccountUseCase(
            GetSpaceByIdForAccountUseCase.Params(
                accountName = accountName,
                spaceId = spaceId
            )
        )
        videoUploadsSpace = result
    }
}
