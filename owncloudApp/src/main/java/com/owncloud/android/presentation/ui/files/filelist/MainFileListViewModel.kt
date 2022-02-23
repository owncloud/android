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

import android.content.Context
import android.os.PowerManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentAsLiveDataUseCase
import com.owncloud.android.domain.files.usecases.GetSearchFolderContentUseCase
import com.owncloud.android.domain.files.usecases.RefreshFolderFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.isDownloadPending
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.launch
import java.io.File

class MainFileListViewModel(
    private val getFolderContentAsLiveDataUseCase: GetFolderContentAsLiveDataUseCase,
    private val getSearchFolderContentUseCase: GetSearchFolderContentUseCase,
    private val refreshFolderFromServerAsyncUseCase: RefreshFolderFromServerAsyncUseCase,
    private val getFileByIdUseCase: GetFileByIdUseCase,
    private val getFileByRemotePathUseCase: GetFileByRemotePathUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val sharedPreferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider,
    private val workManager: WorkManager,
) : ViewModel() {

    private lateinit var file: OCFile

    private val _getFilesListStatusLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val getFilesListStatusLiveData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getFilesListStatusLiveData

    private val _getSearchedFilesData = MutableLiveData<Event<UIResult<List<OCFile>>>>()
    val getSearchedFilesData: LiveData<Event<UIResult<List<OCFile>>>>
        get() = _getSearchedFilesData

    private val _getFileData = MutableLiveData<Event<UIResult<OCFile>>>()
    val getFileData: LiveData<Event<UIResult<OCFile>>>
        get() = _getFileData

    private fun getFilesList(folderId: Long) {
        val filesListLiveData: LiveData<List<OCFile>> =
            getFolderContentAsLiveDataUseCase.execute(GetFolderContentAsLiveDataUseCase.Params(folderId = folderId))

        _getFilesListStatusLiveData.addSource(filesListLiveData) {
            _getFilesListStatusLiveData.postValue(Event(UIResult.Success(it)))
        }
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

    fun getFile(): OCFile {
        return file
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
            var parentDir: OCFile?
            var parentPath: String? = null
            if (file.parentId != FileDataStorageManager.ROOT_PARENT_ID.toLong()) {
                parentPath = File(file.remotePath).parent
                parentPath = if (parentPath.endsWith(File.separator)) parentPath else parentPath + File.separator

                val fileByIdResult = getFileByRemotePathUseCase.execute(
                    GetFileByRemotePathUseCase.Params(
                        owner = file.owner,
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
                        owner = file.owner,
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
            file = parentDir!!

            _getFileData.postValue(Event(UIResult.Success(file)))
        }
    }

    fun isInPowerSaveMode(fragmentActivity: FragmentActivity?): Boolean {
        val powerManager = fragmentActivity?.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    fun fileIsDownloading(file: OCFile, containerActivity: FileFragment.ContainerActivity?): Boolean {
        return workManager.isDownloadPending((containerActivity as FileActivity).account, file)
    }

    companion object {
        private const val RECYCLER_VIEW_PREFERRED = "RECYCLER_VIEW_PREFERRED"
    }
}

