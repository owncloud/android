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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_VIDEO
import com.owncloud.android.operations.UploadFileOperation.CREATED_BY_USER
import com.owncloud.android.workers.UploadFileFromContentUriWorker
import com.owncloud.android.workers.UploadFileFromContentUriWorker.Companion.TRANSFER_TAG_CAMERA_UPLOAD
import com.owncloud.android.workers.UploadFileFromContentUriWorker.Companion.TRANSFER_TAG_MANUAL_UPLOAD
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
        val inputData = workDataOf(
            UploadFileFromContentUriWorker.KEY_PARAM_ACCOUNT_NAME to accountName,
            UploadFileFromContentUriWorker.KEY_PARAM_BEHAVIOR to behavior.name,
            UploadFileFromContentUriWorker.KEY_PARAM_CONTENT_URI to contentUri.toString(),
            UploadFileFromContentUriWorker.KEY_PARAM_LAST_MODIFIED to lastModifiedInSeconds,
            UploadFileFromContentUriWorker.KEY_PARAM_UPLOAD_PATH to uploadPath,
            UploadFileFromContentUriWorker.KEY_PARAM_UPLOAD_ID to uploadIdInStorageManager
        )

        val networkRequired = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkRequired)
            .setRequiresCharging(chargingOnly)
            .build()

        val uploadFileFromContentUriWorker = OneTimeWorkRequestBuilder<UploadFileFromContentUriWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(accountName)
            .addTag(uploadIdInStorageManager.toString())
            .addTag(enqueuedBy.toTransferTag())
            .build()

        workManager.enqueue(uploadFileFromContentUriWorker)
        Timber.i("Upload of ${contentUri.path} has been enqueued.")
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

/**
 * Analog to the old LOCAL_BEHAVIOUR but with fixed options.
 *
 * By default, the file will be copied. We do not want to remove the file without user approval.
 *
 * Warning -> Order of elements is really important. The ordinal is used to store the value in the database.
 */
enum class UploadBehavior {
    COPY, MOVE;

    companion object {
        fun fromLegacyLocalBehavior(oldLocalBehavior: Int): UploadBehavior {
            return when (oldLocalBehavior) {
                FileUploader.LOCAL_BEHAVIOUR_MOVE -> MOVE
                FileUploader.LOCAL_BEHAVIOUR_COPY -> COPY
                else -> COPY
            }
        }
    }
}

/**
 * Analog to the old CREATED_BY but with fixed options.
 *
 * By default, we consider that it is enqueued manually.
 *
 * Warning -> Order of elements is really important. The ordinal is used to store the value in the database.
 */
enum class UploadEnqueuedBy {
    ENQUEUED_BY_USER,
    ENQUEUED_AS_CAMERA_UPLOAD_PICTURE,
    ENQUEUED_AS_CAMERA_UPLOAD_VIDEO;

    fun fromLegacyCreatedBy(oldCreatedBy: Int): UploadEnqueuedBy {
        return when (oldCreatedBy) {
            CREATED_BY_USER -> ENQUEUED_BY_USER
            CREATED_AS_CAMERA_UPLOAD_PICTURE -> ENQUEUED_AS_CAMERA_UPLOAD_PICTURE
            CREATED_AS_CAMERA_UPLOAD_VIDEO -> ENQUEUED_AS_CAMERA_UPLOAD_VIDEO
            else -> ENQUEUED_BY_USER
        }
    }

    fun toTransferTag(): String {
        return when (this) {
            ENQUEUED_BY_USER -> TRANSFER_TAG_MANUAL_UPLOAD
            ENQUEUED_AS_CAMERA_UPLOAD_PICTURE -> TRANSFER_TAG_CAMERA_UPLOAD
            ENQUEUED_AS_CAMERA_UPLOAD_VIDEO -> TRANSFER_TAG_CAMERA_UPLOAD
        }
    }
}
