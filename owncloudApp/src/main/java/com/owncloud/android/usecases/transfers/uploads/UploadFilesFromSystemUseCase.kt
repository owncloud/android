/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.usecases.transfers.uploads

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.domain.transfers.model.UploadEnqueuedBy
import timber.log.Timber
import java.io.File

/**
 * General use case to upload a file from the File System.
 *
 * We use this to upload files from:
 * - (FAB) Picture from camera
 * - Share with oC - Plain Text
 * - Share with oC - Files
 * - Conflicts - Keep both
 *
 * It stores the upload in the database and then enqueue a new worker to upload the single file
 */
class UploadFilesFromSystemUseCase(
    private val uploadFileFromSystemUseCase: UploadFileFromSystemUseCase,
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, UploadFilesFromSystemUseCase.Params>() {

    override fun run(params: Params) {
        params.listOfLocalPaths.forEach { localPath ->
            val localFile = File(localPath)

            if (!localFile.exists()) {
                Timber.w("Upload of $localPath won't be enqueued. We were not able to find it in the local storage")
                return@forEach
            }

            val uploadId = storeInUploadsDatabase(
                localFile = localFile,
                uploadPath = params.uploadFolderPath.plus(localFile.name),
                accountName = params.accountName,
                spaceId = params.spaceId,
            )

            enqueueSingleUpload(
                localPath = localFile.absolutePath,
                uploadPath = params.uploadFolderPath.plus(localFile.name),
                lastModifiedInSeconds = localFile.lastModified().div(1_000).toString(),
                accountName = params.accountName,
                uploadIdInStorageManager = uploadId,
            )
        }
    }

    private fun storeInUploadsDatabase(
        localFile: File,
        uploadPath: String,
        accountName: String,
        spaceId: String?,
    ): Long {
        val ocTransfer = OCTransfer(
            localPath = localFile.absolutePath,
            remotePath = uploadPath,
            accountName = accountName,
            fileSize = localFile.length(),
            status = TransferStatus.TRANSFER_QUEUED,
            localBehaviour = UploadBehavior.MOVE,
            forceOverwrite = false,
            createdBy = UploadEnqueuedBy.ENQUEUED_BY_USER,
            spaceId = spaceId,
        )

        return transferRepository.saveTransfer(ocTransfer).also {
            Timber.i("Upload of $uploadPath has been stored in the uploads database with id: $it")
        }
    }

    private fun enqueueSingleUpload(
        accountName: String,
        localPath: String,
        lastModifiedInSeconds: String,
        uploadIdInStorageManager: Long,
        uploadPath: String,
    ) {
        val uploadFileParams = UploadFileFromSystemUseCase.Params(
            accountName = accountName,
            localPath = localPath,
            lastModifiedInSeconds = lastModifiedInSeconds,
            behavior = UploadBehavior.MOVE.toString(),
            uploadPath = uploadPath,
            uploadIdInStorageManager = uploadIdInStorageManager
        )
        uploadFileFromSystemUseCase.execute(uploadFileParams)
    }

    data class Params(
        val accountName: String,
        val listOfLocalPaths: List<String>,
        val uploadFolderPath: String,
        val spaceId: String?,
    )
}
