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
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.RemoveFileUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber

class FileOperationViewModel(
    private val removeFileUseCase: RemoveFileUseCase,
    private val contextProvider: ContextProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _removeFileLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val removeFileLiveData: LiveData<Event<UIResult<List<OCFile>>>> = _removeFileLiveData

    fun performOperation(fileOperation: FileOperation) {
        when (fileOperation) {
            is FileOperation.RemoveOperation -> removeOperation(fileOperation)
        }
    }

    private fun removeOperation(fileOperation: FileOperation.RemoveOperation) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            _removeFileLiveData.postValue(Event(UIResult.Loading()))

            if (!contextProvider.isConnected()) {
                _removeFileLiveData.postValue(Event(UIResult.Error(error = NoNetworkConnectionException())))
                Timber.w("${removeFileUseCase.javaClass.simpleName} will not be executed due to lack of network connection")
                return@launch
            }
            val useCaseResult =
                removeFileUseCase.execute(RemoveFileUseCase.Params(fileOperation.listOfFilesToRemove, fileOperation.removeOnlyLocalCopy))

            Timber.d("Use case executed: ${removeFileUseCase.javaClass.simpleName} with result: $useCaseResult")

            when (useCaseResult) {
                is UseCaseResult.Success -> {
                    _removeFileLiveData.postValue(Event(UIResult.Success(fileOperation.listOfFilesToRemove)))
                }
                is UseCaseResult.Error -> {
                    _removeFileLiveData.postValue(Event(UIResult.Error(error = useCaseResult.throwable)))
                }
            }

        }
    }
}
