/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author Christian Schabesberger
 * Copyright (C) 2018 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.errorhandling

import android.content.res.Resources
import com.owncloud.android.R
import com.owncloud.android.extensions.parseError
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.errorhandling.TransferOperation.Download
import java.io.File
import java.net.SocketTimeoutException

/**
 * Class to choose proper error messages to show to the user depending on the results of operations,
 * always following the same policy
 */

class ErrorMessageAdapter {

    private class Formatter(val resources: Resources) {

        fun format(resId: Int): String {
            return resources.getString(resId)
        }

        fun format(resId: Int, m1: String?): String {
            return String.format(resources.getString(resId), m1)
        }

        fun format(resId: Int, m1: Int): String {
            return String.format(resources.getString(resId), resources.getString(m1))
        }

        fun format(resId: Int, m1: String, m2: String): String {
            return String.format(resources.getString(resId), m1, m2)
        }

        fun format(resId: Int, m1: String?, m2: Int): String {
            return String.format(resources.getString(resId), m1, resources.getString(m2))
        }

        fun forbidden(resId1: Int): String {
            return String.format(resources.getString(R.string.forbidden_permissions), resources.getString(resId1))
        }
    }

    companion object {

        fun getMessageFromTransfer(
            transferOperation: TransferOperation,
            throwable: Throwable?,
            resources: Resources
        ): String {
            val formatter = Formatter(resources)

            if (throwable == null) {
                return when (transferOperation) {
                    is Download -> {
                        formatter.format(
                            R.string.downloader_download_succeeded_content,
                            File(transferOperation.downloadPath).name
                        )
                    }
                }
            } else {
                val genericMessage = when (transferOperation) {
                    is Download -> formatter.format(
                        R.string.downloader_download_failed_content,
                        File(transferOperation.downloadPath).name
                    )
                }
                return throwable.parseError(genericMessage, resources, true).toString()
            }
        }

        /**
         * Return an internationalized user message corresponding to an operation result
         * and the operation performed.
         *
         * @param result                Result of a [RemoteOperation] performed.
         * @param operation             Operation performed.
         * @param resources             Reference to app resources, for i18n.
         * @return User message corresponding to 'result' and 'operation'
         */
        fun getResultMessage(
            result: RemoteOperationResult<*>,
            operation: RemoteOperation<*>?,
            resources: Resources
        ): String {
            val formatter = Formatter(resources)
            if (result.isSuccess) {
                when (operation) {
                    is UploadFileOperation -> return formatter.format(
                        R.string.uploader_upload_succeeded_content_single,
                        operation.fileName
                    )
                }
            }

            if (operation is SynchronizeFileOperation && !operation.transferWasRequested()) {
                return formatter.format(R.string.sync_file_nothing_to_do_msg)
            }

            return when (result.code) {
                ResultCode.LOCAL_STORAGE_FULL -> formatter.format(
                    R.string.error__upload__local_file_not_copied,
                    (operation as UploadFileOperation).fileName, R.string.app_name
                )
                ResultCode.LOCAL_STORAGE_NOT_COPIED -> formatter.format(
                    R.string.error__upload__local_file_not_copied,
                    (operation as UploadFileOperation).fileName, R.string.app_name
                )
                ResultCode.FORBIDDEN -> {
                    if (operation is UploadFileOperation) formatter.format(
                        R.string.forbidden_permissions,
                        R.string.uploader_upload_forbidden_permissions
                    )
                    if (operation is CreateFolderOperation) formatter.forbidden(R.string.forbidden_permissions_create)
                    else formatter.format(
                        R.string.filename_forbidden_characters_from_server
                    )
                }
                ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER ->
                    formatter.format(R.string.filename_forbidden_characters_from_server)
                ResultCode.QUOTA_EXCEEDED ->
                    formatter.format(R.string.failed_upload_quota_exceeded_text)
                ResultCode.FILE_NOT_FOUND -> {
                    if (operation is UploadFileOperation)
                        formatter.format(R.string.uploads_view_upload_status_failed_folder_error)
                    if (operation is SynchronizeFolderOperation)
                        formatter.format(
                            R.string.sync_current_folder_was_removed,
                            File(operation.folderPath).name
                        )
                    else formatter.format(R.string.rename_local_fail_msg)
                }
                ResultCode.INVALID_LOCAL_FILE_NAME ->
                    formatter.format(R.string.rename_local_fail_msg)
                ResultCode.INVALID_CHARACTER_IN_NAME ->
                    formatter.format(R.string.filename_forbidden_characters)
                ResultCode.INVALID_OVERWRITE -> {
                    formatter.format(R.string.move_file_error)
                }
                ResultCode.CONFLICT -> formatter.format(R.string.move_file_error)
                ResultCode.INVALID_COPY_INTO_DESCENDANT ->
                    formatter.format(R.string.copy_file_invalid_into_descendent)
                else -> getCommonMessageForResult(operation, result, resources)
            }
        }

        /**
         * Return a user message corresponding to an operation result with no knowledge about the operation
         * performed.
         *
         * @param result        Result of a [RemoteOperation] performed.
         * @param res           Reference to app resources, for i18n.
         * @return User message corresponding to 'result'.
         */
        private fun getCommonMessageForResult(
            operation: RemoteOperation<*>?,
            result: RemoteOperationResult<*>,
            res: Resources
        ): String {

            val formatter = Formatter(res)

            if (result.isSuccess) return ""
            return when (result.code) {
                ResultCode.WRONG_CONNECTION -> formatter.format(R.string.network_error_socket_exception)
                ResultCode.NO_NETWORK_CONNECTION -> formatter.format(R.string.error_no_network_connection)
                ResultCode.TIMEOUT ->
                    if (result.exception is SocketTimeoutException) formatter.format(R.string.network_error_socket_timeout_exception)
                    else formatter.format(R.string.network_error_connect_timeout_exception)
                ResultCode.HOST_NOT_AVAILABLE -> formatter.format(R.string.network_host_not_available)
                ResultCode.SERVICE_UNAVAILABLE -> formatter.format(R.string.service_unavailable)
                ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> formatter.format(R.string.ssl_certificate_not_trusted)
                ResultCode.BAD_OC_VERSION -> formatter.format(R.string.auth_bad_oc_version_title)
                ResultCode.INCORRECT_ADDRESS -> formatter.format(R.string.auth_incorrect_address_title)
                ResultCode.SSL_ERROR -> formatter.format(R.string.auth_ssl_general_error_title)
                ResultCode.UNAUTHORIZED -> formatter.format(R.string.auth_unauthorized)
                ResultCode.INSTANCE_NOT_CONFIGURED -> formatter.format(R.string.auth_not_configured_title)
                ResultCode.FILE_NOT_FOUND -> formatter.format(R.string.auth_incorrect_path_title)
                ResultCode.OAUTH2_ERROR -> formatter.format(R.string.auth_oauth_error)
                ResultCode.OAUTH2_ERROR_ACCESS_DENIED -> formatter.format(R.string.auth_oauth_error_access_denied)
                ResultCode.ACCOUNT_NOT_NEW -> formatter.format(R.string.auth_account_not_new)
                ResultCode.ACCOUNT_NOT_THE_SAME -> formatter.format(R.string.auth_account_not_the_same)
                ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION -> formatter.format(R.string.auth_redirect_non_secure_connection_title)
                else -> if (result.httpPhrase != null && result.httpPhrase.isNotEmpty())
                    result.httpPhrase else getGenericErrorMessageForOperation(operation, result, res)
            }
        }

        /**
         * Return a user message corresponding to a generic error for a given operation.
         *
         * @param operation     Operation performed.
         * @param res           Reference to app resources, for i18n.
         * @return User message corresponding to a generic error of 'operation'.
         */
        private fun getGenericErrorMessageForOperation(
            operation: RemoteOperation<*>?,
            result: RemoteOperationResult<*>,
            res: Resources
        ): String {
            val formatter = Formatter(res)

            return when (operation) {
                is UploadFileOperation -> formatter.format(R.string.uploader_upload_failed_content_single, operation.fileName)
                is CreateFolderOperation -> formatter.format(R.string.create_dir_fail_msg)
                is SynchronizeFolderOperation -> formatter.format(
                    R.string.sync_folder_failed_content,
                    File(operation.folderPath).name
                )
                // if everything else fails
                else -> if (result.isSuccess) formatter.format(android.R.string.ok) else formatter.format(R.string.common_error_unknown)
            }
        }
    }
}
