/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * @author Jose Antonio Barros Ramos
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

package com.owncloud.android.presentation.files.filelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.R
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.domain.appregistry.usecases.GetAppRegistryForMimeTypeAsStreamUseCase
import com.owncloud.android.domain.appregistry.usecases.GetAppRegistryWhichAllowCreationAsStreamUseCase
import com.owncloud.android.domain.appregistry.usecases.GetUrlToOpenInWebUseCase
import com.owncloud.android.domain.availableoffline.usecases.GetFilesAvailableOfflineFromAccountAsStreamUseCase
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.FileMenuOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.model.OCFileSyncInfo
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsStreamUseCase
import com.owncloud.android.domain.files.usecases.GetSharedByLinkForAccountAsStreamUseCase
import com.owncloud.android.domain.files.usecases.SortFilesWithSyncInfoUseCase
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.usecases.GetSpaceWithSpecialsByIdForAccountUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.files.SortOrder
import com.owncloud.android.presentation.files.SortOrder.Companion.PREF_FILE_LIST_SORT_ORDER
import com.owncloud.android.presentation.files.SortType
import com.owncloud.android.presentation.files.SortType.Companion.PREF_FILE_LIST_SORT_TYPE
import com.owncloud.android.presentation.settings.advanced.SettingsAdvancedFragment.Companion.PREF_SHOW_HIDDEN_FILES
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.files.FilterFileMenuOptionsUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase.SyncFolderMode.SYNC_CONTENTS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.owncloud.android.domain.files.usecases.SortType.Companion as SortTypeDomain

class MainFileListViewModel(
    private val getFolderContentAsStreamUseCase: GetFolderContentAsStreamUseCase,
    private val getSharedByLinkForAccountAsStreamUseCase: GetSharedByLinkForAccountAsStreamUseCase,
    private val getFilesAvailableOfflineFromAccountAsStreamUseCase: GetFilesAvailableOfflineFromAccountAsStreamUseCase,
    private val getFileByIdUseCase: GetFileByIdUseCase,
    private val getFileByRemotePathUseCase: GetFileByRemotePathUseCase,
    private val getSpaceWithSpecialsByIdForAccountUseCase: GetSpaceWithSpecialsByIdForAccountUseCase,
    private val sortFilesWithSyncInfoUseCase: SortFilesWithSyncInfoUseCase,
    private val synchronizeFolderUseCase: SynchronizeFolderUseCase,
    getAppRegistryWhichAllowCreationAsStreamUseCase: GetAppRegistryWhichAllowCreationAsStreamUseCase,
    private val getAppRegistryForMimeTypeAsStreamUseCase: GetAppRegistryForMimeTypeAsStreamUseCase,
    private val getUrlToOpenInWebUseCase: GetUrlToOpenInWebUseCase,
    private val filterFileMenuOptionsUseCase: FilterFileMenuOptionsUseCase,
    private val contextProvider: ContextProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val sharedPreferencesProvider: SharedPreferencesProvider,
    initialFolderToDisplay: OCFile,
    fileListOptionParam: FileListOption,
) : ViewModel() {

    private val showHiddenFiles: Boolean = sharedPreferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, false)

    val currentFolderDisplayed: MutableStateFlow<OCFile> = MutableStateFlow(initialFolderToDisplay)
    val fileListOption: MutableStateFlow<FileListOption> = MutableStateFlow(fileListOptionParam)
    private val searchFilter: MutableStateFlow<String> = MutableStateFlow("")
    private val sortTypeAndOrder = MutableStateFlow(Pair(SortType.SORT_TYPE_BY_NAME, SortOrder.SORT_ORDER_ASCENDING))
    val space: MutableStateFlow<OCSpace?> = MutableStateFlow(null)
    val appRegistryToCreateFiles: StateFlow<List<AppRegistryMimeType>> =
        getAppRegistryWhichAllowCreationAsStreamUseCase(
            GetAppRegistryWhichAllowCreationAsStreamUseCase.Params(
                accountName = initialFolderToDisplay.owner
            )
        ).stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _appRegistryMimeType: MutableSharedFlow<AppRegistryMimeType?> = MutableSharedFlow()
    val appRegistryMimeType: SharedFlow<AppRegistryMimeType?> = _appRegistryMimeType

    private val _appRegistryMimeTypeSingleFile: MutableSharedFlow<AppRegistryMimeType?> = MutableSharedFlow()
    val appRegistryMimeTypeSingleFile: SharedFlow<AppRegistryMimeType?> = _appRegistryMimeTypeSingleFile

    /** File list ui state combines the other fields and generate a new state whenever any of them changes */
    val fileListUiState: StateFlow<FileListUiState> =
        combine(
            currentFolderDisplayed,
            fileListOption,
            searchFilter,
            sortTypeAndOrder,
            space,
        ) { currentFolderDisplayed, fileListOption, searchFilter, sortTypeAndOrder, space ->
            composeFileListUiStateForThisParams(
                currentFolderDisplayed = currentFolderDisplayed,
                fileListOption = fileListOption,
                searchFilter = searchFilter,
                sortTypeAndOrder = sortTypeAndOrder,
                space = space,
            )
        }
            .flatMapLatest { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FileListUiState.Loading
            )

    private val _openInWebFlow = MutableStateFlow<Event<UIResult<String>>?>(null)
    val openInWebFlow: StateFlow<Event<UIResult<String>>?> = _openInWebFlow

    private val _menuOptions: MutableSharedFlow<List<FileMenuOption>> = MutableSharedFlow()
    val menuOptions: SharedFlow<List<FileMenuOption>> = _menuOptions

    private val _menuOptionsSingleFile: MutableSharedFlow<List<FileMenuOption>> = MutableSharedFlow()
    val menuOptionsSingleFile: SharedFlow<List<FileMenuOption>> = _menuOptionsSingleFile

    init {
        val sortTypeSelected = SortType.values()[sharedPreferencesProvider.getInt(PREF_FILE_LIST_SORT_TYPE, SortType.SORT_TYPE_BY_NAME.ordinal)]
        val sortOrderSelected =
            SortOrder.values()[sharedPreferencesProvider.getInt(PREF_FILE_LIST_SORT_ORDER, SortOrder.SORT_ORDER_ASCENDING.ordinal)]
        sortTypeAndOrder.update { Pair(sortTypeSelected, sortOrderSelected) }
        updateSpace()
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            synchronizeFolderUseCase(
                SynchronizeFolderUseCase.Params(
                    remotePath = initialFolderToDisplay.remotePath,
                    accountName = initialFolderToDisplay.owner,
                    spaceId = initialFolderToDisplay.spaceId,
                    syncMode = SYNC_CONTENTS,
                )
            )
        }
    }

    fun navigateToFolderId(folderId: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = getFileByIdUseCase(GetFileByIdUseCase.Params(fileId = folderId))
            result.getDataOrNull()?.let {
                updateFolderToDisplay(it)
            }
        }
    }

    fun getFile(): OCFile {
        return currentFolderDisplayed.value
    }

    fun getSpace(): OCSpace? {
        return space.value
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

    private fun sortList(filesWithSyncInfo: List<OCFileWithSyncInfo>, sortTypeAndOrder: Pair<SortType, SortOrder>): List<OCFileWithSyncInfo> {
        return sortFilesWithSyncInfoUseCase(
            SortFilesWithSyncInfoUseCase.Params(
                listOfFiles = filesWithSyncInfo,
                sortType = SortTypeDomain.fromPreferences(sortTypeAndOrder.first.ordinal),
                ascending = sortTypeAndOrder.second == SortOrder.SORT_ORDER_ASCENDING
            )
        )
    }

    fun manageBrowseUp() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val currentFolder = currentFolderDisplayed.value
            val parentId = currentFolder.parentId
            val parentDir: OCFile?

            // browsing back to not shared by link or av offline should update to root
            if (parentId != null && parentId != ROOT_PARENT_ID) {
                // Browsing to parent folder. Not root
                val fileByIdResult = getFileByIdUseCase(GetFileByIdUseCase.Params(parentId))
                when (fileListOption.value) {
                    FileListOption.ALL_FILES -> {
                        parentDir = fileByIdResult.getDataOrNull()
                    }

                    FileListOption.SHARED_BY_LINK -> {
                        val fileById = fileByIdResult.getDataOrNull()
                        parentDir = if (fileById != null && (!fileById.sharedByLink || fileById.sharedWithSharee != true) && fileById.spaceId == null) {
                            getFileByRemotePathUseCase(GetFileByRemotePathUseCase.Params(fileById.owner, ROOT_PATH)).getDataOrNull()
                        } else fileById
                    }

                    FileListOption.AV_OFFLINE -> {
                        val fileById = fileByIdResult.getDataOrNull()
                        parentDir = if (fileById != null && (!fileById.isAvailableOffline)) {
                            getFileByRemotePathUseCase(GetFileByRemotePathUseCase.Params(fileById.owner, ROOT_PATH)).getDataOrNull()
                        } else fileById
                    }

                    FileListOption.SPACES_LIST -> {
                        parentDir = TODO("Move it to usecase if possible")
                    }
                }
            } else if (parentId == ROOT_PARENT_ID) {
                // Browsing to parent folder. Root
                val rootFolderForAccountResult = getFileByRemotePathUseCase(
                    GetFileByRemotePathUseCase.Params(
                        remotePath = ROOT_PATH,
                        owner = currentFolder.owner,
                    )
                )
                parentDir = rootFolderForAccountResult.getDataOrNull()
            } else {
                // Browsing to non existing parent folder.
                TODO()
            }

            parentDir?.let { updateFolderToDisplay(it) }
        }
    }

    fun updateFolderToDisplay(newFolderToDisplay: OCFile) {
        currentFolderDisplayed.update { newFolderToDisplay }
        searchFilter.update { "" }
        updateSpace()
    }

    fun updateSearchFilter(newSearchFilter: String) {
        searchFilter.update { newSearchFilter }
    }

    fun updateFileListOption(newFileListOption: FileListOption) {
        fileListOption.update { newFileListOption }
    }

    fun updateSortTypeAndOrder(sortType: SortType, sortOrder: SortOrder) {
        sharedPreferencesProvider.putInt(PREF_FILE_LIST_SORT_TYPE, sortType.ordinal)
        sharedPreferencesProvider.putInt(PREF_FILE_LIST_SORT_ORDER, sortOrder.ordinal)
        sortTypeAndOrder.update { Pair(sortType, sortOrder) }
    }

    fun openInWeb(fileId: String, appName: String) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            flow = _openInWebFlow,
            useCase = getUrlToOpenInWebUseCase,
            useCaseParams = GetUrlToOpenInWebUseCase.Params(
                fileId = fileId,
                accountName = getFile().owner,
                appName = appName,
            ),
            showLoading = false,
            requiresConnection = true,
        )
    }

    fun resetOpenInWebFlow() {
        _openInWebFlow.value = null
    }

    fun filterMenuOptions(
        files: List<OCFile>, filesSyncInfo: List<OCFileSyncInfo>,
        displaySelectAll: Boolean, isMultiselection: Boolean
    ) {
        val shareViaLinkAllowed = contextProvider.getBoolean(R.bool.share_via_link_feature)
        val shareWithUsersAllowed = contextProvider.getBoolean(R.bool.share_with_users_feature)
        val sendAllowed = contextProvider.getString(R.string.send_files_to_other_apps).equals("on", ignoreCase = true)
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = filterFileMenuOptionsUseCase(
                FilterFileMenuOptionsUseCase.Params(
                    files = files,
                    filesSyncInfo = filesSyncInfo,
                    accountName = currentFolderDisplayed.value.owner,
                    isAnyFileVideoPreviewing = false,
                    displaySelectAll = displaySelectAll,
                    displaySelectInverse = isMultiselection,
                    onlyAvailableOfflineFiles = fileListOption.value.isAvailableOffline(),
                    onlySharedByLinkFiles = fileListOption.value.isSharedByLink(),
                    shareViaLinkAllowed = shareViaLinkAllowed,
                    shareWithUsersAllowed = shareWithUsersAllowed,
                    sendAllowed = sendAllowed,
                )
            )
            if (isMultiselection) {
                _menuOptions.emit(result)
            } else {
                _menuOptionsSingleFile.emit(result)
            }
        }
    }

    fun getAppRegistryForMimeType(mimeType: String, isMultiselection: Boolean) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = getAppRegistryForMimeTypeAsStreamUseCase(
                GetAppRegistryForMimeTypeAsStreamUseCase.Params(accountName = getFile().owner, mimeType)
            )
            if (isMultiselection) {
                _appRegistryMimeType.emit(result.firstOrNull())
            } else {
                _appRegistryMimeTypeSingleFile.emit(result.firstOrNull())
            }
        }
    }

    private fun updateSpace() {
        val folderToDisplay = currentFolderDisplayed.value
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            if (folderToDisplay.remotePath == ROOT_PATH) {
                val currentSpace = getSpaceWithSpecialsByIdForAccountUseCase(
                    GetSpaceWithSpecialsByIdForAccountUseCase.Params(
                        spaceId = folderToDisplay.spaceId,
                        accountName = folderToDisplay.owner,
                    )
                )
                space.update { currentSpace }
            }
        }

    }

    private fun composeFileListUiStateForThisParams(
        currentFolderDisplayed: OCFile,
        fileListOption: FileListOption,
        searchFilter: String?,
        sortTypeAndOrder: Pair<SortType, SortOrder>,
        space: OCSpace?,
    ): Flow<FileListUiState> =
        when (fileListOption) {
            FileListOption.ALL_FILES -> retrieveFlowForAllFiles(currentFolderDisplayed, currentFolderDisplayed.owner)
            FileListOption.SHARED_BY_LINK -> retrieveFlowForShareByLink(currentFolderDisplayed, currentFolderDisplayed.owner)
            FileListOption.AV_OFFLINE -> retrieveFlowForAvailableOffline(currentFolderDisplayed, currentFolderDisplayed.owner)
            FileListOption.SPACES_LIST -> TODO()
        }.toFileListUiState(
            currentFolderDisplayed,
            fileListOption,
            searchFilter,
            sortTypeAndOrder,
            space,
        )

    private fun retrieveFlowForAllFiles(
        currentFolderDisplayed: OCFile,
        accountName: String,
    ): Flow<List<OCFileWithSyncInfo>> =
        getFolderContentAsStreamUseCase(
            GetFolderContentAsStreamUseCase.Params(
                folderId = currentFolderDisplayed.id
                    ?: getFileByRemotePathUseCase(GetFileByRemotePathUseCase.Params(accountName, ROOT_PATH)).getDataOrNull()!!.id!!
            )
        )

    /**
     * In root folder, all the shared by link files should be shown. Otherwise, the folder content should be shown.
     * Logic to handle the browse back in [manageBrowseUp]
     */
    private fun retrieveFlowForShareByLink(
        currentFolderDisplayed: OCFile,
        accountName: String,
    ): Flow<List<OCFileWithSyncInfo>> =
        if (currentFolderDisplayed.remotePath == ROOT_PATH && currentFolderDisplayed.spaceId == null) {
            getSharedByLinkForAccountAsStreamUseCase(GetSharedByLinkForAccountAsStreamUseCase.Params(accountName))
        } else {
            retrieveFlowForAllFiles(currentFolderDisplayed, accountName)
        }

    /**
     * In root folder, all the available offline files should be shown. Otherwise, the folder content should be shown.
     * Logic to handle the browse back in [manageBrowseUp]
     */
    private fun retrieveFlowForAvailableOffline(
        currentFolderDisplayed: OCFile,
        accountName: String,
    ): Flow<List<OCFileWithSyncInfo>> =
        if (currentFolderDisplayed.remotePath == ROOT_PATH) {
            getFilesAvailableOfflineFromAccountAsStreamUseCase(GetFilesAvailableOfflineFromAccountAsStreamUseCase.Params(accountName))
        } else {
            retrieveFlowForAllFiles(currentFolderDisplayed, accountName)
        }

    private fun Flow<List<OCFileWithSyncInfo>>.toFileListUiState(
        currentFolderDisplayed: OCFile,
        fileListOption: FileListOption,
        searchFilter: String?,
        sortTypeAndOrder: Pair<SortType, SortOrder>,
        space: OCSpace?,
    ) = this.map { folderContent ->
        FileListUiState.Success(
            folderToDisplay = currentFolderDisplayed,
            folderContent = folderContent.filter { fileWithSyncInfo ->
                fileWithSyncInfo.file.fileName.contains(
                    searchFilter ?: "",
                    ignoreCase = true
                ) && (showHiddenFiles || !fileWithSyncInfo.file.fileName.startsWith("."))
            }.let { sortList(it, sortTypeAndOrder) },
            fileListOption = fileListOption,
            searchFilter = searchFilter,
            space = space,
        )
    }

    sealed interface FileListUiState {
        object Loading : FileListUiState
        data class Success(
            val folderToDisplay: OCFile?,
            val folderContent: List<OCFileWithSyncInfo>,
            val fileListOption: FileListOption,
            val searchFilter: String?,
            val space: OCSpace?,
        ) : FileListUiState
    }

    companion object {
        private const val RECYCLER_VIEW_PREFERRED = "RECYCLER_VIEW_PREFERRED"
    }
}

