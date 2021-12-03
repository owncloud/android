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
import com.owncloud.android.R
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFilesSharedByLinkUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsLiveDataUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider

class MainFileListViewModel(
    private val getFolderContentAsLiveDataUseCase: GetFolderContentAsLiveDataUseCase,
    private val getFilesSharedByLinkUseCase: GetFilesSharedByLinkUseCase,
    private val contextProvider: ContextProvider,
) : ViewModel() {

    private var file: OCFile? = null

    private val _getFilesListStatusLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesListStatusLiveData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesListStatusLiveData

    private val _getFilesSharedByLinkData = MutableLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesSharedByLinkData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesSharedByLinkData

    private val _numberOfFilesPerType = MutableLiveData<Event<UIResult<Pair<Int, Int>>>>()
    val numberOfFilesPerType: LiveData<Event<UIResult<Pair<Int, Int>>>>
        get() = _numberOfFilesPerType

    private val _footerText = MutableLiveData<Event<UIResult<String>>>()
    val footerText: LiveData<Event<UIResult<String>>>
        get() = _footerText

    private fun getFilesList(folderId: Long) {
        val filesListLiveData: LiveData<List<OCFile>> =
            getFolderContentAsLiveDataUseCase.execute(GetFolderContentAsLiveDataUseCase.Params(folderId = folderId))

        _getFilesListStatusLiveData.addSource(filesListLiveData) {
            _getFilesListStatusLiveData.postValue(Event(UIResult.Success(it)))
        }
    }

    fun getSharedByLinkFilesList(owner: String) {
        getFilesSharedByLinkUseCase.execute(GetFilesSharedByLinkUseCase.Params(owner = owner)).let {
            when (it) {
                is UseCaseResult.Error -> _getFilesSharedByLinkData.postValue(Event(UIResult.Error(it.getThrowableOrNull())))
                is UseCaseResult.Success -> _getFilesSharedByLinkData.postValue(Event(UIResult.Success(it.getDataOrNull())))
            }
        }
    }

    fun manageListOfFiles(list: List<OCFile>) {
        var filesCount = 0
        var foldersCount = 0
        val count: Int = list.size
        var file: OCFile
        for (i in 0 until count) {
            file = list[i]
            if (file.isFolder) {
                foldersCount++
            } else {
                if (!file.isHidden) {
                    filesCount++
                }
            }
        }

        _numberOfFilesPerType.postValue(Event(UIResult.Success(Pair(foldersCount, filesCount))))
    }

    fun generateFooterText(filesCount: Int, foldersCount: Int) {
        _footerText.postValue(
            Event(
                UIResult.Success(
                    when {
                        filesCount <= 0 -> {
                            when {
                                foldersCount <= 0 -> {
                                    ""
                                }
                                foldersCount == 1 -> {
                                    contextProvider.getContext().getString(R.string.file_list__footer__folder)
                                }
                                else -> { // foldersCount > 1
                                    contextProvider.getContext().getString(R.string.file_list__footer__folders, foldersCount)
                                }
                            }
                        }
                        filesCount == 1 -> {
                            when {
                                foldersCount <= 0 -> {
                                    contextProvider.getContext().getString(R.string.file_list__footer__file)
                                }
                                foldersCount == 1 -> {
                                    contextProvider.getContext().getString(R.string.file_list__footer__file_and_folder)
                                }
                                else -> { // foldersCount > 1
                                    contextProvider.getContext().getString(R.string.file_list__footer__file_and_folders, foldersCount)
                                }
                            }
                        }
                        else -> {    // filesCount > 1
                            when {
                                foldersCount <= 0 -> {
                                    contextProvider.getContext().getString(R.string.file_list__footer__files, filesCount)
                                }
                                foldersCount == 1 -> {
                                    contextProvider.getContext().getString(R.string.file_list__footer__files_and_folder, filesCount)
                                }
                                else -> { // foldersCount > 1
                                    contextProvider.getContext().getString(
                                        R.string.file_list__footer__files_and_folders, filesCount, foldersCount
                                    )
                                }
                            }
                        }
                    }
                )
            )
        )
    }

    fun listDirectory(directory: OCFile) {
        getFilesList(directory.id!!)
    }
}

