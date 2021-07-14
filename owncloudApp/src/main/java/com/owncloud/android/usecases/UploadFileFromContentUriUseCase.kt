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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.workers.UploadFileFromContentUriWorker
import timber.log.Timber

class UploadFileFromContentUriUseCase(
    private val workManager: WorkManager
) : BaseUseCase<Unit, UploadFileFromContentUriUseCase.Params>() {

    override fun run(params: Params) {
        val inputData = workDataOf(
            UploadFileFromContentUriWorker.KEY_PARAM_ACCOUNT_NAME to params.accountName,
            UploadFileFromContentUriWorker.KEY_PARAM_BEHAVIOR to params.behavior,
            UploadFileFromContentUriWorker.KEY_PARAM_CONTENT_URI to params.contentUri.toString(),
            UploadFileFromContentUriWorker.KEY_PARAM_LAST_MODIFIED to params.lastModifiedInSeconds,
            UploadFileFromContentUriWorker.KEY_PARAM_UPLOAD_PATH to params.uploadPath,
            UploadFileFromContentUriWorker.KEY_PARAM_UPLOAD_ID to params.uploadIdInStorageManager
        )

        val networkRequired = if (params.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkRequired)
            .build()

        val uploadFileFromContentUriWorker = OneTimeWorkRequestBuilder<UploadFileFromContentUriWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(params.accountName)
            .addTag(params.uploadIdInStorageManager.toString())
            .addTag(UploadFileFromContentUriWorker.TRANSFER_TAG_CAMERA_UPLOAD)
            .build()

        workManager.enqueue(uploadFileFromContentUriWorker)
        Timber.i("Upload of ${params.contentUri.path} has been enqueued.")
    }

    data class Params(
        val accountName: String,
        val contentUri: Uri,
        val lastModifiedInSeconds: String,
        val behavior: String,
        val uploadPath: String,
        val uploadIdInStorageManager: Long,
        val wifiOnly: Boolean
    )
}
