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
import timber.log.Timber
import java.io.File

/**
 * General usecase to upload a file.
 *
 * It stores the upload in the database and then enqueue a new worker to upload the single file
 */
class UploadFileUseCase(
    private val workManager: WorkManager,
) : BaseUseCase<Unit, UploadFileUseCase.Params>() {

    override fun run(params: Params) {
        val documentFile = DocumentFile.fromSingleUri(MainApp.appContext.applicationContext, params.contentUri)

        if (documentFile == null) {
            Timber.w("Upload of ${params.contentUri} won't be enqueued. We were not able to find it in the local storage")
            return
        }

        val uploadId = storeInUploadsDatabase(
            documentFile = documentFile,
            uploadPath = params.uploadFolderPath.plus(File.separator).plus(documentFile.name),
            accountName = params.accountName,
            behavior = params.behavior,
            enqueuedBy = params.enqueuedBy
        )

        enqueueSingleUpload(
            contentUri = documentFile.uri,
            uploadPath = params.uploadFolderPath.plus(File.separator).plus(documentFile.name),
            lastModifiedInSeconds = documentFile.lastModified().div(1_000).toString(),
            behavior = params.behavior,
            accountName = params.accountName,
            uploadIdInStorageManager = uploadId,
            wifiOnly = params.wifiOnly,
            chargingOnly = params.chargingOnly,
            enqueuedBy = params.enqueuedBy
        )
    }

    private fun storeInUploadsDatabase(
        documentFile: DocumentFile,
        uploadPath: String,
        accountName: String,
        behavior: UploadBehavior,
        enqueuedBy: UploadEnqueuedBy,
    ): Long {
        val uploadStorageManager = UploadsStorageManager(MainApp.appContext.contentResolver)

        val ocUpload = OCUpload(documentFile.uri.toString(), uploadPath, accountName).apply {
            fileSize = documentFile.length()
            isForceOverwrite = false
            createdBy = enqueuedBy.ordinal
            localAction = behavior.ordinal
            uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS
        }

        return uploadStorageManager.storeUpload(ocUpload).also {
            Timber.i("Upload of $uploadPath has been stored in the uploads database with id: $it")
        }
    }

    private fun enqueueSingleUpload(
        accountName: String,
        behavior: UploadBehavior,
        chargingOnly: Boolean,
        contentUri: Uri,
        enqueuedBy: UploadEnqueuedBy,
        lastModifiedInSeconds: String,
        uploadIdInStorageManager: Long,
        uploadPath: String,
        wifiOnly: Boolean,
    ) {
        val uploadFileFromContentUriUseCase = UploadFileFromContentUriUseCase(workManager)
        val uploadFileParams = UploadFileFromContentUriUseCase.Params(
            contentUri = contentUri,
            uploadPath = uploadPath,
            lastModifiedInSeconds = lastModifiedInSeconds,
            behavior = behavior.toString(),
            accountName = accountName,
            uploadIdInStorageManager = uploadIdInStorageManager,
            wifiOnly = wifiOnly,
            chargingOnly = chargingOnly,
            transferTag = enqueuedBy.toTransferTag()
        )
        uploadFileFromContentUriUseCase.execute(uploadFileParams)
    }

    data class Params(
        val accountName: String,
        val behavior: UploadBehavior,
        val chargingOnly: Boolean,
        val contentUri: Uri,
        val enqueuedBy: UploadEnqueuedBy,
        val uploadSourceTag: String,
        val uploadFolderPath: String,
        val wifiOnly: Boolean,
    )
}
