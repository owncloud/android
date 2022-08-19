/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.domain.transfers.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import java.io.File

class UpdatePendingUploadsPathUseCase(
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, UpdatePendingUploadsPathUseCase.Params>() {

    override fun run(params: Params) {
        transferRepository.clearSuccessfulTransfers()
        val storedUploads = transferRepository.getAllTransfers()
        storedUploads.forEach { upload ->
            transferRepository.updateTransferStorageDirectoryInLocalPath(upload.id!!, params.oldDirectory, params.newDirectory)
        }
        val updatedUploads = transferRepository.getAllTransfers()
        clearUnrelatedTemporalFiles(updatedUploads, params.temporalFoldersForAccounts)
    }

    private fun clearUnrelatedTemporalFiles(uploads: List<OCTransfer>, temporalFoldersForAccounts: List<File>) {
        temporalFoldersForAccounts.forEach { temporalFolderForAccount ->
            cleanTemporalRecursively(temporalFolderForAccount) { temporalFile ->
                if (!uploads.map { it.localPath }.contains(temporalFile.absolutePath)) {
                    temporalFile.delete()
                }
            }
        }
    }

    private fun cleanTemporalRecursively(temporalFolder: File, deleteFileInCaseItIsNotNeeded: (file: File) -> Unit) {
        temporalFolder.listFiles()?.forEach { temporalFile ->
            if (temporalFile.isDirectory) {
                cleanTemporalRecursively(temporalFile, deleteFileInCaseItIsNotNeeded)
            } else {
                deleteFileInCaseItIsNotNeeded(temporalFile)
            }

        }
    }

    data class Params(
        val oldDirectory: String,
        val newDirectory: String,
        val temporalFoldersForAccounts: List<File>
    )
}
