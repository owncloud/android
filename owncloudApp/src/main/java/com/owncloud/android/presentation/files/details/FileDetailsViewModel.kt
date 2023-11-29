/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.files.details

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.owncloud.android.R
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.domain.appregistry.usecases.GetAppRegistryForMimeTypeAsStreamUseCase
import com.owncloud.android.domain.appregistry.usecases.GetUrlToOpenInWebUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.extensions.isOneOf
import com.owncloud.android.domain.files.model.FileMenuOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.domain.files.usecases.GetFileWithSyncInfoByIdUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.extensions.getRunningWorkInfosByTags
import com.owncloud.android.extensions.isDownload
import com.owncloud.android.extensions.isUpload
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.files.details.FileDetailsViewModel.ActionsInDetailsView.NONE
import com.owncloud.android.presentation.files.details.FileDetailsViewModel.ActionsInDetailsView.SYNC_AND_OPEN
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.files.FilterFileMenuOptionsUseCase
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadForFileUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadForFileUseCase
import com.owncloud.android.workers.DownloadFileWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class FileDetailsViewModel(
    private val openInWebUseCase: GetUrlToOpenInWebUseCase,
    refreshCapabilitiesFromServerAsyncUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    getAppRegistryForMimeTypeAsStreamUseCase: GetAppRegistryForMimeTypeAsStreamUseCase,
    private val cancelDownloadForFileUseCase: CancelDownloadForFileUseCase,
    private val cancelUploadForFileUseCase: CancelUploadForFileUseCase,
    private val filterFileMenuOptionsUseCase: FilterFileMenuOptionsUseCase,
    getFileWithSyncInfoByIdUseCase: GetFileWithSyncInfoByIdUseCase,
    val contextProvider: ContextProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val workManager: WorkManager,
    account: Account,
    ocFile: OCFile,
    shouldSyncFile: Boolean,
) : ViewModel() {

    private val _openInWebUriLiveData: MediatorLiveData<Event<UIResult<String?>>> = MediatorLiveData()
    val openInWebUriLiveData: LiveData<Event<UIResult<String?>>> = _openInWebUriLiveData

    val appRegistryMimeType: StateFlow<AppRegistryMimeType?> =
        getAppRegistryForMimeTypeAsStreamUseCase(
            GetAppRegistryForMimeTypeAsStreamUseCase.Params(accountName = account.name, ocFile.mimeType)
        ).stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val account: StateFlow<Account> = MutableStateFlow(account)
    private val ocFileWithSyncInfo = OCFileWithSyncInfo(
        file = ocFile,
        uploadWorkerUuid = UUID.randomUUID(),
        downloadWorkerUuid = UUID.randomUUID(),
        isSynchronizing = true,
        space = null
    )

    val currentFile: StateFlow<OCFileWithSyncInfo?> =
        getFileWithSyncInfoByIdUseCase(GetFileWithSyncInfoByIdUseCase.Params(ocFile.id!!))
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ocFileWithSyncInfo
            )

    private val _ongoingTransferUUID = MutableLiveData<UUID>()
    private val _ongoingTransfer = _ongoingTransferUUID.switchMap { transferUUID ->
        workManager.getWorkInfoByIdLiveData(transferUUID)
    }.map { Event(it) }
    val ongoingTransfer: LiveData<Event<WorkInfo?>> = _ongoingTransfer

    init {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            refreshCapabilitiesFromServerAsyncUseCase(RefreshCapabilitiesFromServerAsyncUseCase.Params(account.name))
        }
    }

    private val _actionsInDetailsView: MutableStateFlow<ActionsInDetailsView> = MutableStateFlow(if (shouldSyncFile) SYNC_AND_OPEN else NONE)
    val actionsInDetailsView: StateFlow<ActionsInDetailsView> = _actionsInDetailsView

    private val _menuOptions: MutableStateFlow<List<FileMenuOption>> = MutableStateFlow(emptyList())
    val menuOptions: StateFlow<List<FileMenuOption>> = _menuOptions

    fun getCurrentFile(): OCFileWithSyncInfo? = currentFile.value
    fun getAccount() = account.value

    fun updateActionInDetailsView(actionsInDetailsView: ActionsInDetailsView) {
        _actionsInDetailsView.update { actionsInDetailsView }
    }

    fun startListeningToWorkInfo(uuid: UUID?) {
        uuid?.let {
            _ongoingTransferUUID.postValue(it)
        }
    }

    fun checkOnGoingTransfersWhenOpening() {
        val safeFile = currentFile.value ?: return
        val listOfWorkers =
            workManager.getRunningWorkInfosByTags(listOf(safeFile.file.id!!.toString(), getAccount().name, DownloadFileWorker::class.java.name))
        listOfWorkers.firstOrNull()?.let { workInfo ->
            _ongoingTransferUUID.postValue(workInfo.id)
        }
    }

    fun cancelCurrentTransfer() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val currentTransfer = ongoingTransfer.value?.peekContent() ?: return@launch
            val safeFile = currentFile.value ?: return@launch
            if (currentTransfer.isUpload()) {
                cancelUploadForFileUseCase(CancelUploadForFileUseCase.Params(safeFile.file))
            } else if (currentTransfer.isDownload()) {
                cancelDownloadForFileUseCase(CancelDownloadForFileUseCase.Params(safeFile.file))
            }
        }
    }

    fun openInWeb(fileId: String, appName: String) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            liveData = _openInWebUriLiveData,
            useCase = openInWebUseCase,
            useCaseParams = GetUrlToOpenInWebUseCase.Params(
                fileId = fileId,
                accountName = getAccount().name,
                appName = appName,
            ),
            showLoading = false,
            requiresConnection = true,
        )
    }

    fun filterMenuOptions(file: OCFile) {
        val shareViaLinkAllowed = contextProvider.getBoolean(R.bool.share_via_link_feature)
        val shareWithUsersAllowed = contextProvider.getBoolean(R.bool.share_with_users_feature)
        val sendAllowed = contextProvider.getString(R.string.send_files_to_other_apps).equals("on", ignoreCase = true)
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = filterFileMenuOptionsUseCase(
                FilterFileMenuOptionsUseCase.Params(
                    files = listOf(file),
                    accountName = getAccount().name,
                    isAnyFileVideoPreviewing = false,
                    displaySelectAll = false,
                    displaySelectInverse = false,
                    onlyAvailableOfflineFiles = false,
                    onlySharedByLinkFiles = false,
                    shareViaLinkAllowed = shareViaLinkAllowed,
                    shareWithUsersAllowed = shareWithUsersAllowed,
                    sendAllowed = sendAllowed,
                )
            )
            result.apply {
                remove(FileMenuOption.DETAILS)
                remove(FileMenuOption.MOVE)
                remove(FileMenuOption.COPY)
            }
            _menuOptions.update { result }
        }
    }


    enum class ActionsInDetailsView {
        NONE, SYNC, SYNC_AND_OPEN, SYNC_AND_OPEN_WITH, SYNC_AND_SEND;

        fun requiresSync(): Boolean = this.isOneOf(SYNC, SYNC_AND_OPEN, SYNC_AND_OPEN_WITH, SYNC_AND_SEND)
    }
}
