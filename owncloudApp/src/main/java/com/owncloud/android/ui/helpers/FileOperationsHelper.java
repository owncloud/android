/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.ui.helpers;

import android.accounts.Account;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.webkit.MimeTypeMap;

import androidx.fragment.app.DialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.AvailableOfflineHandler;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.RemoteShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.shares.db.OCShare;
import com.owncloud.android.shares.ui.ShareActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ShareLinkToDialog;

import java.util.Collection;
import java.util.List;

public class FileOperationsHelper {

    private static final String TAG = FileOperationsHelper.class.getSimpleName();

    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG";

    private FileActivity mFileActivity = null;

    /// Identifier of operation in progress which result shouldn't be lost 
    private long mWaitingForOpId = Long.MAX_VALUE;

    public FileOperationsHelper(FileActivity fileActivity) {
        mFileActivity = fileActivity;
    }

    public void openFile(OCFile file) {
        if (file != null) {
            String storagePath = file.getStoragePath();

            Intent intentForSavedMimeType = new Intent(Intent.ACTION_VIEW);
            intentForSavedMimeType.setDataAndType(
                    file.getExposedFileUri(mFileActivity),
                    file.getMimetype()
            );

            intentForSavedMimeType.setFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            Intent intentForGuessedMimeType = null;
            if (storagePath.lastIndexOf('.') >= 0) {
                String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        storagePath.substring(storagePath.lastIndexOf('.') + 1)
                );
                if (guessedMimeType != null && !guessedMimeType.equals(file.getMimetype())) {
                    intentForGuessedMimeType = new Intent(Intent.ACTION_VIEW);
                    intentForGuessedMimeType.setDataAndType(
                            file.getExposedFileUri(mFileActivity),
                            guessedMimeType
                    );
                    intentForGuessedMimeType.setFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                }
            }

            Intent openFileWithIntent;
            if (intentForGuessedMimeType != null) {
                openFileWithIntent = intentForGuessedMimeType;
            } else {
                openFileWithIntent = intentForSavedMimeType;
            }

            List<ResolveInfo> launchables = mFileActivity.getPackageManager().
                    queryIntentActivities(openFileWithIntent, PackageManager.GET_INTENT_FILTERS);

            if (launchables != null && launchables.size() > 0) {
                try {
                    mFileActivity.startActivity(
                            Intent.createChooser(
                                    openFileWithIntent, mFileActivity.getString(R.string.actionbar_open_with)
                            )
                    );
                } catch (ActivityNotFoundException anfe) {
                    mFileActivity.showSnackMessage(
                            mFileActivity.getString(
                                    R.string.file_list_no_app_for_file_type
                            )
                    );
                }
            } else {
                mFileActivity.showSnackMessage(
                        mFileActivity.getString(R.string.file_list_no_app_for_file_type)
                );
            }

        } else {
            Log_OC.e(TAG, "Trying to open a NULL OCFile");
        }
    }

    /**
     * Show dialog to allow the user to choose an app to send the private link of an {@link OCFile},
     * or copy it to clipboard.
     *
     * @param file    @param file {@link OCFile} which will be shared with internal users
     */
    public void copyOrSendPrivateLink(OCFile file) {

        // Parse remoteId
        String privateLink = file.getPrivateLink();

        if (privateLink == null || privateLink.isEmpty()) {
            mFileActivity.showSnackMessage(
                    mFileActivity.getString(R.string.file_private_link_error)
            );
            return;
        }

        shareLink(privateLink);
    }

    /**
     * Show dialog to allow the user to choose an app to send the link of an {@link RemoteShare},
     * or copy it to clipboard.
     *
     * @param share     {@link OCShare} which link will be sent to the app chosen by the user.
     */
    public void copyOrSendPublicLink(OCShare share) {
        String link = share.getShareLink();
        if (link.length() <= 0) {
            mFileActivity.showSnackMessage(
                    mFileActivity.getString(R.string.share_no_link_in_this_share)
            );
            return;
        }

        shareLink(link);
    }

    /**
     * Helper method to share a file with a known sharee. Starts a request to do it in {@link OperationsService}
     *
     * @param file          The file to share.
     * @param shareeName    Name (user name or group name) of the target sharee.
     * @param shareType     The share type determines the sharee type.
     * @param permissions   Permissions to grant to sharee on the shared file.
     */
    public void shareFileWithSharee(OCFile file, String shareeName, ShareType shareType, int permissions) {
        if (file != null) {
            // TODO check capability?
            mFileActivity.showLoadingDialog(R.string.wait_a_moment);

            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_CREATE_SHARE_WITH_SHAREE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_SHARE_WITH, shareeName);
            service.putExtra(OperationsService.EXTRA_SHARE_TYPE, shareType);
            service.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, permissions);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

        } else {
            Log_OC.e(TAG, "Trying to share a NULL OCFile");
        }
    }

    /**
     * @return 'True' if the server supports the Share API
     */
    public boolean isSharedSupported() {
        if (mFileActivity.getAccount() != null) {
            OwnCloudVersion serverVersion = AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isSharedSupported());
        }
        return false;
    }

    /**
     * Helper method to remove an existing share, no matter if public or private.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share      The {@link OCShare} to remove (unshare).
     */
    public void removeShare(OCShare share) {

        Intent unshareService = new Intent(mFileActivity, OperationsService.class);
        unshareService.setAction(OperationsService.ACTION_UNSHARE);
        unshareService.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        unshareService.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());

        queueShareIntent(unshareService);
    }

    private void queueShareIntent(Intent shareIntent) {
        if (isSharedSupported()) {
            // Unshare the file
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().
                    queueNewOperation(shareIntent);

            mFileActivity.showLoadingDialog(R.string.wait_a_moment);

        } else {
            // Show a Message
            mFileActivity.showSnackMessage(
                    mFileActivity.getString(R.string.share_link_no_support_share_api)
            );
        }
    }

    /**
     * Show an instance of {@link ShareType} for sharing or unsharing the {@link OCFile} received as parameter.
     *
     * @param file  File to share or unshare.
     */
    public void showShareFile(OCFile file) {
        Intent intent = new Intent(mFileActivity, ShareActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, file);
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, mFileActivity.getAccount());
        mFileActivity.startActivity(intent);

    }

    /**
     * Updates a share on a file to set its access permissions.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share                     {@link OCShare} instance which permissions will be updated.
     * @param permissions               New permissions to set. A value <= 0 makes no update.
     */
    public void setPermissionsToShareWithSharee(OCShare share, int permissions) {
        Intent updateShareIntent = new Intent(mFileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE_WITH_SHAREE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_PERMISSIONS,
                permissions
        );
        queueShareIntent(updateShareIntent);
    }

    /**
     * Updates at once all the properties of a public share on a file.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share                     Public share to updated.
     * @param name                      Name to set for the link (ignored in servers < 10.0.0).
     * @param password                  Password to set for the public link; null or empty string to clear
     *                                  the current password. - TODO select value to leave unchanged?
     * @param expirationTimeInMillis    Expiration date to set. A negative value clears the current expiration
     *                                  date, leaving the link unrestricted. Zero makes no change.
     * @param uploadToFolderPermission  New state of the permission for editing the folder shared via link.
     *                                  Ignored if the file is not a folder. - TODO select value to leave unchanged?
     * @param permissions               Optional permissions to allow or not specific actions in the folder
     */
    public void updateShareViaLink(
        RemoteShare share,
        String name,
        String password,
        long expirationTimeInMillis,
        boolean uploadToFolderPermission,
        int permissions
    ) {
        // Set password updating share
        Intent updateShareIntent = new Intent(mFileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE_VIA_LINK);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
//        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());

        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_NAME,
                (name == null) ? "" : name
        );

        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_PASSWORD,
                password
        );

        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS,
                expirationTimeInMillis
        );

        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_PUBLIC_UPLOAD,
                uploadToFolderPermission
        );

        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_PERMISSIONS,
                permissions
        );

        queueShareIntent(updateShareIntent);
    }

    /**
     * @return 'True' if the server supports the Search Users API
     */
    public boolean isSearchUserSupported() {
        if (mFileActivity.getAccount() != null) {
            OwnCloudVersion serverVersion = AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isSearchUsersSupported());
        }
        return false;
    }

    public void sendDownloadedFile(OCFile file) {
        if (file != null) {
            Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
            // set MimeType
            sendIntent.setType(file.getMimetype());
            sendIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    file.getExposedFileUri(mFileActivity)
            );
            sendIntent.putExtra(Intent.ACTION_SEND, true);      // Send Action

            // Show dialog, without the own app
            String[] packagesToExclude = new String[]{mFileActivity.getPackageName()};
            DialogFragment chooserDialog = ShareLinkToDialog.newInstance(sendIntent, packagesToExclude);
            chooserDialog.show(mFileActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);

        } else {
            Log_OC.e(TAG, "Trying to send a NULL OCFile");
        }
    }

    public void syncFiles(Collection<OCFile> files) {
        for (OCFile file : files) {
            syncFile(file);
        }
    }

    /**
     * Request the synchronization of a file or folder with the OC server, including its contents.
     *
     * @param file          The file or folder to synchronize
     */
    public void syncFile(OCFile file) {
        if (!file.isFolder()) {
            Intent intent = new Intent(mFileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FILE);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(intent);

        } else {
            Intent intent = new Intent(mFileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            intent.putExtra(
                    OperationsService.EXTRA_SYNC_REGULAR_FILES,
                    true
            );
            mFileActivity.startService(intent);
        }
    }

    public void toggleAvailableOffline(Collection<OCFile> files, boolean isAvailableOffline) {
        for (OCFile file : files) {
            toggleAvailableOffline(file, isAvailableOffline);
        }
    }

    public void toggleAvailableOffline(OCFile file, boolean isAvailableOffline) {
        if (OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT == file.getAvailableOfflineStatus()) {
            /// files descending of an av-offline folder can't be toggled
            mFileActivity.showSnackMessage(
                    mFileActivity.getString(R.string.available_offline_inherited_msg)
            );

        } else {
            /// update local property, for file and all its descendents (if folder)
            OCFile.AvailableOfflineStatus targetAvailableOfflineStatus = isAvailableOffline ?
                    OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE :
                    OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE;
            file.setAvailableOfflineStatus(targetAvailableOfflineStatus);
            boolean success = mFileActivity.getStorageManager().saveLocalAvailableOfflineStatus(file);

            if (success) {
                // Schedule job to check to watch for local changes in available offline files and sync them
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    AvailableOfflineHandler availableOfflineHandler =
                            new AvailableOfflineHandler(mFileActivity, mFileActivity.getAccount().name);
                    availableOfflineHandler.scheduleAvailableOfflineJob(mFileActivity);
                }

                /// immediate content synchronization
                if (OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE == file.getAvailableOfflineStatus()) {
                    syncFile(file);
                } else {
                    cancelTransference(file);
                }
            } else {
                /// unexpected error
                mFileActivity.showSnackMessage(
                        mFileActivity.getString(R.string.common_error_unknown)
                );
            }
        }
    }

    public void renameFile(OCFile file, String newFilename) {
        // RenameFile
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_RENAME);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        service.putExtra(OperationsService.EXTRA_NEWNAME, newFilename);
        mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

        mFileActivity.showLoadingDialog(R.string.wait_a_moment);
    }

    /**
     * Start operations to delete one or several files
     *
     * @param files             Files to delete
     * @param onlyLocalCopy     When 'true' only local copy of the files is removed; otherwise files are also deleted
     *                          in the server.
     */
    public void removeFiles(Collection<OCFile> files, boolean onlyLocalCopy) {
        for (OCFile file : files) {
            // RemoveFile
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_REMOVE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, onlyLocalCopy);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }

        mFileActivity.showLoadingDialog(R.string.wait_a_moment);
    }

    public void createFolder(String remotePath, boolean createFullPath) {
        // Create Folder
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_CREATE_FOLDER);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, remotePath);
        service.putExtra(OperationsService.EXTRA_CREATE_FULL_PATH, createFullPath);
        mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

        mFileActivity.showLoadingDialog(R.string.wait_a_moment);
    }

    /**
     * Cancel the transference in downloads (files/folders) and file uploads
     * @param file OCFile
     */
    public void cancelTransference(OCFile file) {
        Account account = mFileActivity.getAccount();
        if (file.isFolder()) {
            OperationsService.OperationsServiceBinder opsBinder =
                    mFileActivity.getOperationsServiceBinder();
            if (opsBinder != null) {
                opsBinder.cancel(account, file);
            }
        }

        // for both files and folders
        FileDownloaderBinder downloaderBinder = mFileActivity.getFileDownloaderBinder();
        if (downloaderBinder != null && downloaderBinder.isDownloading(account, file)) {
            downloaderBinder.cancel(account, file);
        }
        FileUploaderBinder uploaderBinder = mFileActivity.getFileUploaderBinder();
        if (uploaderBinder != null && uploaderBinder.isUploading(account, file)) {
            uploaderBinder.cancel(account, file);
        }
    }

    /**
     * Start operations to move one or several files
     *
     * @param files            Files to move
     * @param targetFolder     Folder where the files while be moved into
     */
    public void moveFiles(Collection<OCFile> files, OCFile targetFolder) {
        for (OCFile file : files) {
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_MOVE_FILE);
            service.putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }
        mFileActivity.showLoadingDialog(R.string.wait_a_moment);
    }

    /**
     * Start operations to copy one or several files
     *
     * @param files            Files to copy
     * @param targetFolder     Folder where the files while be copied into
     */
    public void copyFiles(Collection<OCFile> files, OCFile targetFolder) {
        for (OCFile file : files) {
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_COPY_FILE);
            service.putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }
        mFileActivity.showLoadingDialog(R.string.wait_a_moment);
    }

    public long getOpIdWaitingFor() {
        return mWaitingForOpId;
    }

    public void setOpIdWaitingFor(long waitingForOpId) {
        mWaitingForOpId = waitingForOpId;
    }

    /**
     *  @return 'True' if the server doesn't need to check forbidden characters
     */
    public boolean isVersionWithForbiddenCharacters() {
        if (mFileActivity.getAccount() != null) {
            OwnCloudVersion serverVersion =
                    AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isVersionWithForbiddenCharacters());
        }
        return false;
    }

    /**
     * Starts a check of the currenlty stored credentials for the given account.
     *
     * @param account       OC account which credentials will be checked.
     */
    public void checkCurrentCredentials(Account account) {
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_CHECK_CURRENT_CREDENTIALS);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, account);
        mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

        mFileActivity.showLoadingDialog(R.string.wait_checking_credentials);
    }

    /**
     * Share link with other apps
     *
     * @param link link to share
     */
    private void shareLink(String link) {
        Intent intentToShareLink = new Intent(Intent.ACTION_SEND);
        intentToShareLink.putExtra(Intent.EXTRA_TEXT, link);
        intentToShareLink.setType("text/plain");
        String username = com.owncloud.android.lib.common.accounts.AccountUtils.getUsernameForAccount(
                mFileActivity.getAccount()
        );
        if (username != null) {
            intentToShareLink.putExtra(
                    Intent.EXTRA_SUBJECT,
                    mFileActivity.getString(
                            R.string.subject_user_shared_with_you,
                            username,
                            mFileActivity.getFile().getFileName()
                    )
            );
        } else {
            intentToShareLink.putExtra(
                    Intent.EXTRA_SUBJECT,
                    mFileActivity.getString(
                            R.string.subject_shared_with_you,
                            mFileActivity.getFile().getFileName()
                    )
            );
        }

        String[] packagesToExclude = new String[]{mFileActivity.getPackageName()};
        DialogFragment chooserDialog = ShareLinkToDialog.newInstance(intentToShareLink, packagesToExclude);
        chooserDialog.show(mFileActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);
    }
}
