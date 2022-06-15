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
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.domain.BaseUseCase
import timber.log.Timber

class RetryFailedUploadsUseCase(
    private val context: Context,
) : BaseUseCase<Unit, Unit>() {

    override fun run(params: Unit) {

        val uploadsStorageManager = UploadsStorageManager(context.contentResolver)
        val failedUploads = uploadsStorageManager.failedUploads

        if (failedUploads.isEmpty()) {
            Timber.d("There are no failed uploads to retry.")
            return
        }
        failedUploads.forEach { upload ->
            if (isContentUri(context = context, upload = upload)) {
                RetryUploadFromContentUriUseCase(context).execute(RetryUploadFromContentUriUseCase.Params(upload.uploadId))
            } else {
                RetryUploadFromSystemUseCase(context).execute(RetryUploadFromSystemUseCase.Params(upload.uploadId))
            }
        }
    }

    private fun isContentUri(context: Context, upload: OCUpload): Boolean {
        return DocumentFile.isDocumentUri(context, Uri.parse(upload.localPath))
    }

}
