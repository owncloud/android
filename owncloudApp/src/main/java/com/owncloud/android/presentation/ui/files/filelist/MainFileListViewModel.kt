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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainFileListViewModel(
    private val getFolderContentUseCase: GetFolderContentUseCase
) : ViewModel() {

    private var file: OCFile? = null

    private val _getFilesListStatusLiveData = MutableLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesListStatusLiveData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesListStatusLiveData

    private fun getFilesList(folderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _getFilesListStatusLiveData.postValue(Event(UIResult.Loading()))
            getFolderContentUseCase.execute(GetFolderContentUseCase.Params(folderId = folderId)).let {
                when (it) {
                    is UseCaseResult.Error -> _getFilesListStatusLiveData.postValue(Event(UIResult.Error(it.getThrowableOrNull())))
                    is UseCaseResult.Success -> _getFilesListStatusLiveData.postValue(Event(UIResult.Success(it.getDataOrNull())))
                }
            }
        }
    }

    fun listDirectory(directory: OCFile) {
        getFilesList(directory.id!!)
    }
}
