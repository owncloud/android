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

package com.owncloud.android.presentation.conflicts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdAsStreamUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileInConflictUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConflictsResolveViewModel(
    private val downloadFileUseCase: DownloadFileUseCase,
    private val uploadFileInConflictUseCase: UploadFileInConflictUseCase,
    private val uploadFilesFromSystemUseCase: UploadFilesFromSystemUseCase,
    getFileByIdAsStreamUseCase: GetFileByIdAsStreamUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    ocFile: OCFile,
) : ViewModel() {

    val currentFile: StateFlow<OCFile?> =
        getFileByIdAsStreamUseCase.execute(GetFileByIdAsStreamUseCase.Params(ocFile.id!!))
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = ocFile
            )

    fun downloadFile() {
        val fileToDownload = currentFile.value ?: return
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            downloadFileUseCase.execute(
                DownloadFileUseCase.Params(
                    accountName = fileToDownload.owner,
                    file = fileToDownload
                )
            )
        }
    }

    fun uploadFileInConflict() {
        val fileToUpload = currentFile.value ?: return
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFileInConflictUseCase.execute(
                UploadFileInConflictUseCase.Params(
                    accountName = fileToUpload.owner,
                    localPath = fileToUpload.storagePath!!,
                    uploadFolderPath = fileToUpload.getParentRemotePath(),
                    spaceId = fileToUpload.spaceId,
                )
            )
        }
    }

    fun uploadFileFromSystem() {
        val fileToUpload = currentFile.value ?: return
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFilesFromSystemUseCase.execute(
                UploadFilesFromSystemUseCase.Params(
                    accountName = fileToUpload.owner,
                    listOfLocalPaths = listOf(fileToUpload.storagePath!!),
                    uploadFolderPath = fileToUpload.getParentRemotePath(),
                    spaceId = fileToUpload.spaceId,
                )
            )
        }
    }
}
