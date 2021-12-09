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
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFolderContentAsLiveDataUseCase
import com.owncloud.android.domain.files.usecases.RefreshFolderFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch

class MainFileListViewModel(
    private val getFolderContentAsLiveDataUseCase: GetFolderContentAsLiveDataUseCase,
    private val refreshFolderFromServerAsyncUseCase: RefreshFolderFromServerAsyncUseCase,
    private val contextProvider: ContextProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private lateinit var file: OCFile

    private val _getFilesListStatusLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesListStatusLiveData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesListStatusLiveData

    private fun getFilesList(folderId: Long) {
        val filesListLiveData: LiveData<List<OCFile>> =
            getFolderContentAsLiveDataUseCase.execute(GetFolderContentAsLiveDataUseCase.Params(folderId = folderId))

        _getFilesListStatusLiveData.addSource(filesListLiveData) {
            _getFilesListStatusLiveData.postValue(Event(UIResult.Success(it)))
        }
    }

    private fun refreshFilesList(remotePath: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            _getFilesListStatusLiveData.postValue(Event(UIResult.Loading()))
            refreshFolderFromServerAsyncUseCase.execute(RefreshFolderFromServerAsyncUseCase.Params(remotePath = remotePath))
        }
    }

    fun listDirectory(directory: OCFile) {
        file = directory
        getFilesList(directory.id!!)
    }

    fun refreshDirectory() {
        refreshFilesList(file.remotePath)
    }
}


