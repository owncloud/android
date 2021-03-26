/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * @author Shashvat Kedia
 * Copyright (C) 2020 ownCloud GmbH.
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
import android.os.Build;
import android.webkit.MimeTypeMap;

import androidx.fragment.app.DialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.domain.sharing.shares.model.OCShare;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.presentation.manager.TransferManager;
import com.owncloud.android.presentation.ui.sharing.ShareActivity;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ShareLinkToDialog;
import com.owncloud.android.utils.UriUtilsKt;
import timber.log.Timber;

import java.util.Collection;
import java.util.List;

public class FileOperationsHelper {

    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG";

    private FileActivity mFileActivity;

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
                    UriUtilsKt.INSTANCE.getExposedFileUriForOCFile(mFileActivity, file),
                    file.getMimeType()
            );

            intentForSavedMimeType.setFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            Intent intentForGuessedMimeType = null;
            if (storagePath.lastIndexOf('.') >= 0) {
                String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        storagePath.substring(storagePath.lastIndexOf('.') + 1)
                );
                if (guessedMimeType != null && !guessedMimeType.equals(file.getMimeType())) {
                    intentForGuessedMimeType = new Intent(Intent.ACTION_VIEW);
                    intentForGuessedMimeType.setDataAndType(
                            UriUtilsKt.INSTANCE.getExposedFileUriForOCFile(mFileActivity, file),
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
                    queryIntentActivities(openFileWithIntent, PackageManager.MATCH_DEFAULT_ONLY);

            if (launchables.size() > 0) {
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
            Timber.e("Trying to open a NULL OCFile");
        }
    }

    /**
     * Show dialog to allow the user to choose an app to send the private link of an {@link OCFile},
     * or copy it to clipboard.
     *
     * @param file @param file {@link OCFile} which will be shared with internal users
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
     * Show dialog to allow the user to choose an app to send the link of an {@link OCShare},
     * or copy it to clipboard.
     *
     * @param share {@link OCShare} which link will be sent to the app chosen by the user.
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
     * Show an instance of {@link com.owncloud.android.domain.sharing.shares.model.ShareType} for sharing or unsharing
     * the {@link OCFile} received as parameter.
     *
     * @param file File to share or unshare.
     */
    public void showShareFile(OCFile file) {
        Intent intent = new Intent(mFileActivity, ShareActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, file);
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, mFileActivity.getAccount());
        mFileActivity.startActivity(intent);

    }

    public void sendDownloadedFile(OCFile file) {
        if (file != null) {
            Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
            // set MimeType
            sendIntent.setType(file.getMimeType());
            sendIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    UriUtilsKt.INSTANCE.getExposedFileUriForOCFile(mFileActivity, file)
            );
            sendIntent.putExtra(Intent.ACTION_SEND, true);      // Send Action

            // Show dialog, without the own app
            String[] packagesToExclude = new String[]{mFileActivity.getPackageName()};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent shareSheetIntent = new ShareSheetHelper().getShareSheetIntent(
                        sendIntent,
                        mFileActivity.getApplicationContext(),
                        R.string.activity_chooser_send_file_title,
                        packagesToExclude
                );

                mFileActivity.startActivity(shareSheetIntent);
            } else {
                DialogFragment chooserDialog = ShareLinkToDialog.newInstance(sendIntent, packagesToExclude);
                chooserDialog.show(mFileActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);
            }
        } else {
            Timber.e("Trying to send a NULL OCFile");
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
     * @param file The file or folder to synchronize
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
        // FIXME: 13/10/2020 : New_arch: Av.Offline

        //        if (OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT == file.getAvailableOfflineStatus()) {
        //            /// files descending of an av-offline folder can't be toggled
        //            mFileActivity.showSnackMessage(
        //                    mFileActivity.getString(R.string.available_offline_inherited_msg)
        //            );
        //
        //        } else {
        //            /// update local property, for file and all its descendents (if folder)
        //            OCFile.AvailableOfflineStatus targetAvailableOfflineStatus = isAvailableOffline ?
        //                    OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE :
        //                    OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE;
        //            file.setAvailableOfflineStatus(targetAvailableOfflineStatus);
        //            boolean success = mFileActivity.getStorageManager().saveLocalAvailableOfflineStatus(file);
        //
        //            if (success) {
        //                // Schedule job to check to watch for local changes in available offline files and sync them
        //                AvailableOfflineHandler availableOfflineHandler = new AvailableOfflineHandler(mFileActivity);
        //                availableOfflineHandler.scheduleAvailableOfflineJob(mFileActivity);
        //
        //                /// immediate content synchronization
        //                if (OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE == file.getAvailableOfflineStatus()) {
        //                    syncFile(file);
        //                } else {
        //                    cancelTransference(file);
        //                }
        //            } else {
        //                /// unexpected error
        //                mFileActivity.showSnackMessage(
        //                        mFileActivity.getString(R.string.common_error_unknown)
        //                );
        //            }
        //        }
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
     * @param files         Files to delete
     * @param onlyLocalCopy When 'true' only local copy of the files is removed; otherwise files are also deleted
     *                      in the server.
     */
    public void removeFiles(Collection<OCFile> files, boolean onlyLocalCopy) {
        int countOfFilesToRemove = 0;
        boolean isLastFileToRemove = false;
        for (OCFile file : files) {
            countOfFilesToRemove++;
            // RemoveFile
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_REMOVE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, onlyLocalCopy);
            if (countOfFilesToRemove == files.size()) {
                isLastFileToRemove = true;
            }
            service.putExtra(OperationsService.EXTRA_IS_LAST_FILE_TO_REMOVE, isLastFileToRemove);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }

        mFileActivity.showLoadingDialog(R.string.wait_a_moment);
    }

    /**
     * Cancel the transference in downloads (files/folders) and file uploads
     *
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
        TransferManager transferManager = new TransferManager(mFileActivity.getApplicationContext());
        if (transferManager.isDownloadPending(account, file)) {
            transferManager.cancelDownloadForFile(file);
        }
        FileUploaderBinder uploaderBinder = mFileActivity.getFileUploaderBinder();
        if (uploaderBinder != null && uploaderBinder.isUploading(account, file)) {
            uploaderBinder.cancel(account, file);
        }
    }

    /**
     * Start operations to move one or several files
     *
     * @param files        Files to move
     * @param targetFolder Folder where the files while be moved into
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
     * @param files        Files to copy
     * @param targetFolder Folder where the files while be copied into
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
     * Starts a check of the currenlty stored credentials for the given account.
     *
     * @param account OC account which credentials will be checked.
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent shareSheetIntent = new ShareSheetHelper().getShareSheetIntent(
                    intentToShareLink,
                    mFileActivity.getApplicationContext(),
                    R.string.activity_chooser_title,
                    packagesToExclude
            );

            mFileActivity.startActivity(shareSheetIntent);
        } else {
            DialogFragment chooserDialog = ShareLinkToDialog.newInstance(intentToShareLink, packagesToExclude);
            chooserDialog.show(mFileActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);
        }
    }
}
