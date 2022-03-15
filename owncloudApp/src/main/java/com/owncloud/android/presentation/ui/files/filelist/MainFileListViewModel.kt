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
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsLiveDataUseCase
import com.owncloud.android.domain.files.usecases.GetSearchFolderContentUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.isDownloadPending
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.launch
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

    /** LiveData to maintain the folder content for the current file displayed.
     * It is automatically updated after updating the _currentFileLiveData
     */
    private val _folderContentLiveData: LiveData<Event<List<OCFile>>> =
        Transformations.switchMap(currentFileLiveData) { folder ->
            getFolderContentAsLiveDataUseCase.execute(GetFolderContentAsLiveDataUseCase.Params(folderId = folder.id!!))
                .map { Event(it) }
        }
    val folderContentLiveData: LiveData<Event<List<OCFile>>>
        get() = _folderContentLiveData

    private val _getSearchedFilesData = MutableLiveData<Event<UIResult<List<OCFile>>>>()
    val getSearchedFilesData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getSearchedFilesData

    private fun getFilesList(folderId: Long) {
        //fileIdLiveData.postValue(folderId)
    }

    private fun getSearchFilesList(fileListOption: FileListOption, folderId: Long, searchText: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getSearchFolderContentUseCase.execute(
                GetSearchFolderContentUseCase.Params(
                    fileListOption = fileListOption,
                    folderId = folderId,
                    search = searchText
                )
            ).let {
                when (it) {
                    is UseCaseResult.Error -> _getSearchedFilesData.postValue(Event(UIResult.Error(it.getThrowableOrNull())))
                    is UseCaseResult.Success -> _getSearchedFilesData.postValue(Event(UIResult.Success(it.getDataOrNull())))
                }
            }
        }
    }

    // FIXME: I think that this function is not needed at all. File list is automatically refreshed with database via LiveData.
    fun listCurrentDirectory() {
        //getFilesList(file.id!!)
    }

    fun navigateTo(ocFile: OCFile) {
        _currentFileLiveData.postValue(ocFile)
    }

    fun navigateTo(fileId: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId = fileId))
            result.getDataOrNull()?.let {
                _currentFileLiveData.postValue(it)
            }
        }
    }

    fun listDirectory(directory: OCFile) {
        navigateTo(ocFile = directory)
        //file = directory
        //getFilesList(directory.id!!)
    }

    fun listSearchCurrentDirectory(fileListOption: FileListOption, searchText: String) {
        getSearchFilesList(fileListOption, currentFileLiveData.value!!.id!!, searchText)
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

            navigateTo(parentDir!!)
        }
    }

    fun fileIsDownloading(file: OCFile, account: Account): Boolean {
        return workManager.isDownloadPending(account, file)
    }

    companion object {
        private const val RECYCLER_VIEW_PREFERRED = "RECYCLER_VIEW_PREFERRED"
    }
}

