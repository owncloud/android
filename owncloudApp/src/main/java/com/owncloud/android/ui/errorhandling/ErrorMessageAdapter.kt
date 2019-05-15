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
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.operations.*
import com.owncloud.android.operations.common.OperationType
import java.io.File
import java.lang.Exception
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
        ): String? {
            val f = Formatter(resources)
            if (result.isSuccess) {
                if (operation is UploadFileOperation)
                    return f.format(R.string.uploader_upload_succeeded_content_single, operation.fileName)
                if (operation is DownloadFileOperation)
                    return f.format(
                        R.string.downloader_download_succeeded_content,
                        File(operation.savePath).name
                    )
                if (operation is RemoveFileOperation)
                    return f.format(R.string.remove_success_msg)
            }

            if (operation is SynchronizeFileOperation && !operation.transferWasRequested()) {
                return f.format(R.string.sync_file_nothing_to_do_msg)
            }

            when (result.code) {
                RemoteOperationResult.ResultCode.LOCAL_STORAGE_FULL -> return f.format(
                    R.string.error__upload__local_file_not_copied,
                    (operation as UploadFileOperation).fileName, R.string.app_name
                )
                RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_COPIED -> return f.format(
                    R.string.error__upload__local_file_not_copied,
                    (operation as UploadFileOperation).fileName, R.string.app_name
                )
                RemoteOperationResult.ResultCode.FORBIDDEN -> {
                    if (operation is UploadFileOperation)
                        return f.format(R.string.forbidden_permissions, R.string.uploader_upload_forbidden_permissions)
                    if (operation is DownloadFileOperation)
                        return f.forbidden(R.string.downloader_download_forbidden_permissions)
                    if (operation is RemoveFileOperation)
                        return f.forbidden(R.string.forbidden_permissions_delete)
                    if (operation is RenameFileOperation)
                        return f.forbidden(R.string.forbidden_permissions_rename)
                    if (operation is CreateFolderOperation)
                        return f.forbidden(R.string.forbidden_permissions_create)
                    if (operation is MoveFileOperation) return f.forbidden(R.string.forbidden_permissions_move)
                    return if (operation is CopyFileOperation) f.forbidden(R.string.forbidden_permissions_copy) else f.format(
                        R.string.filename_forbidden_charaters_from_server
                    )
                }
                RemoteOperationResult.ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER ->
                    return f.format(R.string.filename_forbidden_charaters_from_server)
                RemoteOperationResult.ResultCode.QUOTA_EXCEEDED ->
                    return f.format(R.string.failed_upload_quota_exceeded_text)
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> {
                    if (operation is UploadFileOperation)
                        return f.format(R.string.uploads_view_upload_status_failed_folder_error)
                    if (operation is DownloadFileOperation)
                        return f.format(R.string.downloader_download_forbidden_permissions)
                    if (operation is RenameFileOperation) return f.format(R.string.rename_server_fail_msg)
                    if (operation is MoveFileOperation) return f.format(R.string.move_file_not_found)
                    if (operation is SynchronizeFolderOperation)
                        return f.format(
                            R.string.sync_current_folder_was_removed,
                            File(operation.folderPath).name
                        )
                    return if (operation is CopyFileOperation)
                        f.format(R.string.copy_file_not_found) else f.format(R.string.rename_local_fail_msg)
                }
                RemoteOperationResult.ResultCode.INVALID_LOCAL_FILE_NAME ->
                    return f.format(R.string.rename_local_fail_msg)
                RemoteOperationResult.ResultCode.INVALID_CHARACTER_IN_NAME ->
                    return f.format(R.string.filename_forbidden_characters)
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND -> {
                    if (operation is RemoveShareOperation)
                        return f.format(R.string.unshare_link_file_no_exist)
                    if (operation is UpdateSharePermissionsOperation)
                        return f.format(R.string.update_link_file_no_exist)
                    if (operation is RemoveShareOperation)
                        return f.forbidden(R.string.unshare_link_forbidden_permissions)
                    return if (operation is UpdateSharePermissionsOperation)
                        f.forbidden(R.string.update_link_forbidden_permissions)
                    else f.format(R.string.move_file_invalid_into_descendent)
                }
                RemoteOperationResult.ResultCode.SHARE_FORBIDDEN -> {
                    if (operation is RemoveShareOperation)
                        return f.forbidden(R.string.unshare_link_forbidden_permissions)
                    return if (
                        operation is UpdateSharePermissionsOperation
                    ) f.forbidden(
                        R.string.update_link_forbidden_permissions
                    ) else f.format(R.string.move_file_invalid_into_descendent)
                }
                RemoteOperationResult.ResultCode.INVALID_MOVE_INTO_DESCENDANT -> return f.format(R.string.move_file_invalid_into_descendent)
                RemoteOperationResult.ResultCode.INVALID_OVERWRITE -> {
                    if (operation is MoveFileOperation) return f.format(R.string.move_file_invalid_overwrite)
                    return if (operation is CopyFileOperation) f.format(R.string.copy_file_invalid_overwrite)
                    else f.format(R.string.move_file_error)
                }
                RemoteOperationResult.ResultCode.CONFLICT -> return f.format(R.string.move_file_error)
                RemoteOperationResult.ResultCode.INVALID_COPY_INTO_DESCENDANT ->
                    return f.format(R.string.copy_file_invalid_into_descendent)
                else -> return getCommonMessageForResult(operation, result, resources)
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
        ): String? {

            val f = Formatter(res)

            if (result.isSuccess) return ""
            when (result.code) {
                RemoteOperationResult.ResultCode.WRONG_CONNECTION ->
                    return f.format(R.string.network_error_socket_exception)
                RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION ->
                    return f.format(R.string.error_no_network_connection)
                RemoteOperationResult.ResultCode.TIMEOUT ->
                    return if (result.exception is SocketTimeoutException)
                        f.format(R.string.network_error_socket_timeout_exception)
                    else
                        f.format(R.string.network_error_connect_timeout_exception)
                RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE ->
                    return f.format(R.string.network_host_not_available)
                RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE ->
                    return f.format(R.string.service_unavailable)
                RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED ->
                    return f.format(R.string.ssl_certificate_not_trusted)
                RemoteOperationResult.ResultCode.BAD_OC_VERSION ->
                    return f.format(R.string.auth_bad_oc_version_title)
                RemoteOperationResult.ResultCode.INCORRECT_ADDRESS ->
                    return f.format(R.string.auth_incorrect_address_title)
                RemoteOperationResult.ResultCode.SSL_ERROR ->
                    return f.format(R.string.auth_ssl_general_error_title)
                RemoteOperationResult.ResultCode.UNAUTHORIZED ->
                    return f.format(R.string.auth_unauthorized)
                RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED ->
                    return f.format(R.string.auth_not_configured_title)
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND ->
                    return f.format(R.string.auth_incorrect_path_title)
                RemoteOperationResult.ResultCode.OAUTH2_ERROR ->
                    return f.format(R.string.auth_oauth_error)
                RemoteOperationResult.ResultCode.OAUTH2_ERROR_ACCESS_DENIED ->
                    return f.format(R.string.auth_oauth_error_access_denied)
                RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW ->
                    return f.format(R.string.auth_account_not_new)
                RemoteOperationResult.ResultCode.ACCOUNT_NOT_THE_SAME ->
                    return f.format(R.string.auth_account_not_the_same)
                RemoteOperationResult.ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION ->
                    return f.format(R.string.auth_redirect_non_secure_connection_title)
                else -> if (result.httpPhrase != null && result.httpPhrase.length > 0)
                    return result.httpPhrase
            }

            return getGenericErrorMessageForOperation(operation, result, res)
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
        ): String? {
            val f = Formatter(res)

            if (operation is UploadFileOperation)
                return f.format(
                    R.string.uploader_upload_failed_content_single,
                    operation.fileName
                )
            if (operation is DownloadFileOperation)
                return f.format(
                    R.string.downloader_download_failed_content,
                    File(operation.savePath).name
                )
            if (operation is RemoveFileOperation) return f.format(R.string.remove_fail_msg)
            if (operation is RenameFileOperation) return f.format(R.string.rename_server_fail_msg)
            if (operation is CreateFolderOperation) return f.format(R.string.create_dir_fail_msg)
            if (operation is CreateShareWithShareeOperation)
                return f.format(R.string.share_link_file_error)
            if (operation is RemoveShareOperation) return f.format(R.string.unshare_link_file_error)
            if (operation is UpdateSharePermissionsOperation)
                return f.format(R.string.update_link_file_error)
            if (operation is MoveFileOperation) return f.format(R.string.move_file_error)
            if (operation is SynchronizeFolderOperation)
                return f.format(
                    R.string.sync_folder_failed_content,
                    File(operation.folderPath).name
                )
            if (operation is CopyFileOperation) return f.format(R.string.copy_file_error)
            // if everything else failes
            return if (result.isSuccess)
                f.format(android.R.string.ok)
            else
                f.format(R.string.common_error_unknown)
        }

        /**
         * TODO Remove methods above when new architecture is completed
         */

        /**
         * Return an internationalized user message corresponding to an operation result
         * and the operation performed.
         *
         * @param resultCode            ResultCode of a [RemoteOperation] performed.
         * @param resultException       Exception of a [RemoteOperation] performed.
         * @param operationType         [OperationType] performed.
         * @param resources             Reference to app resources, for i18n.
         * @return User message corresponding to 'result' and 'operation'
         */
        fun getResultMessage(
            resultCode: RemoteOperationResult.ResultCode?,
            resultException: Exception?,
            operationType: OperationType,
            resources: Resources
        ): String {
            val f = Formatter(resources)

            when (resultCode) {
                RemoteOperationResult.ResultCode.FORBIDDEN -> {
                    return when (operationType) {
                        OperationType.UPLOAD_FILE ->
                            f.format(R.string.forbidden_permissions, R.string.uploader_upload_forbidden_permissions)
                        OperationType.DOWNLOAD_FILE -> f.forbidden(R.string.downloader_download_forbidden_permissions)
                        OperationType.REMOVE_FILE -> f.forbidden(R.string.forbidden_permissions_delete)
                        OperationType.RENAME_FILE -> f.forbidden(R.string.forbidden_permissions_rename)
                        OperationType.CREATE_FOLDER -> f.forbidden(R.string.forbidden_permissions_create)
                        OperationType.MOVE_FILE -> f.forbidden(R.string.forbidden_permissions_move)
                        OperationType.COPY_FILE -> f.forbidden(R.string.forbidden_permissions_copy)
                        else -> getCommonMessageForResult(operationType, resultCode, resultException, resources)
                    }
                }
                RemoteOperationResult.ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER ->
                    return f.format(R.string.filename_forbidden_charaters_from_server)
                RemoteOperationResult.ResultCode.QUOTA_EXCEEDED ->
                    return f.format(R.string.failed_upload_quota_exceeded_text)
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> {
                    return when (operationType) {
                        OperationType.UPLOAD_FILE -> f.format(R.string.uploads_view_upload_status_failed_folder_error)
                        OperationType.DOWNLOAD_FILE -> f.format(R.string.downloader_download_forbidden_permissions)
                        OperationType.RENAME_FILE -> f.format(R.string.rename_server_fail_msg)
                        OperationType.MOVE_FILE -> f.format(R.string.move_file_not_found)
                        OperationType.COPY_FILE -> f.format(R.string.copy_file_not_found)
                        else -> getCommonMessageForResult(operationType, resultCode, resultException, resources)
                    }
                }
                RemoteOperationResult.ResultCode.INVALID_LOCAL_FILE_NAME ->
                    return f.format(R.string.rename_local_fail_msg)
                RemoteOperationResult.ResultCode.INVALID_CHARACTER_IN_NAME ->
                    return f.format(R.string.filename_forbidden_characters)
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND -> {
                    return when (operationType) {
                        OperationType.CREATE_PUBLIC_SHARE -> f.format(R.string.share_link_file_no_exist)
                        OperationType.REMOVE_SHARE -> f.format(R.string.unshare_link_file_no_exist)
                        OperationType.UPDATE_SHARE_PERMISSIONS, OperationType.UPDATE_PUBLIC_SHARE ->
                            f.format(R.string.update_link_file_no_exist)
                        else -> getCommonMessageForResult(operationType, resultCode, resultException, resources)
                    }
                }
                RemoteOperationResult.ResultCode.SHARE_FORBIDDEN -> {
                    if (operationType == OperationType.CREATE_PUBLIC_SHARE)
                        return f.forbidden(R.string.share_link_forbidden_permissions)
                    if (operationType == OperationType.REMOVE_SHARE)
                        return f.forbidden(R.string.unshare_link_forbidden_permissions)
                    return if (
                        operationType == OperationType.UPDATE_SHARE_PERMISSIONS ||
                        operationType == OperationType.UPDATE_PUBLIC_SHARE
                    ) f.forbidden(
                        R.string.update_link_forbidden_permissions
                    ) else f.format(R.string.move_file_invalid_into_descendent)
                }
                RemoteOperationResult.ResultCode.INVALID_MOVE_INTO_DESCENDANT ->
                    return f.format(R.string.move_file_invalid_into_descendent)
                RemoteOperationResult.ResultCode.INVALID_OVERWRITE -> {
                    return when (operationType) {
                        OperationType.MOVE_FILE -> f.format(R.string.move_file_invalid_overwrite)
                        OperationType.COPY_FILE -> f.format(R.string.copy_file_invalid_overwrite)
                        else -> f.format(R.string.move_file_error)
                    }
                }
                RemoteOperationResult.ResultCode.CONFLICT -> return f.format(R.string.move_file_error)
                RemoteOperationResult.ResultCode.INVALID_COPY_INTO_DESCENDANT ->
                    return f.format(R.string.copy_file_invalid_into_descendent)
                else -> return getCommonMessageForResult(operationType, resultCode, resultException, resources)
            }
        }

        /**
         * Return a user message corresponding to an operation result with no knowledge about the operation
         * performed.
         *
         * @param operationType [OperationType] performed.
         * @param resultCode    ResultCode of a [RemoteOperation] performed.
         * @param resultException    Exception of a [RemoteOperation] performed.
         * @param res           Reference to app resources, for i18n.
         * @return User message corresponding to 'result'.
         */
        private fun getCommonMessageForResult(
            operation: OperationType,
            resultCode: RemoteOperationResult.ResultCode?,
            resultException: Exception?,
            res: Resources
        ): String {

            val f = Formatter(res)

            if (resultCode == RemoteOperationResult.ResultCode.OK) return ""
            when (resultCode) {
                RemoteOperationResult.ResultCode.WRONG_CONNECTION ->
                    return f.format(R.string.network_error_socket_exception)
                RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION ->
                    return f.format(R.string.error_no_network_connection)
                RemoteOperationResult.ResultCode.TIMEOUT ->
                    return if (resultException is SocketTimeoutException)
                        f.format(R.string.network_error_socket_timeout_exception)
                    else
                        f.format(R.string.network_error_connect_timeout_exception)
                RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE ->
                    return f.format(R.string.network_host_not_available)
                RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE ->
                    return f.format(R.string.service_unavailable)
                RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED ->
                    return f.format(R.string.ssl_certificate_not_trusted)
                RemoteOperationResult.ResultCode.BAD_OC_VERSION ->
                    return f.format(R.string.auth_bad_oc_version_title)
                RemoteOperationResult.ResultCode.INCORRECT_ADDRESS ->
                    return f.format(R.string.auth_incorrect_address_title)
                RemoteOperationResult.ResultCode.SSL_ERROR ->
                    return f.format(R.string.auth_ssl_general_error_title)
                RemoteOperationResult.ResultCode.UNAUTHORIZED ->
                    return f.format(R.string.auth_unauthorized)
                RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED ->
                    return f.format(R.string.auth_not_configured_title)
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND ->
                    return f.format(R.string.auth_incorrect_path_title)
                RemoteOperationResult.ResultCode.OAUTH2_ERROR ->
                    return f.format(R.string.auth_oauth_error)
                RemoteOperationResult.ResultCode.OAUTH2_ERROR_ACCESS_DENIED ->
                    return f.format(R.string.auth_oauth_error_access_denied)
                RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW ->
                    return f.format(R.string.auth_account_not_new)
                RemoteOperationResult.ResultCode.ACCOUNT_NOT_THE_SAME ->
                    return f.format(R.string.auth_account_not_the_same)
                RemoteOperationResult.ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION ->
                    return f.format(R.string.auth_redirect_non_secure_connection_title)
            }

            return getGenericErrorMessageForOperation(operation, resultCode, res)
        }

        /**
         * Return a user message corresponding to a generic error for a given operation.
         *
         * @param operationType [OperationType] performed.
         * @param resultCode    ResultCode of a [RemoteOperation] performed.
         * @return User message corresponding to a generic error of 'operation'.
         */
        private fun getGenericErrorMessageForOperation(
            operationType: OperationType,
            resultCode: RemoteOperationResult.ResultCode?,
            res: Resources
        ): String {
            val f = Formatter(res)
            return when (operationType) {
                OperationType.REMOVE_FILE -> f.format(R.string.remove_fail_msg)
                OperationType.RENAME_FILE -> f.format(R.string.rename_server_fail_msg)
                OperationType.CREATE_FOLDER -> f.format(R.string.create_dir_fail_msg)
                OperationType.CREATE_PUBLIC_SHARE, OperationType.CREATE_SHARE_WITH_SHAREES ->
                    f.format(R.string.share_link_file_error)
                OperationType.REMOVE_SHARE -> f.format(R.string.unshare_link_file_error)
                OperationType.UPDATE_PUBLIC_SHARE, OperationType.UPDATE_SHARE_PERMISSIONS ->
                    f.format(R.string.update_link_file_error)
                OperationType.MOVE_FILE -> f.format(R.string.move_file_error)
                OperationType.COPY_FILE -> f.format(R.string.copy_file_error)
                else -> {
                    if (resultCode == RemoteOperationResult.ResultCode.OK) {
                        f.format(android.R.string.ok)
                    } else {
                        f.format(R.string.common_error_unknown)
                    }
                }
            }
        }
    }
}