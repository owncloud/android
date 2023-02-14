/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.transfers

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.usecases.ClearSuccessfulTransfersUseCase
import com.owncloud.android.domain.transfers.usecases.GetAllTransfersAsLiveDataUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadForFileUseCase
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadsRecursivelyUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadForFileUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadsRecursivelyUseCase
import com.owncloud.android.usecases.transfers.uploads.ClearFailedTransfersUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryFailedUploadsForAccountUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryFailedUploadsUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.RetryUploadFromSystemUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromContentUriUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import kotlinx.coroutines.launch

class TransfersViewModel(
    private val uploadFilesFromContentUriUseCase: UploadFilesFromContentUriUseCase,
    private val uploadFilesFromSystemUseCase: UploadFilesFromSystemUseCase,
    private val cancelUploadUseCase: CancelUploadUseCase,
    private val retryUploadFromSystemUseCase: RetryUploadFromSystemUseCase,
    private val retryUploadFromContentUriUseCase: RetryUploadFromContentUriUseCase,
    private val retryFailedUploadsForAccountUseCase: RetryFailedUploadsForAccountUseCase,
    private val clearFailedTransfersUseCase: ClearFailedTransfersUseCase,
    private val retryFailedUploadsUseCase: RetryFailedUploadsUseCase,
    private val clearSuccessfulTransfersUseCase: ClearSuccessfulTransfersUseCase,
    getAllTransfersAsLiveDataUseCase: GetAllTransfersAsLiveDataUseCase,
    private val cancelDownloadForFileUseCase: CancelDownloadForFileUseCase,
    private val cancelUploadForFileUseCase: CancelUploadForFileUseCase,
    private val cancelUploadsRecursivelyUseCase: CancelUploadsRecursivelyUseCase,
    private val cancelDownloadsRecursivelyUseCase: CancelDownloadsRecursivelyUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    workManagerProvider: WorkManagerProvider,
) : ViewModel() {

    private val _transfersListLiveData = MediatorLiveData<List<OCTransfer>>()
    val transfersListLiveData: LiveData<List<OCTransfer>>
        get() = _transfersListLiveData

    private val _workInfosListLiveData = MediatorLiveData<List<WorkInfo>>()
    val workInfosListLiveData: LiveData<List<WorkInfo>>
        get() = _workInfosListLiveData

    private var transfersLiveData = getAllTransfersAsLiveDataUseCase.execute(Unit)

    private var workInfosLiveData = workManagerProvider.getRunningUploadsWorkInfosLiveData()

    init {
        _transfersListLiveData.addSource(transfersLiveData) { transfers ->
            _transfersListLiveData.postValue(transfers)
        }
        _workInfosListLiveData.addSource(workInfosLiveData) { workInfos ->
            _workInfosListLiveData.postValue(workInfos)
        }
    }

    fun uploadFilesFromContentUri(
        accountName: String,
        listOfContentUris: List<Uri>,
        uploadFolderPath: String,
        spaceId: String?
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFilesFromContentUriUseCase.execute(
                UploadFilesFromContentUriUseCase.Params(
                    accountName = accountName,
                    listOfContentUris = listOfContentUris,
                    uploadFolderPath = uploadFolderPath,
                    spaceId = spaceId,
                )
            )
        }
    }

    fun uploadFilesFromSystem(
        accountName: String,
        listOfLocalPaths: List<String>,
        uploadFolderPath: String,
        spaceId: String?,
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFilesFromSystemUseCase.execute(
                UploadFilesFromSystemUseCase.Params(
                    accountName = accountName,
                    listOfLocalPaths = listOfLocalPaths,
                    uploadFolderPath = uploadFolderPath,
                    spaceId = spaceId,
                )
            )
        }
    }

    fun cancelUpload(upload: OCTransfer) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            cancelUploadUseCase.execute(
                CancelUploadUseCase.Params(upload = upload)
            )
        }
    }

    fun cancelTransfersForFile(ocFile: OCFile) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            cancelUploadForFileUseCase.execute(CancelUploadForFileUseCase.Params(ocFile))
            cancelDownloadForFileUseCase.execute(CancelDownloadForFileUseCase.Params(ocFile))
        }
    }

    fun cancelTransfersRecursively(ocFiles: List<OCFile>, accountName: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            cancelDownloadsRecursivelyUseCase.execute(CancelDownloadsRecursivelyUseCase.Params(ocFiles, accountName))
        }
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            cancelUploadsRecursivelyUseCase.execute(CancelUploadsRecursivelyUseCase.Params(ocFiles, accountName))
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

    fun retryUploadsForAccount(accountName: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            retryFailedUploadsForAccountUseCase.execute(RetryFailedUploadsForAccountUseCase.Params(accountName))
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
