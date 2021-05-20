/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.ui.files.operations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.MoveFileUseCase
import com.owncloud.android.domain.files.usecases.RemoveFileUseCase
import com.owncloud.android.domain.files.usecases.RenameFileUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber

class FileOperationViewModel(
    private val moveFileUseCase: MoveFileUseCase,
    private val removeFileUseCase: RemoveFileUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val contextProvider: ContextProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _moveFileLiveData = MediatorLiveData<Event<UIResult<OCFile>>>()
    val moveFileLiveData: LiveData<Event<UIResult<OCFile>>> = _moveFileLiveData

    private val _removeFileLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val removeFileLiveData: LiveData<Event<UIResult<List<OCFile>>>> = _removeFileLiveData

    private val _renameFileLiveData = MediatorLiveData<Event<UIResult<OCFile>>>()
    val renameFileLiveData: LiveData<Event<UIResult<OCFile>>> = _renameFileLiveData

    fun performOperation(fileOperation: FileOperation) {
        when (fileOperation) {
            is FileOperation.MoveOperation -> moveOperation(fileOperation)
            is FileOperation.RemoveOperation -> removeOperation(fileOperation)
            is FileOperation.RenameOperation -> renameOperation(fileOperation)
        }
    }

    private fun moveOperation(fileOperation: FileOperation.MoveOperation) {
        runOperation(
            liveData = _moveFileLiveData,
            useCase = moveFileUseCase,
            useCaseParams = MoveFileUseCase.Params(fileOperation.listOfFilesToMove, fileOperation.targetFolder),
            postValue = fileOperation.targetFolder
        )
    }

    private fun removeOperation(fileOperation: FileOperation.RemoveOperation) {
        runOperation(
            liveData = _removeFileLiveData,
            useCase = removeFileUseCase,
            useCaseParams = RemoveFileUseCase.Params(fileOperation.listOfFilesToRemove, fileOperation.removeOnlyLocalCopy),
            postValue = fileOperation.listOfFilesToRemove
        )
    }

    private fun renameOperation(fileOperation: FileOperation.RenameOperation) {
        runOperation(
            liveData = _renameFileLiveData,
            useCase = renameFileUseCase,
            useCaseParams = RenameFileUseCase.Params(fileOperation.ocFileToRename, fileOperation.newName),
            postValue = fileOperation.ocFileToRename
        )
    }

    private fun <Type, Params, PostResult> runOperation(
        liveData: MediatorLiveData<Event<UIResult<PostResult>>>,
        useCase: BaseUseCaseWithResult<Type, Params>,
        useCaseParams: Params,
        postValue: PostResult? = null
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            liveData.postValue(Event(UIResult.Loading()))

            if (!contextProvider.isConnected()) {
                liveData.postValue(Event(UIResult.Error(error = NoNetworkConnectionException())))
                Timber.w("${useCase.javaClass.simpleName} will not be executed due to lack of network connection")
                return@launch
            }

            val useCaseResult = useCase.execute(useCaseParams).also {
                Timber.d("Use case executed: ${useCase.javaClass.simpleName} with result: $it")
            }

            when (useCaseResult) {
                is UseCaseResult.Success -> {
                    liveData.postValue(Event(UIResult.Success(postValue)))
                }
                is UseCaseResult.Error -> {
                    liveData.postValue(Event(UIResult.Error(error = useCaseResult.throwable)))
                }
            }
        }
    }
}
