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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsLiveDataUseCase
import com.owncloud.android.domain.files.usecases.GetSearchFolderContentUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.isDownloadPending
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class MainFileListViewModel(
    private val getFolderContentAsLiveDataUseCase: GetFolderContentAsLiveDataUseCase,
    private val getSearchFolderContentUseCase: GetSearchFolderContentUseCase,
    private val getFileByIdUseCase: GetFileByIdUseCase,
    private val getFileByRemotePathUseCase: GetFileByRemotePathUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val sharedPreferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider,
    private val workManager: WorkManager,
) : ViewModel() {

    /** LiveData to maintain the current file displayed */
    private val _currentFileLiveData = MutableLiveData<OCFile>()
    val currentFileLiveData: LiveData<OCFile>
        get() = _currentFileLiveData

    /** LiveData to maintain the current file list state */
    private val _fileListUiStateLiveData = MutableLiveData<FileListUiState>()
    val fileListUiStateLiveData: LiveData<FileListUiState>
        get() = _fileListUiStateLiveData

    /** LiveData to maintain the folder content for the current file displayed.
     * It is automatically updated after updating the _currentFileLiveData
     */
    private val _folderContentLiveData: LiveData<Event<List<OCFile>>> =
        Transformations.switchMap(currentFileLiveData) { folder ->
            getFolderContentAsLiveDataUseCase.execute(GetFolderContentAsLiveDataUseCase.Params(folderId = folder.id!!)).map { folderContent ->
                val newFileListUiState = composeFileListUiState(folderToDisplay = folder, folderContent = folderContent)
                _fileListUiStateLiveData.postValue(newFileListUiState)
                Event(folderContent)
            }
        }
    val folderContentLiveData: LiveData<Event<List<OCFile>>>
        get() = _folderContentLiveData

    fun navigateTo(fileId: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId = fileId))
            result.getDataOrNull()?.let {
                _currentFileLiveData.postValue(it)
            }
        }
    }

    fun getFile(): OCFile {
        // FIXME: Remove those ugly !!
        return currentFileLiveData.value!!
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

        return FileStorageUtils.sortFolder(
            files, sortOrderSaved,
            ascendingModeSaved
        )
    }

    fun manageBrowseUp(fileListOption: FileListOption?) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val currentFolder = currentFileLiveData.value!!
            var parentDir: OCFile?
            var parentPath: String? = null
            if (currentFolder.parentId != FileDataStorageManager.ROOT_PARENT_ID.toLong()) {
                parentPath = File(currentFolder.remotePath).parent
                parentPath = if (parentPath.endsWith(File.separator)) parentPath else parentPath + File.separator

                val fileByIdResult = getFileByRemotePathUseCase.execute(
                    GetFileByRemotePathUseCase.Params(
                        owner = currentFolder.owner,
                        remotePath = parentPath!!
                    )
                )
                parentDir = if (fileByIdResult.isSuccess) fileByIdResult.getDataOrNull() else null
            } else {
                val fileByIdResult = getFileByIdUseCase.execute(
                    GetFileByIdUseCase.Params(
                        fileId = OCFile.ROOT_PARENT_ID
                    )
                )
                parentDir = if (fileByIdResult.isSuccess) fileByIdResult.getDataOrNull() else null
            }
            while (parentDir == null) {
                parentPath = if (parentPath != null && parentPath != File.separator) File(parentPath).parent else File.separator
                parentPath = if (parentPath.endsWith(File.separator)) parentPath else parentPath + File.separator
                val fileByIdResult = getFileByRemotePathUseCase.execute(
                    GetFileByRemotePathUseCase.Params(
                        owner = currentFolder.owner,
                        remotePath = parentPath!!
                    )
                )
                parentDir = if (fileByIdResult.isSuccess) fileByIdResult.getDataOrNull() else null
            }

            if (fileListOption?.isSharedByLink() == true && !parentDir.sharedByLink) {
                val fileByIdResult = getFileByIdUseCase.execute(
                    GetFileByIdUseCase.Params(
                        fileId = OCFile.ROOT_PARENT_ID
                    )
                )
                parentDir = if (fileByIdResult.isSuccess) fileByIdResult.getDataOrNull() else null
            }

            updateFolderToDisplay(parentDir!!)
        }
    }

    fun fileIsDownloading(file: OCFile, account: Account): Boolean {
        return workManager.isDownloadPending(account, file)
    }

    fun updateFolderToDisplay(newFolderToDisplay: OCFile) {
        _currentFileLiveData.postValue(newFolderToDisplay)
    }

    fun updateSearchFilter(newSearchFilter: String) {
        _fileListUiStateLiveData.postValue(
            composeFileListUiState(searchFilter = newSearchFilter)
        )
    }

    fun updateFileListOption(newFileListOption: FileListOption) {
        _fileListUiStateLiveData.postValue(
            composeFileListUiState(fileListOption = newFileListOption)
        )
    }

    private fun composeFileListUiState(
        account: Account? = _fileListUiStateLiveData.value?.account,
        folderToDisplay: OCFile? = _fileListUiStateLiveData.value?.folderToDisplay,
        folderContent: List<OCFile>? = _fileListUiStateLiveData.value?.folderContent,
        fileListOption: FileListOption? = _fileListUiStateLiveData.value?.fileListOption,
        searchFilter: String? = _fileListUiStateLiveData.value?.searchFilter,
    ): FileListUiState {
        Timber.i("================ Composing a new file list Ui state ==============")
        Timber.i("Account received from parameter: $account")
        Timber.i("FolderToDisplay received from parameter: $folderToDisplay")
        Timber.i("FolderContent received from parameter: $folderContent")
        Timber.i("FileListOption received from parameter: $fileListOption")
        Timber.i("SearchFilter received from parameter: $searchFilter")

        return FileListUiState(
            account = account ?: AccountUtils.getCurrentOwnCloudAccount(contextProvider.getContext()),
            folderToDisplay = folderToDisplay!!,
            folderContent = folderContent ?: emptyList(),
            fileListOption = fileListOption ?: FileListOption.ALL_FILES,
            searchFilter = searchFilter ?: ""
        ).also {
            Timber.i("================ Already composed a new file list Ui state ==============")
            Timber.i("Account: ${it.account.name}")
            Timber.i("File list option: ${it.fileListOption}")
            Timber.i("Search filter text: ${it.searchFilter}")
            Timber.i("Folder to display: id[${it.folderToDisplay.id}], fileName[${it.folderToDisplay.fileName}]")
            Timber.i("Folder content: size[${it.folderContent.size}]")
        }
    }

    data class FileListUiState(
        val account: Account,
        val folderToDisplay: OCFile,
        val folderContent: List<OCFile>,
        val fileListOption: FileListOption,
        val searchFilter: String,
    )

    companion object {
        private const val RECYCLER_VIEW_PREFERRED = "RECYCLER_VIEW_PREFERRED"
    }
}

