/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.operations;

import java.util.ArrayList;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;

import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.syncadapter.FileSyncAdapter;


/**
 *  Operation performing a REFRESH on a folder, conceived to be triggered by an action started
 *  FROM THE USER INTERFACE.
 *
 *  Fetches the LIST and properties of the files contained in the given folder (including the
 *  properties of the folder itself), and updates the local database with them.
 *
 *  Synchronizes the CONTENTS of any file or folder set locally as AVAILABLE OFFLINE.
 *
 *  If the folder is ROOT, it also retrieves the VERSION of the server, and the USER PROFILE info.
 *
 *  Does NOT travel subfolders to refresh their contents also, UNLESS they are
 *  set as AVAILABLE OFFLINE FOLDERS.
 */
public class RefreshFolderOperation extends SyncOperation<ArrayList<RemoteFile>> {

    private static final String TAG = RefreshFolderOperation.class.getSimpleName();

    public static final String EVENT_SINGLE_FOLDER_CONTENTS_SYNCED  = 
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_CONTENTS_SYNCED";
    public static final String EVENT_SINGLE_FOLDER_SHARES_SYNCED    = 
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_SHARES_SYNCED";
    
    /** Locally cached information about folder to synchronize */
    private OCFile mLocalFolder;
    
    /** Account where the file to synchronize belongs */
    private Account mAccount;
    
    /** Android context; necessary to send requests to the download service */
    private Context mContext;
    
    /** 'True' means that Share resources bound to the files into should be refreshed also */
    private boolean mIsShareSupported;

    /**
     * 'True' means that the list of files in the remote folder should
     *  be fetched and merged locally even though the 'eTag' did not change.
     */
    private boolean mIgnoreETag;    // TODO - use it prefetching ETag of folder; two PROPFINDS, but better
                                    // TODO -   performance with (big) unchanged folders

    private LocalBroadcastManager mLocalBroadcastManager;

    /**
     * Creates a new instance of {@link RefreshFolderOperation}.
     * 
     * @param   folder                  Folder to synchronize.
     * @param   isShareSupported        'True' means that the server supports the sharing API.
     * @param   ignoreETag              'True' means that the content of the remote folder should
     *                                  be fetched and updated even though the 'eTag' did not 
     *                                  change.  
     * @param   account                 ownCloud account where the folder is located.
     * @param   context                 Application context.
     */
    public RefreshFolderOperation(OCFile folder,
                                  boolean isShareSupported,
                                  boolean ignoreETag,
                                  Account account,
                                  Context context) {
        mLocalFolder = folder;
        mIsShareSupported = isShareSupported;
        mAccount = account;
        mContext = context;
        mIgnoreETag = ignoreETag;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
    }
    
    
    /**
     * Performs the synchronization.
     * 
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult<ArrayList<RemoteFile>> run(OwnCloudClient client) {
        RemoteOperationResult<ArrayList<RemoteFile>> result;

        // get 'fresh data' from the database
        mLocalFolder = getStorageManager().getFileByPath(mLocalFolder.getRemotePath());

        // only in root folder: sync server version and user profile
        if (OCFile.ROOT_PATH.equals(mLocalFolder.getRemotePath())) {
            OwnCloudVersion serverVersion = syncCapabilitiesAndGetServerVersion();
            mIsShareSupported = serverVersion.isSharedSupported();
            syncUserProfile();
        }

        // sync list of files, and contents of available offline files & folders
        SynchronizeFolderOperation syncOp = new SynchronizeFolderOperation(
            mContext,
            mLocalFolder.getRemotePath(),
            mAccount,
            System.currentTimeMillis(),
            false,
            false,
            false
        );
        result = syncOp.execute(client, getStorageManager());

        sendLocalBroadcast(
                EVENT_SINGLE_FOLDER_CONTENTS_SYNCED, mLocalFolder.getRemotePath(), result);

        // sync list of shares
        if (result.isSuccess() && mIsShareSupported) {
            refreshSharesForFolder(client); // share result is ignored 
        }
        
        sendLocalBroadcast(
                EVENT_SINGLE_FOLDER_SHARES_SYNCED, mLocalFolder.getRemotePath(), result);

        return result;
        
    }

    private void syncUserProfile() {
        GetUserProfileOperation update = new GetUserProfileOperation(mLocalFolder.getRemotePath());
        RemoteOperationResult result = update.execute(getStorageManager(), mContext);
        if (!result.isSuccess()) {
            Log_OC.w(TAG, "Couldn't update user profile from server");
        } else {
            Log_OC.i(TAG, "Got user profile");
        }
    }

    private OwnCloudVersion syncCapabilitiesAndGetServerVersion() {
        OwnCloudVersion serverVersion;
        SyncCapabilitiesOperation getCapabilities = new SyncCapabilitiesOperation();
        RemoteOperationResult<OCCapability> result = getCapabilities.execute(getStorageManager(), mContext);
        if (result.isSuccess()) {
            OCCapability capability = result.getData();
            serverVersion = new OwnCloudVersion(capability.getVersionString());
        } else {
            // get whatever was stored before for the version
            serverVersion = AccountUtils.getServerVersion(mAccount);
        }
        return serverVersion;
    }

    /**
     * Syncs the Share resources for the files contained in the folder refreshed (children, not deeper descendants).
     *
     * @param client    Handler of a session with an OC server.
     * @return          The result of the remote operation retrieving the Share resources in the folder refreshed by
     *                  the operation.
     */
    private RemoteOperationResult<ShareParserResult> refreshSharesForFolder(OwnCloudClient client) {
        RemoteOperationResult<ShareParserResult> result;
        
        // remote request 
        GetRemoteSharesForFileOperation operation =
                new GetRemoteSharesForFileOperation(mLocalFolder.getRemotePath(), true, true);
        result = operation.execute(client);
        
        if (result.isSuccess()) {
            // update local database
            getStorageManager().saveSharesInFolder(result.getData().getShares(), mLocalFolder);
        }

        return result;
    }
    

    /**
     * Sends a message to any application component interested in the progress 
     * of the synchronization.
     * 
     * @param event             Action type to broadcast
     * @param dirRemotePath     Remote path of a folder that was just synchronized 
     *                          (with or without success)
     */
    private void sendLocalBroadcast(
            String event, String dirRemotePath, RemoteOperationResult result
        ) {
        Log_OC.d(TAG, "Send broadcast " + event);
        Intent intent = new Intent(event);
        intent.putExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME, mAccount.name);
        if (dirRemotePath != null) {
            intent.putExtra(FileSyncAdapter.EXTRA_FOLDER_PATH, dirRemotePath);
        }
        intent.putExtra(FileSyncAdapter.EXTRA_RESULT, result);
        mLocalBroadcastManager.sendBroadcast(intent);
    }
}
