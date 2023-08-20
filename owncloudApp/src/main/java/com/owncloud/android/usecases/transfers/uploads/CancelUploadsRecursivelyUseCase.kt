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

package com.owncloud.android.usecases.transfers.uploads

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.extensions.getWorkInfoByTags
import com.owncloud.android.workers.UploadFileFromContentUriWorker
import com.owncloud.android.workers.UploadFileFromFileSystemWorker
import timber.log.Timber

/**
 * Cancel every pending upload for a file or folder and all its content.
 * Note that cancellation is a best-effort policy and work that is
 * already executing may continue to run.
 */
class CancelUploadsRecursivelyUseCase(
    private val workManager: WorkManager,
    private val transferRepository: TransferRepository,
    private val localStorageProvider: LocalStorageProvider,
    private val getFolderContentUseCase: GetFolderContentUseCase,
) : BaseUseCase<Unit, CancelUploadsRecursivelyUseCase.Params>() {

    private lateinit var currentAndPendingTransfers: List<OCTransfer>
    private lateinit var uploadsWorkInfos: List<WorkInfo>

    override fun run(params: Params) {
        currentAndPendingTransfers = transferRepository.getCurrentAndPendingTransfers()

        val uploadsFromContentUriWorkInfos = workManager.getWorkInfoByTags(
            listOf(
                params.accountName,
                UploadFileFromContentUriWorker::class.java.name
            )
        )
        val uploadsFromFileSystemWorkInfos = workManager.getWorkInfoByTags(
            listOf(
                params.accountName,
                UploadFileFromFileSystemWorker::class.java.name
            )
        )
        uploadsWorkInfos = uploadsFromContentUriWorkInfos + uploadsFromFileSystemWorkInfos

        val files = params.files
        files.forEach { file ->
            cancelRecursively(file)
        }
    }

    private fun cancelRecursively(file: OCFile) {
        if (file.isFolder) {
            val result = getFolderContentUseCase(GetFolderContentUseCase.Params(file.id!!))
            val files = result.getDataOrNull()
            files?.forEach { fileInFolder ->
                cancelRecursively(fileInFolder)
            }
        } else {
            // Check if there are pending uploads for this file
            // FirstOrNull because there should not be 2 uploads with same owner and remote path at the same time
            val uploadForFile = currentAndPendingTransfers.firstOrNull {
                file.owner == it.accountName && file.remotePath == it.remotePath
            }
            uploadForFile?.let { upload ->
                val workersToCancel = uploadsWorkInfos.filter { it.tags.contains(upload.id.toString()) }

                workersToCancel.forEach {
                    workManager.cancelWorkById(it.id)
                    Timber.i("Upload with id ${upload.id} has been cancelled.")
                }

                localStorageProvider.deleteCacheIfNeeded(upload)

                transferRepository.deleteTransferById(upload.id!!)
            }
        }
    }

    data class Params(
        val files: List<OCFile>,
        val accountName: String
    )
}
