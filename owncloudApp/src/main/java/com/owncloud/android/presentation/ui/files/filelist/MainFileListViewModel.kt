/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFilesAvailableOfflineUseCase
import com.owncloud.android.domain.files.usecases.GetFilesSharedByLinkUseCase
import com.owncloud.android.domain.files.usecases.GetSearchFolderContentUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsLiveDataUseCase
import com.owncloud.android.domain.files.usecases.RefreshFolderFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.launch

class MainFileListViewModel(
    private val getFolderContentAsLiveDataUseCase: GetFolderContentAsLiveDataUseCase,
    private val getFilesSharedByLinkUseCase: GetFilesSharedByLinkUseCase,
    private val getFilesAvailableOfflineUseCase: GetFilesAvailableOfflineUseCase,
    private val getSearchFolderContentUseCase: GetSearchFolderContentUseCase,
    private val refreshFolderFromServerAsyncUseCase: RefreshFolderFromServerAsyncUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val contextProvider: ContextProvider,
) : ViewModel() {

    private lateinit var file: OCFile

    private val _getFilesListStatusLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesListStatusLiveData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesListStatusLiveData

    private val _getFilesSharedByLinkData = MutableLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesSharedByLinkData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesSharedByLinkData

    private val _getFilesAvailableOfflineData = MutableLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesAvailableOfflineData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesAvailableOfflineData

    private val _getSearchedFilesData = MutableLiveData<Event<UIResult<List<OCFile>>>>()
    val getSearchedFilesData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getSearchedFilesData

    private fun getFilesList(folderId: Long) {
        val filesListLiveData: LiveData<List<OCFile>> =
            getFolderContentAsLiveDataUseCase.execute(GetFolderContentAsLiveDataUseCase.Params(folderId = folderId))

        _getFilesListStatusLiveData.addSource(filesListLiveData) {
            _getFilesListStatusLiveData.postValue(Event(UIResult.Success(it)))
        }
    }

    fun getSharedByLinkFilesList() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getFilesSharedByLinkUseCase.execute(GetFilesSharedByLinkUseCase.Params(owner = file.owner)).let {
                when (it) {
                    is UseCaseResult.Error -> _getFilesSharedByLinkData.postValue(Event(UIResult.Error(it.getThrowableOrNull())))
                    is UseCaseResult.Success -> _getFilesSharedByLinkData.postValue(Event(UIResult.Success(it.getDataOrNull())))
                }
            }
        }
    }

    fun getAvailableOfflineFilesList() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getFilesAvailableOfflineUseCase.execute(GetFilesAvailableOfflineUseCase.Params(owner = file.owner)).let {
                when (it) {
                    is UseCaseResult.Error -> _getFilesAvailableOfflineData.postValue(Event(UIResult.Error(it.getThrowableOrNull())))
                    is UseCaseResult.Success -> _getFilesAvailableOfflineData.postValue(Event(UIResult.Success(it.getDataOrNull())))
                }
            }
        }
    }

    private fun getSearchFilesList(fileListOption: FileListOption, folderId: Long, searchText: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getSearchFolderContentUseCase.execute(GetSearchFolderContentUseCase.Params(
                fileListOption = fileListOption,
                folderId = folderId,
                search = searchText)
            ).let {
                when (it) {
                    is UseCaseResult.Error -> _getSearchedFilesData.postValue(Event(UIResult.Error(it.getThrowableOrNull())))
                    is UseCaseResult.Success -> _getSearchedFilesData.postValue(Event(UIResult.Success(it.getDataOrNull())))
                }
            }
        }
    }

    private fun refreshFilesList(remotePath: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            _getFilesListStatusLiveData.postValue(Event(UIResult.Loading()))
            refreshFolderFromServerAsyncUseCase.execute(RefreshFolderFromServerAsyncUseCase.Params(remotePath = remotePath))
        }
    }

    fun listCurrentDirectory() {
        getFilesList(file.id!!)
    }

    fun listDirectory(directory: OCFile) {
        file = directory
        getFilesList(directory.id!!)
    }

    fun listSearchCurrentDirectory(fileListOption: FileListOption, searchText: String) {
        getSearchFilesList(fileListOption, file.id!!, searchText)
    }

    fun refreshDirectory() {
        refreshFilesList(file.remotePath)
    }

    fun getFile(): OCFile{
        return file
    }

    fun sortList(files: List<OCFile>): List<OCFile> {
        val sortOrderSaved = PreferenceManager.getSortOrder(contextProvider.getContext(), FileStorageUtils.FILE_DISPLAY_SORT)
        val ascendingModeSaved = PreferenceManager.getSortAscending(contextProvider.getContext(), FileStorageUtils.FILE_DISPLAY_SORT)

        return FileStorageUtils.sortFolder(
            files, sortOrderSaved,
            ascendingModeSaved
        )
    }
}

