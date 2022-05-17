/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.usecases

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import timber.log.Timber
import java.io.File

/**
 * General use case to upload a file from the Storage Access Framework.
 *
 * We use this use case to upload new files when clicking the FAB -> Upload from files
 *
 * It stores the upload in the database and then enqueue a new worker to upload the single file
 */
class UploadFilesFromSAFUseCase(
    private val workManager: WorkManager,
) : BaseUseCase<Unit, UploadFilesFromSAFUseCase.Params>() {

    override fun run(params: Params) {
        params.listOfContentUris.forEach { contentUri ->
            val documentFile = DocumentFile.fromSingleUri(MainApp.appContext.applicationContext, contentUri)

            if (documentFile == null) {
                Timber.w("Upload of $contentUri won't be enqueued. We were not able to find it in the local storage")
                return@forEach
            }

            val uploadId = storeInUploadsDatabase(
                documentFile = documentFile,
                uploadPath = params.uploadFolderPath.plus(File.separator).plus(documentFile.name),
                accountName = params.accountName,
            )

            enqueueSingleUpload(
                contentUri = documentFile.uri,
                uploadPath = params.uploadFolderPath.plus(File.separator).plus(documentFile.name),
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
    ): Long {
        val uploadStorageManager = UploadsStorageManager(MainApp.appContext.contentResolver)

        val ocUpload = OCUpload(documentFile.uri.toString(), uploadPath, accountName).apply {
            fileSize = documentFile.length()
            isForceOverwrite = false
            createdBy = UploadEnqueuedBy.ENQUEUED_BY_USER.ordinal
            localAction = UploadBehavior.COPY.ordinal
            uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS
        }

        return uploadStorageManager.storeUpload(ocUpload).also {
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
        val uploadFileFromContentUriUseCase = UploadFileFromContentUriUseCase(workManager)
        val uploadFileParams = UploadFileFromContentUriUseCase.Params(
            contentUri = contentUri,
            uploadPath = uploadPath,
            lastModifiedInSeconds = lastModifiedInSeconds,
            behavior = UploadBehavior.COPY.toString(),
            accountName = accountName,
            uploadIdInStorageManager = uploadIdInStorageManager,
            wifiOnly = false,
            chargingOnly = false,
            transferTag = UploadEnqueuedBy.ENQUEUED_BY_USER.toTransferTag()
        )
        uploadFileFromContentUriUseCase.execute(uploadFileParams)
    }

    data class Params(
        val accountName: String,
        val listOfContentUris: List<Uri>,
        val uploadFolderPath: String,
    )
}
