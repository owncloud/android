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

package com.owncloud.android.presentation.viewmodels.conflicts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileInConflictUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import kotlinx.coroutines.launch

class ConflictsResolveViewModel(
    private val downloadFileUseCase: DownloadFileUseCase,
    private val uploadFileInConflictUseCase: UploadFileInConflictUseCase,
    private val uploadFilesFromSystemUseCase: UploadFilesFromSystemUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    fun downloadFile(file: OCFile) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            downloadFileUseCase.execute(
                DownloadFileUseCase.Params(
                    accountName = file.owner,
                    file = file
                )
            )
        }
    }

    fun uploadFileInConflict(ocFile: OCFile) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFileInConflictUseCase.execute(
                UploadFileInConflictUseCase.Params(
                    accountName = ocFile.owner,
                    localPath = ocFile.storagePath!!,
                    uploadFolderPath = ocFile.getParentRemotePath()
                )
            )
        }
    }

    fun uploadFileFromSystem(ocFile: OCFile) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            uploadFilesFromSystemUseCase.execute(
                UploadFilesFromSystemUseCase.Params(
                    accountName = ocFile.owner,
                    listOfLocalPaths = listOf(ocFile.storagePath!!),
                    uploadFolderPath = ocFile.getParentRemotePath()
                )
            )
        }
    }
}
