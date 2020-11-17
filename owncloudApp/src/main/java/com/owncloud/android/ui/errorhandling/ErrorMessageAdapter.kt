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
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.errorhandling.TypeOfOperation.TransferDownload
import java.io.File
import java.net.SocketTimeoutException

/**
 * Class to choose proper error messages to show to the user depending on the results of operations,
 * always following the same policy
 */

class ErrorMessageAdapter {

    private class Formatter internal constructor(internal val r: Resources) {

        fun format(resId: Int): String {
            return r.getString(resId)
        }

        fun format(resId: Int, m1: String?): String {
            return String.format(r.getString(resId), m1)
        }

        fun format(resId: Int, m1: Int): String {
            return String.format(r.getString(resId), r.getString(m1))
        }

        fun format(resId: Int, m1: String, m2: String): String {
            return String.format(r.getString(resId), m1, m2)
        }

        fun format(resId: Int, m1: String?, m2: Int): String {
            return String.format(r.getString(resId), m1, r.getString(m2))
        }

        fun forbidden(resId1: Int): String {
            return String.format(r.getString(R.string.forbidden_permissions), r.getString(resId1))
        }
    }

    companion object {

        fun getMessageFromOperation(
            typeOfOperation: TypeOfOperation,
            throwable: Throwable?,
            resources: Resources
        ): String {
            val f = Formatter(resources)

            if (throwable == null) {
                return when (typeOfOperation) {
                    is TransferDownload -> {
                        f.format(
                            R.string.downloader_download_succeeded_content,
                            File(typeOfOperation.downloadPath).name
                        )
                    }
                }
            } else {
                val genericMessage = when (typeOfOperation) {
                    is TransferDownload -> f.format(
                        R.string.downloader_download_failed_content,
                        File(typeOfOperation.downloadPath).name
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
            val f = Formatter(resources)
            if (result.isSuccess) {
                when (operation) {
                    is UploadFileOperation -> return f.format(
                        R.string.uploader_upload_succeeded_content_single,
                        operation.fileName
                    )
                    is DownloadFileOperation -> return f.format(
                        R.string.downloader_download_succeeded_content,
                        File(operation.savePath).name
                    )
                    is RemoveFileOperation -> return f.format(R.string.remove_success_msg)
                }
            }

            if (operation is SynchronizeFileOperation && !operation.transferWasRequested()) {
                return f.format(R.string.sync_file_nothing_to_do_msg)
            }

            return when (result.code) {
                ResultCode.LOCAL_STORAGE_FULL -> f.format(
                    R.string.error__upload__local_file_not_copied,
                    (operation as UploadFileOperation).fileName, R.string.app_name
                )
                ResultCode.LOCAL_STORAGE_NOT_COPIED -> f.format(
                    R.string.error__upload__local_file_not_copied,
                    (operation as UploadFileOperation).fileName, R.string.app_name
                )
                ResultCode.FORBIDDEN -> {
                    if (operation is UploadFileOperation) f.format(
                        R.string.forbidden_permissions,
                        R.string.uploader_upload_forbidden_permissions
                    )
                    if (operation is DownloadFileOperation) f.forbidden(R.string.downloader_download_forbidden_permissions)
                    if (operation is RemoveFileOperation) f.forbidden(R.string.forbidden_permissions_delete)
                    if (operation is RenameFileOperation) f.forbidden(R.string.forbidden_permissions_rename)
                    if (operation is CreateFolderOperation) f.forbidden(R.string.forbidden_permissions_create)
                    if (operation is MoveFileOperation) f.forbidden(R.string.forbidden_permissions_move)
                    if (operation is CopyFileOperation) f.forbidden(R.string.forbidden_permissions_copy) else f.format(
                        R.string.filename_forbidden_charaters_from_server
                    )
                }
                ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER ->
                    f.format(R.string.filename_forbidden_charaters_from_server)
                ResultCode.QUOTA_EXCEEDED ->
                    f.format(R.string.failed_upload_quota_exceeded_text)
                ResultCode.FILE_NOT_FOUND -> {
                    if (operation is UploadFileOperation)
                        f.format(R.string.uploads_view_upload_status_failed_folder_error)
                    if (operation is DownloadFileOperation)
                        f.format(R.string.downloader_download_forbidden_permissions)
                    if (operation is RenameFileOperation) f.format(R.string.rename_server_fail_msg)
                    if (operation is MoveFileOperation) f.format(R.string.move_file_not_found)
                    if (operation is SynchronizeFolderOperation)
                        f.format(
                            R.string.sync_current_folder_was_removed,
                            File(operation.folderPath).name
                        )
                    if (operation is CopyFileOperation)
                        f.format(R.string.copy_file_not_found) else f.format(R.string.rename_local_fail_msg)
                }
                ResultCode.INVALID_LOCAL_FILE_NAME ->
                    f.format(R.string.rename_local_fail_msg)
                ResultCode.INVALID_CHARACTER_IN_NAME ->
                    f.format(R.string.filename_forbidden_characters)
                ResultCode.INVALID_MOVE_INTO_DESCENDANT -> f.format(R.string.move_file_invalid_into_descendent)
                ResultCode.INVALID_OVERWRITE -> {
                    if (operation is MoveFileOperation) f.format(R.string.move_file_invalid_overwrite)
                    if (operation is CopyFileOperation) f.format(R.string.copy_file_invalid_overwrite)
                    else f.format(R.string.move_file_error)
                }
                ResultCode.CONFLICT -> f.format(R.string.move_file_error)
                ResultCode.INVALID_COPY_INTO_DESCENDANT ->
                    f.format(R.string.copy_file_invalid_into_descendent)
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

            val f = Formatter(res)

            if (result.isSuccess) return ""
            return when (result.code) {
                ResultCode.WRONG_CONNECTION -> f.format(R.string.network_error_socket_exception)
                ResultCode.NO_NETWORK_CONNECTION -> f.format(R.string.error_no_network_connection)
                ResultCode.TIMEOUT ->
                    if (result.exception is SocketTimeoutException) f.format(R.string.network_error_socket_timeout_exception)
                    else f.format(R.string.network_error_connect_timeout_exception)
                ResultCode.HOST_NOT_AVAILABLE -> f.format(R.string.network_host_not_available)
                ResultCode.SERVICE_UNAVAILABLE -> f.format(R.string.service_unavailable)
                ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> f.format(R.string.ssl_certificate_not_trusted)
                ResultCode.BAD_OC_VERSION -> f.format(R.string.auth_bad_oc_version_title)
                ResultCode.INCORRECT_ADDRESS -> f.format(R.string.auth_incorrect_address_title)
                ResultCode.SSL_ERROR -> f.format(R.string.auth_ssl_general_error_title)
                ResultCode.UNAUTHORIZED -> f.format(R.string.auth_unauthorized)
                ResultCode.INSTANCE_NOT_CONFIGURED -> f.format(R.string.auth_not_configured_title)
                ResultCode.FILE_NOT_FOUND -> f.format(R.string.auth_incorrect_path_title)
                ResultCode.OAUTH2_ERROR -> f.format(R.string.auth_oauth_error)
                ResultCode.OAUTH2_ERROR_ACCESS_DENIED -> f.format(R.string.auth_oauth_error_access_denied)
                ResultCode.ACCOUNT_NOT_NEW -> f.format(R.string.auth_account_not_new)
                ResultCode.ACCOUNT_NOT_THE_SAME -> f.format(R.string.auth_account_not_the_same)
                ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION -> f.format(R.string.auth_redirect_non_secure_connection_title)
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
            val f = Formatter(res)

            return when (operation) {
                is UploadFileOperation -> f.format(R.string.uploader_upload_failed_content_single, operation.fileName)
                is DownloadFileOperation -> f.format(
                    R.string.downloader_download_failed_content,
                    File(operation.savePath).name
                )
                is RemoveFileOperation -> f.format(R.string.remove_fail_msg)
                is RenameFileOperation -> f.format(R.string.rename_server_fail_msg)
                is CreateFolderOperation -> f.format(R.string.create_dir_fail_msg)
                is MoveFileOperation -> f.format(R.string.move_file_error)
                is SynchronizeFolderOperation -> f.format(
                    R.string.sync_folder_failed_content,
                    File(operation.folderPath).name
                )
                is CopyFileOperation -> f.format(R.string.copy_file_error)
                // if everything else fails
                else -> if (result.isSuccess) f.format(android.R.string.ok) else f.format(R.string.common_error_unknown)
            }
        }
    }
}
