/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author Christian Schabesberger
 * Copyright (C) 2018 ownCloud GmbH.
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


package com.owncloud.android.ui.errorhandling;

import android.content.res.Resources;
import android.support.annotation.Nullable;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RemoveShareOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.operations.UploadFileOperation;



import java.io.File;
import java.net.SocketTimeoutException;

/**
 * Class to choose proper error messages to show to the user depending on the results of operations,
 * always following the same policy
 */

public class ErrorMessageAdapter {

    private static class Formatter {
        final Resources r;
        Formatter(Resources r) {
            this.r = r;
        }

        public String format(final int resId) {
            return r.getString(resId);
        }

        public String format(final int resId, final String m1) {
            return String.format(r.getString(resId), m1);
        }

        public String format(final int resId, final int m1) {
            return String.format(r.getString(resId), r.getString(m1));
        }

        public String format(final int resId, final String m1, final String m2) {
            return String.format(r.getString(resId), m1, m2);
        }

        public String format(final int resId, final String m1, final int m2) {
            return String.format(r.getString(resId), m1, r.getString(m2));
        }

        public String forbidden(final int resId1) {
            return String.format(r.getString(R.string.forbidden_permissions), r.getString(resId1));
        }

    }

    public ErrorMessageAdapter() {
    }

    /**
     * Return an internationalized user message corresponding to an operation result
     * and the operation performed.
     *
     * @param result                Result of a {@link RemoteOperation} performed.
     * @param operation             Operation performed.
     * @param resources             Reference to app resources, for i18n.
     * @return User message corresponding to 'result' and 'operation'
     */
    public static String getResultMessage(final RemoteOperationResult result,
                                          final RemoteOperation operation,
                                          final Resources resources) {
        Formatter f = new Formatter(resources);
        if(result.isSuccess()) {
            if(operation instanceof UploadFileOperation)
                return f.format(R.string.uploader_upload_succeeded_content_single, ((UploadFileOperation) operation).getFileName());
            if(operation instanceof DownloadFileOperation)
                return f.format(R.string.downloader_download_succeeded_content,
                        new File(((DownloadFileOperation) operation).getSavePath()).getName());
            if(operation instanceof RemoveFileOperation)
                return f.format(R.string.remove_success_msg);
        }

        if(operation instanceof SynchronizeFileOperation &&
                !((SynchronizeFileOperation) operation).transferWasRequested()) {
            return f.format(R.string.sync_file_nothing_to_do_msg);
        }

        if((operation instanceof CreateShareWithShareeOperation
                || operation instanceof CreateShareViaLinkOperation
                || operation instanceof RemoveShareOperation
                || operation instanceof UpdateShareViaLinkOperation
                || operation instanceof UpdateSharePermissionsOperation)) {

            RemoteOperationResult<ShareParserResult> shareResult = (RemoteOperationResult<ShareParserResult>) result;

            return (shareResult.getData()!= null
                    && shareResult.getData().getShares() != null
                    && shareResult.getData().getShares().size() > 0)
                    ? shareResult.getData().getShares().get(0).toString()
                    : shareResult.getData().getParserMessage();
        }

        switch (result.getCode()) {
            case LOCAL_STORAGE_FULL:
                return f.format(R.string.error__upload__local_file_not_copied,
                        ((UploadFileOperation) operation).getFileName(),R.string.app_name);
            case LOCAL_STORAGE_NOT_COPIED:
                return f.format(R.string.error__upload__local_file_not_copied,
                        ((UploadFileOperation) operation).getFileName(),R.string.app_name);
            case FORBIDDEN:
                if(operation instanceof UploadFileOperation)
                    return f.format(R.string.forbidden_permissions, R.string.uploader_upload_forbidden_permissions);
                if(operation instanceof DownloadFileOperation)
                    return f.forbidden(R.string.downloader_download_forbidden_permissions);
                if(operation instanceof RemoveFileOperation)
                    return f.forbidden(R.string.forbidden_permissions_delete);
                if(operation instanceof RenameFileOperation)
                    return f.forbidden(R.string.forbidden_permissions_rename);
                if(operation instanceof CreateFolderOperation)
                    return f.forbidden(R.string.forbidden_permissions_create);
                if(operation instanceof MoveFileOperation) return f.forbidden(R.string.forbidden_permissions_move);
                if(operation instanceof CopyFileOperation)return f.forbidden(R.string.forbidden_permissions_copy);
            case INVALID_CHARACTER_DETECT_IN_SERVER:
                return f.format(R.string.filename_forbidden_charaters_from_server);
            case QUOTA_EXCEEDED: return f.format(R.string.failed_upload_quota_exceeded_text);
            case FILE_NOT_FOUND:
                if(operation instanceof UploadFileOperation)
                    return f.format(R.string.uploads_view_upload_status_failed_folder_error);
                if(operation instanceof DownloadFileOperation)
                    return f.format(R.string.downloader_download_forbidden_permissions);
                if(operation instanceof RenameFileOperation) return f.format(R.string.rename_server_fail_msg);
                if(operation instanceof MoveFileOperation) return f.format(R.string.move_file_not_found);
                if(operation instanceof SynchronizeFolderOperation)
                    return f.format(R.string.sync_current_folder_was_removed,
                        new File(((SynchronizeFolderOperation) operation).getFolderPath()).getName());
                if(operation instanceof CopyFileOperation) return f.format(R.string.copy_file_not_found);
            case INVALID_LOCAL_FILE_NAME: return f.format(R.string.rename_local_fail_msg);
            case INVALID_CHARACTER_IN_NAME: return f.format(R.string.filename_forbidden_characters);
            case SHARE_NOT_FOUND:
                    if(operation instanceof CreateShareViaLinkOperation)
                        return f.format(R.string.share_link_file_no_exist);
                    if(operation instanceof RemoveShareOperation)
                        return f.format(R.string.unshare_link_file_no_exist);
                    if(operation instanceof UpdateSharePermissionsOperation
                            || operation instanceof UpdateShareViaLinkOperation)
                        return f.format(R.string.update_link_file_no_exist);
            case SHARE_FORBIDDEN:
                if(operation instanceof CreateShareViaLinkOperation)
                    return f.forbidden(R.string.share_link_forbidden_permissions);
                if(operation instanceof RemoveShareOperation)
                    return f.forbidden(R.string.unshare_link_forbidden_permissions);
                if(operation instanceof UpdateSharePermissionsOperation
                        || operation instanceof UpdateShareViaLinkOperation)
                    return f.forbidden(R.string.update_link_forbidden_permissions);
            case INVALID_MOVE_INTO_DESCENDANT:
                    return f.format(R.string.move_file_invalid_into_descendent);
            case INVALID_OVERWRITE:
                if(operation instanceof MoveFileOperation) return f.format(R.string.move_file_invalid_overwrite);
                if(operation instanceof CopyFileOperation) return f.format(R.string.copy_file_invalid_overwrite);
            case CONFLICT:return f.format(R.string.move_file_error);
            case INVALID_COPY_INTO_DESCENDANT: return f.format(R.string.copy_file_invalid_into_descendent);
            default: return getCommonMessageForResult(operation, result, resources);
        }
    }

    /**
     * Return a user message corresponding to an operation result with no knowledge about the operation
     * performed.
     *
     * @param result        Result of a {@link RemoteOperation} performed.
     * @param res           Reference to app resources, for i18n.
     * @return User message corresponding to 'result'.
     */
    @Nullable
    private static String getCommonMessageForResult(RemoteOperation operation,
                                                    RemoteOperationResult result,
                                                    Resources res) {

        final Formatter f = new Formatter(res);

        if(result.isSuccess()) return "";
        switch(result.getCode()) {
            case WRONG_CONNECTION: return f.format(R.string.network_error_socket_exception);
            case NO_NETWORK_CONNECTION: return f.format(R.string.error_no_network_connection);
            case TIMEOUT: return (result.getException() instanceof SocketTimeoutException)
                    ? f.format(R.string.network_error_socket_timeout_exception)
                    : f.format(R.string.network_error_connect_timeout_exception);
            case HOST_NOT_AVAILABLE: return f.format(R.string.network_host_not_available);
            case SERVICE_UNAVAILABLE: return f.format(R.string.service_unavailable);
            case SSL_RECOVERABLE_PEER_UNVERIFIED: return f.format(R.string.ssl_certificate_not_trusted);
            case BAD_OC_VERSION: return f.format(R.string.auth_bad_oc_version_title);
            case INCORRECT_ADDRESS: return f.format(R.string.auth_incorrect_address_title);
            case SSL_ERROR: return f.format(R.string.auth_ssl_general_error_title);
            case UNAUTHORIZED: return f.format(R.string.auth_unauthorized);
            case INSTANCE_NOT_CONFIGURED: return f.format(R.string.auth_not_configured_title);
            case FILE_NOT_FOUND: return f.format(R.string.auth_incorrect_path_title);
            case OAUTH2_ERROR: return f.format(R.string.auth_oauth_error);
            case OAUTH2_ERROR_ACCESS_DENIED: return f.format(R.string.auth_oauth_error_access_denied);
            case ACCOUNT_NOT_NEW: return f.format(R.string.auth_account_not_new);
            case ACCOUNT_NOT_THE_SAME: return f.format(R.string.auth_account_not_the_same);
            case OK_REDIRECT_TO_NON_SECURE_CONNECTION:
                return f.format(R.string.auth_redirect_non_secure_connection_title);
            default:
                if(result.getHttpPhrase() != null
                        && result.getHttpPhrase().length() > 0)
                    return result.getHttpPhrase();
        }

        return getGenericErrorMessageForOperation(operation, result, res);
    }

    /**
     * Return a user message corresponding to a generic error for a given operation.
     *
     * @param operation     Operation performed.
     * @param res           Reference to app resources, for i18n.
     * @return User message corresponding to a generic error of 'operation'.
     */
    @Nullable
    private static String getGenericErrorMessageForOperation(RemoteOperation operation,
                                                             RemoteOperationResult result,
                                                             Resources res) {
        final Formatter f = new Formatter(res);

        if (operation instanceof UploadFileOperation)
            return f.format(R.string.uploader_upload_failed_content_single,
                    ((UploadFileOperation) operation).getFileName());
        if (operation instanceof DownloadFileOperation) return f.format(R.string.downloader_download_failed_content,
                    new File(((DownloadFileOperation) operation).getSavePath()).getName());
        if (operation instanceof RemoveFileOperation) return f.format(R.string.remove_fail_msg);
        if (operation instanceof RenameFileOperation) return f.format(R.string.rename_server_fail_msg);
        if (operation instanceof CreateFolderOperation) return f.format(R.string.create_dir_fail_msg);
        if (operation instanceof CreateShareViaLinkOperation ||
                operation instanceof CreateShareWithShareeOperation)
            return f.format(R.string.share_link_file_error);
        if (operation instanceof RemoveShareOperation) return f.format(R.string.unshare_link_file_error);
        if (operation instanceof UpdateShareViaLinkOperation ||
                operation instanceof UpdateSharePermissionsOperation)
            return f.format((R.string.update_link_file_error));
        if (operation instanceof MoveFileOperation) return f.format(R.string.move_file_error);
        if (operation instanceof SynchronizeFolderOperation)
            return f.format(R.string.sync_folder_failed_content,
                    new File(((SynchronizeFolderOperation) operation).getFolderPath()).getName());
        if (operation instanceof CopyFileOperation) return f.format(R.string.copy_file_error);

        // if everything else failes
        if(result.isSuccess()) return f.format(R.string.common_ok);
        else return f.format(R.string.common_error_unknown);
    }
}
