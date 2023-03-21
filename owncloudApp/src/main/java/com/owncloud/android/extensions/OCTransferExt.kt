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

package com.owncloud.android.extensions

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import com.owncloud.android.R
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus

@StringRes
fun OCTransfer.statusToStringRes(): Int {
     return when (status) {
         TransferStatus.TRANSFER_IN_PROGRESS -> R.string.uploader_upload_in_progress_ticker
         TransferStatus.TRANSFER_SUCCEEDED -> R.string.uploads_view_upload_status_succeeded
         TransferStatus.TRANSFER_QUEUED -> R.string.uploads_view_upload_status_queued
         TransferStatus.TRANSFER_FAILED -> when (lastResult) {
             TransferResult.CREDENTIAL_ERROR -> R.string.uploads_view_upload_status_failed_credentials_error
             TransferResult.FOLDER_ERROR -> R.string.uploads_view_upload_status_failed_folder_error
             TransferResult.FILE_NOT_FOUND -> R.string.uploads_view_upload_status_failed_localfile_error
             TransferResult.FILE_ERROR -> R.string.uploads_view_upload_status_failed_file_error
             TransferResult.PRIVILEGES_ERROR -> R.string.uploads_view_upload_status_failed_permission_error
             TransferResult.NETWORK_CONNECTION -> R.string.uploads_view_upload_status_failed_connection_error
             TransferResult.DELAYED_FOR_WIFI -> R.string.uploads_view_upload_status_waiting_for_wifi
             TransferResult.CONFLICT_ERROR -> R.string.uploads_view_upload_status_conflict
             TransferResult.SERVICE_INTERRUPTED -> R.string.uploads_view_upload_status_service_interrupted
             TransferResult.SERVICE_UNAVAILABLE -> R.string.service_unavailable
             TransferResult.QUOTA_EXCEEDED -> R.string.failed_upload_quota_exceeded_text
             TransferResult.SSL_RECOVERABLE_PEER_UNVERIFIED -> R.string.ssl_certificate_not_trusted
             TransferResult.UNKNOWN -> R.string.uploads_view_upload_status_unknown_fail
             // Should not get here; cancelled uploads should be wiped out
             TransferResult.CANCELLED -> R.string.uploads_view_upload_status_cancelled
             // Should not get here; status should be UPLOAD_SUCCESS
             TransferResult.UPLOADED -> R.string.uploads_view_upload_status_succeeded
             // We don't know the specific forbidden error message because it is not being saved in transfers storage
             TransferResult.SPECIFIC_FORBIDDEN -> R.string.uploads_view_upload_status_failed_permission_error
             // We don't know the specific unavailable service error message because it is not being saved in transfers storage
             TransferResult.SPECIFIC_SERVICE_UNAVAILABLE -> R.string.service_unavailable
             // We don't know the specific unsupported media type error message because it is not being saved in transfers storage
             TransferResult.SPECIFIC_UNSUPPORTED_MEDIA_TYPE -> R.string.uploads_view_unsupported_media_type
             // Should not get here; status should be not null
             null -> R.string.uploads_view_upload_status_unknown_fail
         }
    }
}

fun OCTransfer.isContentUri(context: Context): Boolean {
    return DocumentFile.isDocumentUri(context, Uri.parse(localPath))
}
