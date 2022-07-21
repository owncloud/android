/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * @author Jose Antonio Barros Ramos
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.presentation.ui.files.filelist

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.datamodel.FileDataStorageManager.Companion.ROOT_PARENT_ID
import com.owncloud.android.datamodel.OCFile.ROOT_PATH
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.availableoffline.usecases.GetFilesAvailableOfflineFromAccountAsStreamUseCase
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsStreamUseCase
import com.owncloud.android.domain.files.usecases.GetSearchFolderContentUseCase
import com.owncloud.android.domain.files.usecases.GetSharedByLinkForAccountAsStreamUseCase
import com.owncloud.android.domain.files.usecases.SortFilesUseCase
import com.owncloud.android.domain.files.usecases.SortType
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.extensions.isDownloadPending
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase.SyncFolderMode.SYNC_CONTENTS
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase.SyncFolderMode.SYNC_FOLDER_RECURSIVELY
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainFileListViewModel(
    private val getFolderContentAsStreamUseCase: GetFolderContentAsStreamUseCase,
    private val getSearchFolderContentUseCase: GetSearchFolderContentUseCase,
    private val getSharedByLinkForAccountAsStreamUseCase: GetSharedByLinkForAccountAsStreamUseCase,
    private val getFilesAvailableOfflineFromAccountAsStreamUseCase: GetFilesAvailableOfflineFromAccountAsStreamUseCase,
    private val getFileByIdUseCase: GetFileByIdUseCase,
    private val getFileByRemotePathUseCase: GetFileByRemotePathUseCase,
    private val sortFilesUseCase: SortFilesUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val sharedPreferencesProvider: SharedPreferencesProvider,
    private val synchronizeFolderUseCase: SynchronizeFolderUseCase,
    private val contextProvider: ContextProvider,
    private val workManager: WorkManager,
) : ViewModel() {

    private var accountName: MutableStateFlow<String> = MutableStateFlow(
        value = AccountUtils.getCurrentOwnCloudAccount(contextProvider.getContext()).name
    )

    val currentFolderDisplayed = MutableStateFlow(
        // TODO: Get not nullable root folder for account or create and retrieve it. This MUST be fixed.
        OCFile(ROOT_PATH, MIME_DIR, 1, accountName.value)
    )

    private var fileListOption: MutableStateFlow<FileListOption> = MutableStateFlow(FileListOption.ALL_FILES)
    private var searchFilter: MutableStateFlow<String> = MutableStateFlow("")

    /** File list ui state combines the other fields and generate a new state whenever any of them changes */
    val fileListUiState: StateFlow<FileListUiState> =
        combine(
            currentFolderDisplayed,
            accountName,
            fileListOption,
            searchFilter,
        ) { currentFolderDisplayed, accountName, fileListOption, searchFilter ->
            generateFileListUiStateForThisParams(
                currentFolderDisplayed = currentFolderDisplayed,
                accountName = accountName,
                fileListOption = fileListOption,
                searchFilter = searchFilter
            )
        }
            .flatMapLatest { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FileListUiState.Loading
            )

    private val _syncFolder = MediatorLiveData<Event<UIResult<Unit>>>()
    val syncFolder: LiveData<Event<UIResult<Unit>>> = _syncFolder

    fun navigateTo(fileId: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId = fileId))
            result.getDataOrNull()?.let {
                currentFolderDisplayed.update { it }
            }
        }
    }

    fun getFile(): OCFile {
        // FIXME: Remove those ugly !!
        return currentFolderDisplayed.value
    }

    fun setGridModeAsPreferred() {
        savePreferredLayoutManager(true)
    }

    fun setListModeAsPreferred() {
        savePreferredLayoutManager(false)
    }

    private fun savePreferredLayoutManager(isGridModeSelected: Boolean) {
        sharedPreferencesProvider.putBoolean(RECYCLER_VIEW_PREFERRED, isGridModeSelected)
    }

    fun isGridModeSetAsPreferred() = sharedPreferencesProvider.getBoolean(RECYCLER_VIEW_PREFERRED, false)

    fun sortList(files: List<OCFile>): List<OCFile> {
        val sortOrderSaved = PreferenceManager.getSortOrder(contextProvider.getContext(), FileStorageUtils.FILE_DISPLAY_SORT)
        val ascendingModeSaved = PreferenceManager.getSortAscending(contextProvider.getContext(), FileStorageUtils.FILE_DISPLAY_SORT)

        return sortFilesUseCase.execute(SortFilesUseCase.Params(files, SortType.fromPreferences(sortOrderSaved), ascendingModeSaved))
    }

    fun manageBrowseUp() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val currentFolder = currentFolderDisplayed.value
            val parentId = currentFolder.parentId
            val parentDir: OCFile?

            // browsing back to not shared by link or av offline should update to root
            if (parentId != null && parentId != ROOT_PARENT_ID.toLong()) {
                // Browsing to parent folder. Not root
                val fileByIdResult = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(parentId))
                when (fileListOption.value) {
                    FileListOption.ALL_FILES -> {
                        parentDir = fileByIdResult.getDataOrNull()
                    }
                    FileListOption.SHARED_BY_LINK -> {
                        val fileById = fileByIdResult.getDataOrNull()!!
                        parentDir = if (!fileById.sharedByLink || fileById.sharedWithSharee != true) {
                            getFileByRemotePathUseCase.execute(GetFileByRemotePathUseCase.Params(fileById.owner, ROOT_PATH)).getDataOrNull()
                        } else fileById
                    }
                    FileListOption.AV_OFFLINE -> {
                        val fileById = fileByIdResult.getDataOrNull()!!
                        parentDir = if (!fileById.isAvailableOffline) {
                            getFileByRemotePathUseCase.execute(GetFileByRemotePathUseCase.Params(fileById.owner, ROOT_PATH)).getDataOrNull()
                        } else fileById
                    }
                }
            } else if (parentId == ROOT_PARENT_ID.toLong()) {
                // Browsing to parent folder. Root
                val rootFolderForAccountResult = getFileByRemotePathUseCase.execute(
                    GetFileByRemotePathUseCase.Params(
                        remotePath = ROOT_PATH, owner = currentFolder.owner
                    )
                )
                parentDir = rootFolderForAccountResult.getDataOrNull()
            } else {
                // Browsing to non existing parent folder.
                TODO()
            }

            updateFolderToDisplay(parentDir!!)
            if (fileListOption.value.isAllFiles()) {
                refreshFolder(
                    ocFolder = parentDir,
                    isPickingAFolder = false
                )
            }
        }
    }

    fun fileIsDownloading(file: OCFile, account: Account): Boolean {
        return workManager.isDownloadPending(account, file)
    }

    fun updateFolderToDisplay(newFolderToDisplay: OCFile) {
        currentFolderDisplayed.update { newFolderToDisplay }
    }

    fun updateSearchFilter(newSearchFilter: String) {
        searchFilter.update { newSearchFilter }
    }

    fun updateFileListOption(newFileListOption: FileListOption) {
        fileListOption.update { newFileListOption }
    }

    fun refreshFolder(
        ocFolder: OCFile,
        isPickingAFolder: Boolean,
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        liveData = _syncFolder,
        useCase = synchronizeFolderUseCase,
        showLoading = true,
        useCaseParams = SynchronizeFolderUseCase.Params(
            remotePath = ocFolder.remotePath,
            accountName = ocFolder.owner,
            syncMode = if (isPickingAFolder) REFRESH_FOLDER else SYNC_CONTENTS
        )
    )

    fun syncFolder(
        ocFolder: OCFile,
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        liveData = _syncFolder,
        useCase = synchronizeFolderUseCase,
        showLoading = true,
        useCaseParams = SynchronizeFolderUseCase.Params(
            remotePath = ocFolder.remotePath,
            accountName = ocFolder.owner,
            syncMode = SYNC_FOLDER_RECURSIVELY
        )
    )

    fun getMessageForEmptyList(pickingAFolder: Boolean = false): String {
        if (pickingAFolder) return contextProvider.getString(R.string.file_list_empty_moving)

        val stringId = when (fileListOption.value) {
            FileListOption.AV_OFFLINE -> R.string.file_list_empty_available_offline
            FileListOption.SHARED_BY_LINK -> R.string.file_list_empty_shared_by_links
            else -> R.string.file_list_empty
        }
        return contextProvider.getString(stringId)
    }

    private fun generateFileListUiStateForThisParams(
        currentFolderDisplayed: OCFile,
        accountName: String,
        fileListOption: FileListOption,
        searchFilter: String?
    ): Flow<FileListUiState> =
        when (fileListOption) {
            FileListOption.ALL_FILES -> retrieveFlowForAllFiles(currentFolderDisplayed, accountName, fileListOption, searchFilter)
            FileListOption.SHARED_BY_LINK -> retrieveFlowForShareByLink(currentFolderDisplayed, accountName, fileListOption, searchFilter)
            FileListOption.AV_OFFLINE -> retrieveFlowForAvailableOffline(currentFolderDisplayed, accountName, fileListOption, searchFilter)
        }

    private fun retrieveFlowForAllFiles(
        currentFolderDisplayed: OCFile,
        accountName: String,
        fileListOption: FileListOption,
        searchFilter: String?
    ): Flow<FileListUiState> =
        getFolderContentAsStreamUseCase.execute(
            GetFolderContentAsStreamUseCase.Params(
                // TODO: Fix the issue with the initial value for currentFolderDisplayed. Fallback MUST be the root folder for the current account.
                //   NOT 1
                currentFolderDisplayed.id ?: 1
            )
        ).map {
            FileListUiState.Success(
                accountName, currentFolderDisplayed, it, fileListOption, searchFilter
            )
        }

    /**
     * In root folder, all the shared by link files should be shown. Otherwise, the folder content should be shown.
     * Logic to handle the browse back in [manageBrowseUp]
     */
    private fun retrieveFlowForShareByLink(
        currentFolderDisplayed: OCFile,
        accountName: String,
        fileListOption: FileListOption,
        searchFilter: String?
    ): Flow<FileListUiState> = if (currentFolderDisplayed.remotePath == ROOT_PATH) {
        getSharedByLinkForAccountAsStreamUseCase.execute(GetSharedByLinkForAccountAsStreamUseCase.Params(accountName)).map {
            FileListUiState.Success(
                accountName, currentFolderDisplayed, it, fileListOption, searchFilter
            )
        }
    } else {
        retrieveFlowForAllFiles(currentFolderDisplayed, accountName, fileListOption, searchFilter)
    }

    /**
     * In root folder, all the available offline files should be shown. Otherwise, the folder content should be shown.
     * Logic to handle the browse back in [manageBrowseUp]
     */
    private fun retrieveFlowForAvailableOffline(
        currentFolderDisplayed: OCFile,
        accountName: String,
        fileListOption: FileListOption,
        searchFilter: String?
    ): Flow<FileListUiState> = if (currentFolderDisplayed.remotePath == ROOT_PATH) {
        getFilesAvailableOfflineFromAccountAsStreamUseCase.execute(GetFilesAvailableOfflineFromAccountAsStreamUseCase.Params(accountName))
            .map {
                FileListUiState.Success(
                    accountName, currentFolderDisplayed, it, fileListOption, searchFilter
                )
            }
    } else {
        retrieveFlowForAllFiles(currentFolderDisplayed, accountName, fileListOption, searchFilter)
    }

    sealed interface FileListUiState {
        object Loading : FileListUiState
        data class Success(
            val accountName: String,
            val folderToDisplay: OCFile?,
            val folderContent: List<OCFile>,
            val fileListOption: FileListOption,
            val searchFilter: String?,
        ) : FileListUiState
    }

    companion object {
        private const val RECYCLER_VIEW_PREFERRED = "RECYCLER_VIEW_PREFERRED"
    }
}

