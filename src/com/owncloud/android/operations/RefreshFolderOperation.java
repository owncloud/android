/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.owncloud.android.datamodel.OCFile;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.utils.FileStorageUtils;



/**
 *  Operation performing the synchronization of the LIST of files contained
 *  in a folder identified with its remote path, and the CONTENTS of the AVAILABLE OFFLINE
 *  files in it.
 *  
 *  Fetches the list and properties of the files contained in the given folder (including the
 *  properties of the folder itself), and updates the local database with them.
 *
 *  Synchronizes the contents of any file or folder set locally as available offline.
 *  
 *  Does NOT travers its children folders to synchronize their contents also, unless they are
 *  set as available offline folders.
 */
public class RefreshFolderOperation extends SyncOperation {

    private static final String TAG = RefreshFolderOperation.class.getSimpleName();

    public static final String EVENT_SINGLE_FOLDER_CONTENTS_SYNCED  = 
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_CONTENTS_SYNCED";
    public static final String EVENT_SINGLE_FOLDER_SHARES_SYNCED    = 
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_SHARES_SYNCED";
    
    /** Time stamp for the synchronization process in progress */
    private long mCurrentSyncTime;

    /** Locally cached information about folder to synchronize */
    private OCFile mLocalFolder;
    
    /** Account where the file to synchronize belongs */
    private Account mAccount;
    
    /** Android context; necessary to send requests to the download service */
    private Context mContext;
    
    /** Files and folders contained in the synchronized folder after a successful operation */
    private List<OCFile> mChildren;

    /** Counter of conflicts found between local and remote files */
    private int mConflictsFound;

    /** Counter of failed operations in synchronization of kept-in-sync files */
    private int mFailsInFavouritesFound;

    /**
     * Map of remote and local paths to files that where locally stored in a location 
     * out of the ownCloud folder and couldn't be copied automatically into it 
     **/
    private Map<String, String> mForgottenLocalFiles;

    /** 'True' means that this operation is part of a full account synchronization */ 
    private boolean mSyncFullAccount;

    /** 'True' means that Share resources bound to the files into should be refreshed also */
    private boolean mIsShareSupported;
    
    /** 'True' means that Etag will be ignored */
    private boolean mIgnoreETag;

    private List<SynchronizeFileOperation> mFilesToSyncContents;

    private List<Intent> mFoldersToSyncContents;


    /**
     * Creates a new instance of {@link RefreshFolderOperation}.
     * 
     * @param   folder                  Folder to synchronize.
     * @param   currentSyncTime         Time stamp for the synchronization process in progress.
     * @param   syncFullAccount         'True' means that this operation is part of a full account 
     *                                  synchronization.
     * @param   isShareSupported        'True' means that the server supports the sharing API.           
     * @param   ignoreETag              'True' means that the content of the remote folder should
     *                                  be fetched and updated even though the 'eTag' did not 
     *                                  change.  
     * @param   account                 ownCloud account where the folder is located.
     * @param   context                 Application context.
     */
    public RefreshFolderOperation(OCFile folder,
                                  long currentSyncTime,
                                  boolean syncFullAccount,
                                  boolean isShareSupported,
                                  boolean ignoreETag,
                                  Account account,
                                  Context context) {
        mLocalFolder = folder;
        mCurrentSyncTime = currentSyncTime;
        mSyncFullAccount = syncFullAccount;
        mIsShareSupported = isShareSupported;
        mAccount = account;
        mContext = context;
        mForgottenLocalFiles = new HashMap<>();
        mIgnoreETag = ignoreETag;
        mFilesToSyncContents = new Vector<>();
        mFoldersToSyncContents = new Vector<>();
    }
    
    
    public int getConflictsFound() {
        return mConflictsFound;
    }
    
    public int getFailsInFavouritesFound() {
        return mFailsInFavouritesFound;
    }
    
    public Map<String, String> getForgottenLocalFiles() {
        return mForgottenLocalFiles;
    }
    
    /**
     * Returns the list of files and folders contained in the synchronized folder, 
     * if called after synchronization is complete.
     * 
     * @return  List of files and folders contained in the synchronized folder.
     */
    public List<OCFile> getChildren() {
        return mChildren;
    }
    
    /**
     * Performs the synchronization.
     * 
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        mFailsInFavouritesFound = 0;
        mConflictsFound = 0;
        mForgottenLocalFiles.clear();

        // get 'fresh data' from the database
        mLocalFolder = getStorageManager().getFileByPath(mLocalFolder.getRemotePath());

        if (OCFile.ROOT_PATH.equals(mLocalFolder.getRemotePath()) && !mSyncFullAccount) {
            updateOCVersion(client);
            updateUserProfile();
        }

        // get list of files in folder from remote server
        result = fetchRemoteFolder(client);

        if (result.isSuccess()) {
            if (mIgnoreETag || folderChanged((RemoteFile)result.getData().get(0))) {
                if (!mIgnoreETag) {
                    // folder was updated in the server side
                    Log_OC.i(
                        TAG,
                        "Checked " + mAccount.name + mLocalFolder.getRemotePath() + ", changed"
                    );
                } else {
                    Log_OC.i(TAG, "Forced merge from server");
                }
                mergeRemoteFolder(result.getData());
                syncContents(); // for available offline files

            } else {
                // folder was not updated in the server side
                Log_OC.i(
                    TAG, "Checked " + mAccount.name + mLocalFolder.getRemotePath() + ", not changed"
                );
                preparePushOfLocalChangesForAvailableOfflineFiles();
                mChildren = getStorageManager().getFolderContent(mLocalFolder);
                // request for the synchronization of KEPT-IN-SYNC file and folder contents
                syncContents();
            }

        } else {
            // fail fetching the server
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                removeLocalFolder();
            }
            if (result.isException()) {
                Log_OC.e(TAG, "Checked " + mAccount.name + mLocalFolder.getRemotePath() + " : " +
                    result.getLogMessage(), result.getException());
            } else {
                Log_OC.e(TAG, "Checked " + mAccount.name + mLocalFolder.getRemotePath() + " : " +
                    result.getLogMessage());
            }
        }

        if (!mSyncFullAccount) {
            sendLocalBroadcast(
                    EVENT_SINGLE_FOLDER_CONTENTS_SYNCED, mLocalFolder.getRemotePath(), result
            );
        }
        
        if (result.isSuccess() && mIsShareSupported && !mSyncFullAccount) {
            refreshSharesForFolder(client); // share result is ignored 
        }
        
        if (!mSyncFullAccount) {            
            sendLocalBroadcast(
                    EVENT_SINGLE_FOLDER_SHARES_SYNCED, mLocalFolder.getRemotePath(), result
            );
        }
        
        return result;
        
    }

    private void updateOCVersion(OwnCloudClient client) {
        UpdateOCVersionOperation update = new UpdateOCVersionOperation(mAccount, mContext);
        RemoteOperationResult result = update.execute(client);
        if (result.isSuccess()) {
            mIsShareSupported = update.getOCVersion().isSharedSupported();

            // Update Capabilities for this account
            if (update.getOCVersion().isVersionWithCapabilitiesAPI()) {
                updateCapabilities();
            } else {
                Log_OC.d(TAG, "Capabilities API disabled");
            }
        }
    }

    private void updateUserProfile() {
        GetUserProfileOperation update = new GetUserProfileOperation();
        RemoteOperationResult result = update.execute(getStorageManager(), mContext);
        if (!result.isSuccess()) {
            Log_OC.w(TAG, "Couldn't update user profile from server");
        } else {
            Log_OC.i(TAG, "Got user profile");
        }
    }

    private void updateCapabilities(){
        GetCapabilitiesOperarion getCapabilities = new GetCapabilitiesOperarion();
        RemoteOperationResult  result = getCapabilities.execute(getStorageManager(),mContext);
        if (!result.isSuccess()){
            Log_OC.w(TAG, "Update Capabilities unsuccessfully");
        }
    }

    /**
     * Get list of files in folder from remote server, only if there were changes from the last time OR
     * if {@link #mIgnoreETag} is true.
     *
     * Stored ETag of folder is used to determine if there were changes in the server from the last sync.
     *
     * @param client                            {@link OwnCloudClient} instance used to access the server.
     * @return                                  Result of the fetch, including list of remote files in the
     *                                          sync'ed folder, if changed from the last time.
     */
    @NonNull
    private RemoteOperationResult fetchRemoteFolder(OwnCloudClient client) {
        Log_OC.d(
            TAG,
            "Fetching list of files in  " + mAccount.name + mLocalFolder.getRemotePath() + ", if changed"
        );

        ReadRemoteFolderOperation readFolderOperation = new ReadRemoteFolderOperation(
            mLocalFolder.getRemotePath()
        );
        return readFolderOperation.execute(client);
    }


    /**
     * Compares stored ETag of folder being synchronized to determine if there were changes in the server
     * from the last sync.
     *
     * @param remoteFolder      Properties of the remote copy of the folder
     * @return                  'true' if ETag of local and remote folder do not match.
     */
    private boolean folderChanged(RemoteFile remoteFolder) {
        return (!mLocalFolder.getEtag().equals(remoteFolder.getEtag()));
    }


    private void removeLocalFolder() {
        if (getStorageManager().fileExists(mLocalFolder.getFileId())) {
            String currentSavePath = FileStorageUtils.getSavePath(mAccount.name);
            getStorageManager().removeFolder(
                    mLocalFolder, 
                    true, 
                    (   mLocalFolder.isDown() && 
                            mLocalFolder.getStoragePath().startsWith(currentSavePath)
                    )
            );
        }
    }


    /**
     *  Synchronizes the data retrieved from the server about the contents of the target folder 
     *  with the current data in the local database.
     *  
     *  Grants that mChildren is updated with fresh data after execution.
     *  
     *  @param folderAndFiles   Remote folder and children files in Folder 
     */
    private void mergeRemoteFolder(ArrayList<Object> folderAndFiles) {

        Log_OC.d(TAG, "Synchronizing " + mAccount.name + mLocalFolder.getRemotePath());

        // parse data from remote folder
        OCFile remoteFolder = FileStorageUtils.createOCFileFrom(
            (RemoteFile) folderAndFiles.get(0)
        );
        remoteFolder.copyLocalPropertiesFrom(mLocalFolder);

        Log_OC.d(TAG, "Remote folder " + mLocalFolder.getRemotePath()
                + " changed - starting update of local data ");
        
        List<OCFile> updatedFiles = new Vector<>(folderAndFiles.size() - 1);
        mFilesToSyncContents.clear();

        // get current data about local contents of the folder to synchronize
        List<OCFile> localFiles = getStorageManager().getFolderContent(mLocalFolder);
        Map<String, OCFile> localFilesMap = new HashMap<>(localFiles.size());
        for (OCFile file : localFiles) {
            localFilesMap.put(file.getRemotePath(), file);
        }
        
        // loop to update every child
        OCFile remoteFile, localFile, updatedLocalFile;
        RemoteFile r;
        for (int i=1; i<folderAndFiles.size(); i++) {
            /// new OCFile instance with the data from the server
            r = (RemoteFile) folderAndFiles.get(i);
            remoteFile = FileStorageUtils.createOCFileFrom(r);

            /// new OCFile instance to merge fresh data from server with local state
            updatedLocalFile = FileStorageUtils.createOCFileFrom(r);

            /// retrieve local data for the read file 
            localFile = localFilesMap.remove(remoteFile.getRemotePath());
            
            /// add to updatedFile data about LOCAL STATE (not existing in server)
            updatedLocalFile.setLastSyncDateForProperties(mCurrentSyncTime);
            if (localFile != null) {
                updatedLocalFile.copyLocalPropertiesFrom(localFile);
                // remote eTag will not be set unless file CONTENTS are synchronized
                updatedLocalFile.setEtag(localFile.getEtag());
                if (!updatedLocalFile.isFolder() &&
                    remoteFile.isImage() &&
                    remoteFile.getModificationTimestamp() != localFile.getModificationTimestamp()) {
                    updatedLocalFile.setNeedsUpdateThumbnail(true);
                }

            } else {
                updatedLocalFile.setParentId(mLocalFolder.getFileId());
                // remote eTag will not be updated unless file CONTENTS are synchronized
                updatedLocalFile.setEtag("");
            }

            /// check and fix, if needed, local storage path
            FileStorageUtils.searchForLocalFileInDefaultPath(updatedLocalFile, mAccount);

            /// prepare content synchronization for kept-in-sync files
            addToSyncContentsIfAvailableOffline(updatedLocalFile, remoteFile, false);

            updatedFiles.add(updatedLocalFile);
        }

        // save updated contents in local database
        getStorageManager().saveFolder(remoteFolder, updatedFiles, localFilesMap.values());

        mChildren = updatedFiles;
    }

    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed
     * or if exists conflict due to changes both in local and remote contents of the each file.
     * 
     * If download or upload is needed, request the operation to the corresponding service and goes 
     * on.
     * 
     */
    private void syncContents() {
        RemoteOperationResult contentsResult;
        for (SyncOperation op: mFilesToSyncContents) {
            contentsResult = op.execute(getStorageManager(), mContext);
            if (!contentsResult.isSuccess()) {
                if (contentsResult.getCode() == ResultCode.SYNC_CONFLICT) {
                    mConflictsFound++;
                } else {
                    mFailsInFavouritesFound++;
                    if (contentsResult.getException() != null) {
                        Log_OC.e(TAG, "Error while synchronizing favourites : " 
                                +  contentsResult.getLogMessage(), contentsResult.getException());
                    } else {
                        Log_OC.e(TAG, "Error while synchronizing favourites : " 
                                + contentsResult.getLogMessage());
                    }
                }
            }   // won't let these fails break the synchronization process
        }

        for (Intent intent: mFoldersToSyncContents) {
            mContext.startService(intent);
        }
    }


    /**
     * Syncs the Share resources for the files contained in the folder refreshed (children, not deeper descendants).
     *
     * @param client    Handler of a session with an OC server.
     * @return          The result of the remote operation retrieving the Share resources in the folder refreshed by
     *                  the operation.
     */
    private RemoteOperationResult refreshSharesForFolder(OwnCloudClient client) {
        RemoteOperationResult result;
        
        // remote request 
        GetRemoteSharesForFileOperation operation = 
                new GetRemoteSharesForFileOperation(mLocalFolder.getRemotePath(), true, true);
        result = operation.execute(client);
        
        if (result.isSuccess()) {
            // update local database
            ArrayList<OCShare> shares = new ArrayList<>();
            for(Object obj: result.getData()) {
                shares.add((OCShare) obj);
            }
            getStorageManager().saveSharesInFolder(shares, mLocalFolder);
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
        mContext.sendStickyBroadcast(intent);
        //LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }


    private void preparePushOfLocalChangesForAvailableOfflineFiles() {
        List<OCFile> children = getStorageManager().getFolderContent(mLocalFolder);
        for (OCFile child : children) {
            addToSyncContentsIfAvailableOffline(
                child,
                null,
                true
            );
        }
    }

    /**
     * Generates the appropriate operations to later sync the contents of localFile with the server, if
     * it's an available offline file or folder.
     *
     * Stores the operations in mFoldersToSyncContents and mFilesToSyncContents.
     *
     * @param localFile         Local information about the file which contents might be sync'ed.
     * @param remoteFile        Server information of the file.
     * @param serverUnchanged   When 'true', will assume that folder didn't change in server side, so
     *                          synchronization of available offline subfolders will not check for
     *                          changes again.
     */
    private void addToSyncContentsIfAvailableOffline(
        OCFile localFile, OCFile remoteFile, boolean serverUnchanged
    ) {
        if (localFile.isAvailableOfflineStatus()) {
            if (localFile.isFolder() &&
                !mSyncFullAccount   // if full account is being traversed by {@link FileSyncAdapter},
                                    // children will be synced in subsequent {@link RefreshFolderOperation}s
                ) {
                Intent intent = new Intent(mContext, OperationsService.class);
                intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
                intent.putExtra(OperationsService.EXTRA_ACCOUNT, mAccount);
                intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, localFile.getRemotePath());
                intent.putExtra(OperationsService.EXTRA_PUSH_ONLY, serverUnchanged);
                mFoldersToSyncContents.add(intent);

            } else if (!localFile.isInConflict()) {
                SynchronizeFileOperation operation = new SynchronizeFileOperation(
                    localFile,
                    remoteFile,
                    mAccount,
                    serverUnchanged,
                    mContext
                );
                mFilesToSyncContents.add(operation);
            }
        }
    }

}
