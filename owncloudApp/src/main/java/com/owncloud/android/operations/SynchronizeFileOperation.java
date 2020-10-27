/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.operations;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.owncloud.android.MainApp;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;
import timber.log.Timber;

/**
 * Operation synchronizing the properties and contents of an OC file between local and remote copies.
 */

public class SynchronizeFileOperation extends SyncOperation {

    private OCFile mLocalFile;
    private String mRemotePath;
    private OCFile mServerFile;
    private Account mAccount;
    private boolean mPushOnly;
    private Context mContext;

    private boolean mTransferWasRequested = false;
    private boolean mRequestedFromAvOfflineJobService;

    /**
     * Constructor for "full synchronization mode".
     * <p>
     * Uses remotePath to retrieve all the data both in local cache and in the remote OC server
     * when the operation is executed, instead of reusing {@link OCFile} instances.
     * <p>
     * Useful for direct synchronization of a single file.
     *
     * @param remotePath Path to the OCFile to sync
     * @param account    ownCloud account holding the file.
     * @param context    Android context; needed to start transfers.
     */
    public SynchronizeFileOperation(
            String remotePath,
            Account account,
            Context context
    ) {

        mRemotePath = remotePath;
        mLocalFile = null;
        mServerFile = null;
        mAccount = account;
        mPushOnly = false;
        mContext = context;
        mRequestedFromAvOfflineJobService = false;
    }

    /**
     * Constructor allowing to reuse {@link OCFile} instances just queried from local cache or
     * from remote OC server.
     * <p>
     * Useful to include this operation as part of the synchronization of a folder
     * (or a full account), avoiding the repetition of fetch operations
     * (both in local database or remote server).
     * <p>
     * At least one of localFile or serverFile MUST NOT BE NULL. If you don't have none of them,
     * use the other constructor.
     *
     * @param localFile                        Data of file (just) retrieved from local cache/database.
     * @param serverFile                       Data of file (just) retrieved from a remote server. If null,
     *                                         will be retrieved from network by the operation when executed.
     * @param account                          ownCloud account holding the file.
     * @param pushOnly                         When 'true', if 'severFile' is NULL, will not fetch remote properties
     *                                         before
     *                                         trying to upload local changes; upload operation will take care of not
     *                                         overwriting
     *                                         remote content if there are unnoticed changes on the server.
     * @param context                          Android context; needed to start transfers.
     * @param requestedFromAvOfflineJobService When 'true' will perform some specific operations
     */
    public SynchronizeFileOperation(
            OCFile localFile,
            OCFile serverFile,
            Account account,
            boolean pushOnly,
            Context context,
            boolean requestedFromAvOfflineJobService
    ) {

        mLocalFile = localFile;
        mServerFile = serverFile;
        if (mLocalFile != null) {
            mRemotePath = mLocalFile.getRemotePath();
            if (mServerFile != null && !mServerFile.getRemotePath().equals(mRemotePath)) {
                throw new IllegalArgumentException("serverFile and localFile do not correspond" +
                        " to the same OC file");
            }
        } else if (mServerFile != null) {
            mRemotePath = mServerFile.getRemotePath();
        } else {
            throw new IllegalArgumentException("Both serverFile and localFile are NULL");
        }
        mAccount = account;
        mPushOnly = pushOnly;
        mContext = context;
        mRequestedFromAvOfflineJobService = requestedFromAvOfflineJobService;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        RemoteOperationResult<RemoteFile> result = null;
        mTransferWasRequested = false;

        if (mLocalFile == null) {
            // Get local file from the DB
            mLocalFile = getStorageManager().getFileByPath(mRemotePath);
        }

        if (!mLocalFile.isAvailableLocally()) {
            /// easy decision
            requestForDownload(mLocalFile);
            result = new RemoteOperationResult<>(ResultCode.OK);

        } else {
            /// local copy in the device -> need to think a bit more before do anything

            if (mServerFile == null && !mPushOnly) {
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(mRemotePath);
                result = operation.execute(client);
                if (result.isSuccess()) {
                    mServerFile = FileStorageUtils.createOCFileFromRemoteFile(result.getData());
                    mServerFile.setLastSyncDateForProperties(System.currentTimeMillis());
                }
            }

            if (mPushOnly || mServerFile != null) { // at this point, conditions should be exclusive

                /// decide if file changed in the server
                boolean serverChanged;
                if (mPushOnly) {
                    serverChanged = false;
                } else if (mLocalFile.getEtag() == null || mLocalFile.getEtag().length() == 0) {
                    // file uploaded (null) or downloaded ("")
                    // before upgrade to version 1.8.0; this is legacy condition
                    serverChanged = mServerFile.getModificationTimestamp() != mLocalFile.getModifiedAtLastSyncForData();
                } else {
                    serverChanged = (!mServerFile.getEtag().equals(mLocalFile.getEtag()));
                }

                /// decide if file changed in local device
                boolean localChanged =
                        (mLocalFile.getLocalModificationTimestamp() > mLocalFile.getLastSyncDateForData());

                /// decide action to perform depending upon changes
                if (localChanged && serverChanged) {
                    result = new RemoteOperationResult<>(ResultCode.SYNC_CONFLICT);
                    getStorageManager().saveConflict(mLocalFile, mServerFile.getEtag());

                } else if (localChanged) {
                    if (mPushOnly) {
                        // prevent accidental override of unnoticed change in server;
                        // dirty trick, more refactoring is needed, but not today;
                        // works due to {@link UploadFileOperation#L364,L367}
                        mLocalFile.setEtagInConflict(mLocalFile.getEtag());
                    }
                    requestForUpload(mLocalFile);
                    result = new RemoteOperationResult<>(ResultCode.OK);

                } else if (serverChanged) {
                    mLocalFile.setRemoteId(mServerFile.getRemoteId());
                    requestForDownload(mLocalFile);
                    // mLocalFile, not mServerFile; we want to keep the value of
                    // available-offline property
                    // the update of local data will be done later by the FileUploader
                    // service when the upload finishes
                    result = new RemoteOperationResult<>(ResultCode.OK);

                } else {
                    // nothing changed, nothing to do
                    result = new RemoteOperationResult<>(ResultCode.OK);
                }

                // safe blanket: sync'ing a not in-conflict file will clean wrong conflict markers in ancestors
                if (result.getCode() != ResultCode.SYNC_CONFLICT) {
                    getStorageManager().saveConflict(mLocalFile, null);
                }
            }
        }

        if (MainApp.Companion.isDeveloper()) {
            Timber.i("Synchronizing " + mAccount.name + ", file " + mLocalFile.getRemotePath() + ": " + result.getLogMessage());
        }

        return result;
    }

    /**
     * Requests for an upload to the FileUploader service
     *
     * @param file OCFile object representing the file to upload
     */
    private void requestForUpload(OCFile file) {
        TransferRequester requester = new TransferRequester();
        requester.uploadUpdate(mContext, mAccount, file, FileUploader.LOCAL_BEHAVIOUR_MOVE, true,
                mRequestedFromAvOfflineJobService);

        mTransferWasRequested = true;
    }

    /**
     * Requests for a download to the FileDownloader service
     *
     * @param file OCFile object representing the file to download
     */
    private void requestForDownload(OCFile file) {
        Intent intent = new Intent(mContext, FileDownloader.class);
        intent.putExtra(FileDownloader.KEY_ACCOUNT, mAccount);
        intent.putExtra(FileDownloader.KEY_FILE, file);

        // Since in Android O and above the apps in background are not allowed to start background
        // services and available offline feature may try to do it, this is the way to proceed
        if (mRequestedFromAvOfflineJobService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(FileDownloader.KEY_IS_AVAILABLE_OFFLINE_FILE, true);
            Timber.d("Download file from foreground/background, startForeground() will be called soon");
            mContext.startForegroundService(intent);
        } else {
            Timber.d("Download file from foreground");
            mContext.startService(intent);
        }

        mTransferWasRequested = true;
    }

    public boolean transferWasRequested() {
        return mTransferWasRequested;
    }

    public OCFile getLocalFile() {
        return mLocalFile;
    }
}
