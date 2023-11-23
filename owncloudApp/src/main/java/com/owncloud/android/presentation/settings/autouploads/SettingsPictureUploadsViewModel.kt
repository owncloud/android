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
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.pictureUploadsName
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.camerauploads.usecases.GetPictureUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetPictureUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.SavePictureUploadsConfigurationUseCase
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

class SettingsPictureUploadsViewModel(
    private val accountProvider: AccountProvider,
    private val savePictureUploadsConfigurationUseCase: SavePictureUploadsConfigurationUseCase,
    private val getPictureUploadsConfigurationStreamUseCase: GetPictureUploadsConfigurationStreamUseCase,
    private val resetPictureUploadsUseCase: ResetPictureUploadsUseCase,
    private val getPersonalSpaceForAccountUseCase: GetPersonalSpaceForAccountUseCase,
    private val getSpaceByIdForAccountUseCase: GetSpaceByIdForAccountUseCase,
    private val workManagerProvider: WorkManagerProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val contextProvider: ContextProvider,
) : ViewModel() {

    private val _pictureUploads: MutableStateFlow<FolderBackUpConfiguration?> = MutableStateFlow(null)
    val pictureUploads: StateFlow<FolderBackUpConfiguration?> = _pictureUploads

    private var pictureUploadsSpace: OCSpace? = null

    init {
        initPictureUploads()
    }

    private fun initPictureUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getPictureUploadsConfigurationStreamUseCase(Unit).collect { pictureUploadsConfiguration ->
                pictureUploadsConfiguration?.accountName?.let {
                    getSpaceById(spaceId = pictureUploadsConfiguration.spaceId, accountName = it)
                }
                _pictureUploads.update { pictureUploadsConfiguration }
            }
        }
    }

    fun enablePictureUploads() {
        // Use current account as default. It should never be null. If no accounts are attached, picture uploads are hidden
        accountProvider.getCurrentOwnCloudAccount()?.name?.let { name ->
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                getPersonalSpaceForAccount(name)
                savePictureUploadsConfigurationUseCase(
                    SavePictureUploadsConfigurationUseCase.Params(
                        composePictureUploadsConfiguration(
                            accountName = name,
                            spaceId = pictureUploadsSpace?.id,
                        )
                    )
                )
            }
        }
    }

    fun disablePictureUploads() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            resetPictureUploadsUseCase(Unit)
        }
    }

    fun useWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase(
                SavePictureUploadsConfigurationUseCase.Params(composePictureUploadsConfiguration(wifiOnly = wifiOnly))
            )
        }
    }

    fun useChargingOnly(chargingOnly: Boolean) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase(
                SavePictureUploadsConfigurationUseCase.Params(
                    composePictureUploadsConfiguration(chargingOnly = chargingOnly)
                )
            )
        }
    }

    fun getPictureUploadsAccount() = _pictureUploads.value?.accountName

    fun getLoggedAccountNames(): Array<String> = accountProvider.getLoggedAccounts().map { it.name }.toTypedArray()

    fun getPictureUploadsPath() = _pictureUploads.value?.uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH

    fun getPictureUploadsSourcePath(): String? = _pictureUploads.value?.sourcePath

    fun handleSelectPictureUploadsPath(data: Intent?) {
        val folderToUpload = data?.getParcelableExtra<OCFile>(FolderPickerActivity.EXTRA_FOLDER)
        folderToUpload?.remotePath?.let {
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                getSpaceById(spaceId = folderToUpload.spaceId, accountName = folderToUpload.owner)
                savePictureUploadsConfigurationUseCase(
                    SavePictureUploadsConfigurationUseCase.Params(
                        composePictureUploadsConfiguration(
                            uploadPath = it,
                            spaceId = pictureUploadsSpace?.id,
                        )
                    )
                )
            }
        }
    }

    fun handleSelectAccount(accountName: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getPersonalSpaceForAccount(accountName)
            savePictureUploadsConfigurationUseCase(
                SavePictureUploadsConfigurationUseCase.Params(
                    composePictureUploadsConfiguration(
                        accountName = accountName,
                        uploadPath = null,
                        spaceId = pictureUploadsSpace?.id,
                    )
                )
            )
        }
    }

    fun handleSelectBehaviour(behaviorString: String) {
        val behavior = UploadBehavior.fromString(behaviorString)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase(
                SavePictureUploadsConfigurationUseCase.Params(composePictureUploadsConfiguration(behavior = behavior))
            )
        }
    }

    fun handleSelectPictureUploadsSourcePath(contentUriForTree: Uri) {
        // If the source path has changed, update camera uploads last sync
        val previousSourcePath = _pictureUploads.value?.sourcePath?.trimEnd(File.separatorChar)

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            savePictureUploadsConfigurationUseCase(
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
        chargingOnly: Boolean? = _pictureUploads.value?.chargingOnly,
        sourcePath: String? = _pictureUploads.value?.sourcePath,
        behavior: UploadBehavior? = _pictureUploads.value?.behavior,
        timestamp: Long? = _pictureUploads.value?.lastSyncTimestamp,
        spaceId: String? = _pictureUploads.value?.spaceId,
    ): FolderBackUpConfiguration = FolderBackUpConfiguration(
        accountName = accountName ?: accountProvider.getCurrentOwnCloudAccount()!!.name,
        behavior = behavior ?: UploadBehavior.COPY,
        sourcePath = sourcePath.orEmpty(),
        uploadPath = uploadPath ?: PREF__CAMERA_UPLOADS_DEFAULT_PATH,
        wifiOnly = wifiOnly ?: false,
        chargingOnly = chargingOnly ?: false,
        lastSyncTimestamp = timestamp ?: System.currentTimeMillis(),
        name = _pictureUploads.value?.name ?: pictureUploadsName,
        spaceId = spaceId,
    ).also {
        Timber.d("Picture uploads configuration updated. New configuration: $it")
    }

    private fun handleSpaceName(spaceName: String?): String? {
        return if (pictureUploadsSpace?.isPersonal == true) {
            contextProvider.getString(R.string.bottom_nav_personal)
        } else {
            spaceName
        }
    }

    fun getUploadPathString(): String {

        val spaceName = handleSpaceName(pictureUploadsSpace?.name)
        val uploadPath = pictureUploads.value?.uploadPath
        val spaceId = pictureUploads.value?.spaceId

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
        pictureUploadsSpace = result
    }

    private fun getSpaceById(spaceId: String?, accountName: String) {
        val result = getSpaceByIdForAccountUseCase(
            GetSpaceByIdForAccountUseCase.Params(
                accountName = accountName,
                spaceId = spaceId
            )
        )
        pictureUploadsSpace = result
    }
}
