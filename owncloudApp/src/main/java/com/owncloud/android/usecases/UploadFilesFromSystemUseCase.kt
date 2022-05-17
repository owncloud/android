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

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.workers.UploadFileFromFileSystemWorker
import timber.log.Timber
import java.io.File

/**
 * General use case to upload a file from the File System.
 *
 * We use this to upload files from:
 * - (FAB) Picture from camera
 * - Share with oC - Plain Text
 * - Share with oC - Files
 * - TODO: Conflicts - Keep local
 * - Conflicts - Keep both
 *
 * It stores the upload in the database and then enqueue a new worker to upload the single file
 */
class UploadFilesFromSystemUseCase(
    private val workManager: WorkManager,
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
                uploadPath = params.uploadFolderPath.plus(File.separator).plus(localFile.name),
                accountName = params.accountName,
            )

            enqueueSingleUpload(
                localPath = localFile.absolutePath,
                uploadPath = params.uploadFolderPath.plus(File.separator).plus(localFile.name),
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
    ): Long {
        val uploadStorageManager = UploadsStorageManager(MainApp.appContext.contentResolver)

        val ocUpload = OCUpload(localFile.absolutePath, uploadPath, accountName).apply {
            fileSize = localFile.length()
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
        localPath: String,
        lastModifiedInSeconds: String,
        uploadIdInStorageManager: Long,
        uploadPath: String,
    ) {
        val inputData = workDataOf(
            UploadFileFromFileSystemWorker.KEY_PARAM_ACCOUNT_NAME to accountName,
            UploadFileFromFileSystemWorker.KEY_PARAM_BEHAVIOR to UploadBehavior.COPY.name,
            UploadFileFromFileSystemWorker.KEY_PARAM_CONTENT_URI to localPath,
            UploadFileFromFileSystemWorker.KEY_PARAM_LAST_MODIFIED to lastModifiedInSeconds,
            UploadFileFromFileSystemWorker.KEY_PARAM_UPLOAD_PATH to uploadPath,
            UploadFileFromFileSystemWorker.KEY_PARAM_UPLOAD_ID to uploadIdInStorageManager
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadFileFromContentUriWorker = OneTimeWorkRequestBuilder<UploadFileFromFileSystemWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(accountName)
            .build()

        workManager.enqueue(uploadFileFromContentUriWorker)
        Timber.i("Plain upload of $localPath has been enqueued.")
    }

    data class Params(
        val accountName: String,
        val listOfLocalPaths: List<String>,
        val uploadFolderPath: String,
    )
}
