/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.transfers

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.usecases.ClearFailedTransfersUseCase
import com.owncloud.android.domain.transfers.usecases.ClearSuccessfulTransfersUseCase
import com.owncloud.android.domain.transfers.usecases.DeleteTransferWithIdUseCase
import com.owncloud.android.domain.transfers.usecases.GetAllTransfersUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.transfers.uploads.CancelUploadWithIdUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryFailedUploadsUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromSystemUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import kotlinx.coroutines.launch

class TransfersViewModel(
    private val uploadFilesFromContentUriUseCase: UploadFilesFromContentUriUseCase,
    private val uploadFilesFromSystemUseCase: UploadFilesFromSystemUseCase,
    private val cancelUploadWithIdUseCase: CancelUploadWithIdUseCase,
    private val deleteTransferWithIdUseCase: DeleteTransferWithIdUseCase,
    private val retryUploadFromSystemUseCase: RetryUploadFromSystemUseCase,
    private val retryUploadFromContentUriUseCase: RetryUploadFromContentUriUseCase,
    private val clearFailedTransfersUseCase: ClearFailedTransfersUseCase,
    private val retryFailedUploadsUseCase: RetryFailedUploadsUseCase,
    private val clearSuccessfulTransfersUseCase: ClearSuccessfulTransfersUseCase,
    getAllTransfersUseCase: GetAllTransfersUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _transfersListLiveData = MediatorLiveData<List<OCTransfer>>()
    val transfersListLiveData: LiveData<List<OCTransfer>>
        get() = _transfersListLiveData

    private var transfersLiveData = getAllTransfersUseCase.execute(Unit)

    init {
        _transfersListLiveData.addSource(transfersLiveData) { transfers ->
            _transfersListLiveData.postValue(transfers)
        }
    }

    fun uploadFilesFromSAF(
        accountName: String,
        listOfContentUris: List<Uri>,
        uploadFolderPath: String
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFilesFromContentUriUseCase.execute(
                UploadFilesFromContentUriUseCase.Params(
                    accountName = accountName,
                    listOfContentUris = listOfContentUris,
                    uploadFolderPath = uploadFolderPath
                )
            )
        }
    }

    fun uploadFilesFromSystem(
        accountName: String,
        listOfLocalPaths: List<String>,
        uploadFolderPath: String
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFilesFromSystemUseCase.execute(
                UploadFilesFromSystemUseCase.Params(
                    accountName = accountName,
                    listOfLocalPaths = listOfLocalPaths,
                    uploadFolderPath = uploadFolderPath
                )
            )
        }
    }

    fun cancelTransferWithId(id: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            cancelUploadWithIdUseCase.execute(
                CancelUploadWithIdUseCase.Params(uploadId = id)
            )
        }
    }

    fun deleteTransferWithId(id: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            deleteTransferWithIdUseCase.execute(
                DeleteTransferWithIdUseCase.Params(id = id)
            )
        }
    }

    fun retryUploadFromSystem(id: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            retryUploadFromSystemUseCase.execute(
                RetryUploadFromSystemUseCase.Params(uploadIdInStorageManager = id)
            )
        }
    }

    fun retryUploadFromContentUri(id: Long) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            retryUploadFromContentUriUseCase.execute(
                RetryUploadFromContentUriUseCase.Params(uploadIdInStorageManager = id)
            )
        }
    }

    fun clearFailedTransfers() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            clearFailedTransfersUseCase.execute(Unit)
        }
    }

    fun retryFailedTransfers() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            retryFailedUploadsUseCase.execute(Unit)
        }
    }

    fun clearSuccessfulTransfers() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            clearSuccessfulTransfersUseCase.execute(Unit)
        }
    }
}
