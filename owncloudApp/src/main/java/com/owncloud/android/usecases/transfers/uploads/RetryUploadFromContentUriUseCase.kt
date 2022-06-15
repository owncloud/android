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
package com.owncloud.android.usecases.transfers.uploads

import android.content.Context
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.camerauploads.model.UploadBehavior

class RetryUploadFromContentUriUseCase(
    private val context: Context,
) : BaseUseCase<Unit, RetryUploadFromContentUriUseCase.Params>() {

    override fun run(params: Params) {

        val uploadsStorageManager = UploadsStorageManager(context.contentResolver)
        val failedUploads = uploadsStorageManager.failedUploads
        val filteredUploads = failedUploads.filter { it.uploadId == params.uploadIdInStorageManager }
        val uploadToRetry = filteredUploads.firstOrNull()

        uploadToRetry ?: return

        val workManager = WorkManager.getInstance(context)
        UploadFileFromContentUriUseCase(workManager).execute(
            UploadFileFromContentUriUseCase.Params(
                accountName = uploadToRetry.accountName,
                contentUri = uploadToRetry.localPath.toUri(),
                lastModifiedInSeconds = (uploadToRetry.uploadEndTimestamp / 1000).toString(),
                behavior = UploadBehavior.fromLegacyLocalBehavior(uploadToRetry.localAction).name,
                uploadPath = uploadToRetry.remotePath,
                uploadIdInStorageManager = uploadToRetry.uploadId,
                wifiOnly = false,
                chargingOnly = false
            )
        )
        uploadsStorageManager.updateUpload(uploadToRetry.apply { uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS })
    }

    data class Params(
        val uploadIdInStorageManager: Long,
    )
}
