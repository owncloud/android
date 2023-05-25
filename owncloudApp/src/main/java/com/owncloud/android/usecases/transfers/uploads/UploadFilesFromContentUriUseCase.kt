/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.usecases.transfers.uploads

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.owncloud.android.MainApp
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.domain.transfers.model.UploadEnqueuedBy
import timber.log.Timber

/**
 * General use case to upload a file from the Storage Access Framework.
 *
 * We use this use case to upload new files when clicking the FAB -> Upload from files
 *
 * It stores the upload in the database and then enqueue a new worker to upload the single file
 */
class UploadFilesFromContentUriUseCase(
    private val uploadFileFromContentUriUseCase: UploadFileFromContentUriUseCase,
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, UploadFilesFromContentUriUseCase.Params>() {

    override fun run(params: Params) {
        params.listOfContentUris.forEach { contentUri ->
            val documentFile = DocumentFile.fromSingleUri(MainApp.appContext.applicationContext, contentUri)

            if (documentFile == null) {
                Timber.w("Upload of $contentUri won't be enqueued. We were not able to find it in the local storage")
                return@forEach
            }

            val uploadId = storeInUploadsDatabase(
                documentFile = documentFile,
                uploadPath = params.uploadFolderPath.plus(documentFile.name),
                accountName = params.accountName,
                spaceId = params.spaceId,
            )

            enqueueSingleUpload(
                contentUri = documentFile.uri,
                uploadPath = params.uploadFolderPath.plus(documentFile.name),
                lastModifiedInSeconds = documentFile.lastModified().div(1_000).toString(),
                accountName = params.accountName,
                uploadIdInStorageManager = uploadId,
            )
        }
    }

    private fun storeInUploadsDatabase(
        documentFile: DocumentFile,
        uploadPath: String,
        accountName: String,
        spaceId: String?,
    ): Long {
        val ocTransfer = OCTransfer(
            localPath = documentFile.uri.toString(),
            remotePath = uploadPath,
            accountName = accountName,
            fileSize = documentFile.length(),
            status = TransferStatus.TRANSFER_QUEUED,
            localBehaviour = UploadBehavior.COPY,
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
        contentUri: Uri,
        lastModifiedInSeconds: String,
        uploadIdInStorageManager: Long,
        uploadPath: String,
    ) {
        val uploadFileParams = UploadFileFromContentUriUseCase.Params(
            contentUri = contentUri,
            uploadPath = uploadPath,
            lastModifiedInSeconds = lastModifiedInSeconds,
            behavior = UploadBehavior.COPY.toString(),
            accountName = accountName,
            uploadIdInStorageManager = uploadIdInStorageManager,
            wifiOnly = false,
            chargingOnly = false
        )
        uploadFileFromContentUriUseCase.execute(uploadFileParams)
    }

    data class Params(
        val accountName: String,
        val listOfContentUris: List<Uri>,
        val uploadFolderPath: String,
        val spaceId: String?,
    )
}
