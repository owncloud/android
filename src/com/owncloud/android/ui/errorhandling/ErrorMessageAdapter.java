/**
 * ownCloud Android client application
 *
 * @author masensio
 * Copyright (C) 2016 ownCloud GmbH.
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
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

import org.apache.commons.httpclient.ConnectTimeoutException;

import java.io.File;
import java.net.SocketTimeoutException;
import java.text.Normalizer;

/**
 * Class to choose proper error messages to show to the user depending on the results of operations,
 * always following the same policy
 */

public class ErrorMessageAdapter {

    private static class Formater {
        final Resources r;
        Formater(Resources r) {
            this.r = r;
        }

        public String f(final int m) {
            return r.getString(m);
        }

        public String f(final int m, final String m1) {
            return String.format(r.getString(m), m1);
        }

        public String f(final int m, final int m1) {
            return String.format(r.getString(m), r.getString(m1));
        }

        public String f(final int m, final String m1, final String m2) {
            return String.format(r.getString(m), m1, m2);
        }

        public String f(final int m, final String m1, final int m2) {
            return String.format(r.getString(m), m1, r.getString(m2));
        }

        public String forbidden(final int m1) {
            return String.format(r.getString(R.string.forbidden_permissions), r.getString(m1));
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
    public static String getErrorCauseMessage(final RemoteOperationResult result,
                                           final RemoteOperation operation,
                                           final Resources resources) {

        final RemoteOperationResult rlt = result;
        final RemoteOperation o = operation;
        Formater f = new Formater(resources);
        if(rlt.isSuccess()) {
            if(o instanceof UploadFileOperation) return f.f(R.string.uploader_upload_succeeded_content_single, ((UploadFileOperation) o).getFileName());
            if(o instanceof DownloadFileOperation) return f.f(R.string.downloader_download_succeeded_content);
            if(o instanceof RemoveFileOperation) return f.f(R.string.remove_success_msg);
        }

        if(o instanceof SynchronizeFileOperation &&
                !((SynchronizeFileOperation) o).transferWasRequested()) {
            return f.f(R.string.sync_file_nothing_to_do_msg);
        }

        if((o instanceof CreateShareViaLinkOperation
                || o instanceof RemoveShareOperation
                || o instanceof UpdateShareViaLinkOperation
                || o instanceof UpdateSharePermissionsOperation)
                && rlt.getData() != null
                && rlt.getData().size() > 0) {
            return rlt.getData().get(0).toString();
        }

        switch (rlt.getCode()) {
            case LOCAL_STORAGE_FULL:
            case LOCAL_STORAGE_NOT_COPIED: return f.f(R.string.error__upload__local_file_not_copied, ((UploadFileOperation) o).getFileName(),R.string.app_name);
            case FORBIDDEN:
                if(o instanceof UploadFileOperation) f.f(R.string.forbidden_permissions, R.string.uploader_upload_forbidden_permissions);
                if(o instanceof DownloadFileOperation) return f.forbidden(R.string.downloader_download_forbidden_permissions);
                if(o instanceof RemoveFileOperation) return f.forbidden(R.string.forbidden_permissions_delete);
                if(o instanceof RenameFileOperation) return f.forbidden(R.string.forbidden_permissions_rename);
                if(o instanceof CreateFolderOperation) return f.forbidden(R.string.forbidden_permissions_create);
                if(o instanceof MoveFileOperation) return f.forbidden(R.string.forbidden_permissions_move);
                if(o instanceof CopyFileOperation) return f.forbidden(R.string.forbidden_permissions_copy);
            case INVALID_CHARACTER_DETECT_IN_SERVER: return f.f(R.string.filename_forbidden_charaters_from_server);
            case QUOTA_EXCEEDED: return f.f(R.string.failed_upload_quota_exceeded_text);
            case FILE_NOT_FOUND:
                if(o instanceof UploadFileOperation) return f.f(R.string.uploads_view_upload_status_failed_folder_error);
                if(o instanceof DownloadFileOperation) return f.f(R.string.downloader_download_forbidden_permissions);
                if(o instanceof RenameFileOperation) return f.f(R.string.rename_server_fail_msg);
                if(o instanceof MoveFileOperation) return f.f(R.string.move_file_not_found);
                if(o instanceof SynchronizeFolderOperation) return f.f(R.string.sync_current_folder_was_removed,
                        new File(((SynchronizeFolderOperation) o).getFolderPath()).getName());
                if(o instanceof CopyFileOperation) return f.f(R.string.copy_file_not_found);
            case INVALID_LOCAL_FILE_NAME: return f.f(R.string.rename_local_fail_msg);
            case INVALID_CHARACTER_IN_NAME: return f.f(R.string.filename_forbidden_characters);
            case SHARE_NOT_FOUND:
                    if(o instanceof CreateShareViaLinkOperation) return f.f(R.string.share_link_file_no_exist);
                    if(o instanceof RemoveShareOperation) return f.f(R.string.unshare_link_file_no_exist);
                    if(o instanceof UpdateSharePermissionsOperation
                            || o instanceof UpdateShareViaLinkOperation) f.f(R.string.update_link_file_no_exist);
            case SHARE_FORBIDDEN:
                if(o instanceof CreateShareViaLinkOperation) return f.forbidden(R.string.share_link_forbidden_permissions);
                if(o instanceof RemoveShareOperation) return f.forbidden(R.string.unshare_link_forbidden_permissions);
                if(o instanceof UpdateSharePermissionsOperation
                        || o instanceof UpdateShareViaLinkOperation) f.forbidden(R.string.update_link_forbidden_permissions);
            case INVALID_MOVE_INTO_DESCENDANT: f.f(R.string.move_file_invalid_into_descendent);
            case INVALID_OVERWRITE:
                if(o instanceof MoveFileOperation) return f.f(R.string.move_file_invalid_overwrite);
                if(o instanceof CopyFileOperation) return f.f(R.string.copy_file_invalid_overwrite);
            case CONFLICT: f.f(R.string.move_file_error);
            case INVALID_COPY_INTO_DESCENDANT: f.f(R.string.copy_file_invalid_into_descendent);

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
    private static String getCommonMessageForResult(RemoteOperation operation, RemoteOperationResult result, Resources res) {

        final Formater f = new Formater(res);

        if(result.isSuccess()) return "";
        switch(result.getCode()) {
            case WRONG_CONNECTION: return f.f(R.string.network_error_socket_exception);
            case NO_NETWORK_CONNECTION: return f.f(R.string.error_no_network_connection);
            case TIMEOUT: return (result.getException() instanceof ConnectTimeoutException) ?
                    f.f(R.string.network_error_connect_timeout_exception) :
                    f.f(R.string.network_error_socket_timeout_exception);
            case HOST_NOT_AVAILABLE: f.f(R.string.network_host_not_available);
            case SERVICE_UNAVAILABLE: f.f(R.string.service_unavailable);
            case SSL_RECOVERABLE_PEER_UNVERIFIED: f.f(R.string.ssl_certificate_not_trusted);
            case BAD_OC_VERSION: f.f(R.string.auth_bad_oc_version_title);
            case INCORRECT_ADDRESS: f.f(R.string.auth_incorrect_address_title);
            case SSL_ERROR: f.f(R.string.auth_ssl_general_error_title);
            case UNAUTHORIZED: f.f(R.string.auth_unauthorized);
            case INSTANCE_NOT_CONFIGURED: f.f(R.string.auth_not_configured_title);
            case FILE_NOT_FOUND: f.f(R.string.auth_incorrect_path_title);
            case OAUTH2_ERROR: f.f(R.string.auth_oauth_error);
            case OAUTH2_ERROR_ACCESS_DENIED: f.f(R.string.auth_oauth_error_access_denied);
            case ACCOUNT_NOT_NEW: f.f(R.string.auth_account_not_new);
            case ACCOUNT_NOT_THE_SAME: f.f(R.string.auth_account_not_the_same);
            case OK_REDIRECT_TO_NON_SECURE_CONNECTION: f.f(R.string.auth_redirect_non_secure_connection_title);
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
    private static String getGenericErrorMessageForOperation(RemoteOperation operation, RemoteOperationResult result, Resources res) {
        final RemoteOperation o = operation;
        final Formater f = new Formater(res);

        if (o instanceof UploadFileOperation) return f.f(R.string.uploader_upload_failed_content_single, ((UploadFileOperation) operation).getFileName());
        if (o instanceof DownloadFileOperation) return f.f(R.string.downloader_download_failed_content,
                    new File(((DownloadFileOperation) operation).getSavePath()).getName());
        if (o instanceof RemoveFileOperation) return f.f(R.string.remove_fail_msg);
        if (o instanceof RenameFileOperation) return f.f(R.string.rename_server_fail_msg);
        if (o instanceof CreateFolderOperation) return f.f(R.string.create_dir_fail_msg);
        if (o instanceof CreateShareViaLinkOperation ||
                o instanceof CreateShareWithShareeOperation)
            f.f(R.string.share_link_file_error);
        if (o instanceof RemoveShareOperation) return f.f(R.string.unshare_link_file_error);
        if (o instanceof UpdateShareViaLinkOperation ||
                operation instanceof UpdateSharePermissionsOperation)
            f.f((R.string.update_link_file_error));
        if (o instanceof MoveFileOperation) return f.f(R.string.move_file_error);
        if (o instanceof SynchronizeFolderOperation)
            return f.f(R.string.sync_folder_failed_content, new File(((SynchronizeFolderOperation) operation).getFolderPath()).getName());
        if (o instanceof CopyFileOperation) return f.f(R.string.copy_file_error);

        // if everything else failes
        if(result.isSuccess()) return f.f(R.string.common_ok);
        else return f.f(R.string.common_error_unknown);
    }
}
