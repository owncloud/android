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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.utils.FileStorageUtils;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 *  Remote operation performing the synchronization of the list of files contained 
 *  in a folder identified with its remote path.
 *  
 *  Fetches the list and properties of the files contained in the given folder, including their 
 *  properties, and updates the local database with them.
 *  
 *  Does NOT enter in the child folders to synchronize their contents also, BUT requests for a new operation instance
 *  doing so.
 */
public class SynchronizeFolderOperation extends SyncOperation {

    private static final String TAG = SynchronizeFolderOperation.class.getSimpleName();

    /** Time stamp for the synchronization process in progress */
    private long mCurrentSyncTime;

    /** Remote path of the folder to synchronize */
    private String mRemotePath;
    
    /** Account where the file to synchronize belongs */
    private Account mAccount;

    /** Android context; necessary to send requests to the download service */
    private Context mContext;

    /** Locally cached information about folder to synchronize */
    private OCFile mLocalFolder;

    /** Files and folders contained in the synchronized folder after a successful operation */
    //private List<OCFile> mChildren;

    /** Counter of conflicts found between local and remote files */
    private int mConflictsFound;

    /** Counter of failed operations in synchronization of kept-in-sync files */
    private int mFailsInFileSyncsFound;

    private List<OCFile> mFilesForDirectDownload;
        // to avoid extra PROPFINDs when there was no change in the folder
    
    private List<SyncOperation> mFilesToSyncContents;

    private List<Intent> mFoldersToSyncContents;

    private final AtomicBoolean mCancellationRequested;

    /**
     * When 'true', will assume that folder did not change in the server and
     * will focus only in push any local change to the server (carefully).
     */
    private boolean mPushOnly;

    /**
     * Creates a new instance of {@link SynchronizeFolderOperation}.
     *
     * @param   context                 Application context.
     * @param   remotePath              Path to synchronize.
     * @param   account                 ownCloud account where the folder is located.
     * @param   currentSyncTime         Time stamp for the synchronization process in progress.
     * @param   pushOnly                When 'true', will assume that folder did not change in the server and
     *                                  will focus only in push any local change to the server (carefully).
     */
    public SynchronizeFolderOperation(
        Context context,
        String remotePath,
        Account account,
        long currentSyncTime,
        boolean pushOnly
    ) {
        mRemotePath = remotePath;
        mCurrentSyncTime = currentSyncTime;
        mAccount = account;
        mContext = context;
        mFilesForDirectDownload = new Vector<>();
        mFilesToSyncContents = new Vector<>();
        mFoldersToSyncContents = new Vector<>();
        mCancellationRequested = new AtomicBoolean(false);
        mPushOnly = pushOnly;
    }


    public int getConflictsFound() {
        return mConflictsFound;
    }

    public int getFailsInFileSyncsFound() {
        return mFailsInFileSyncsFound;
    }

    /**
     * Performs the synchronization.
     *
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        mFailsInFileSyncsFound = 0;
        mConflictsFound = 0;
        
        try {
            // get locally cached information about folder
            mLocalFolder = getStorageManager().getFileByPath(mRemotePath);

            if (!mPushOnly) {
                // get list of files in folder from remote server
                result = fetchRemoteFolder(client);
            }

            if (result != null && result.isSuccess()) {
                // folder was updated in the server side
                Log_OC.i(TAG, "Checked " + mAccount.name + mRemotePath + " : changed");

                mergeRemoteFolder(result.getData());
                syncContents();

            } else if (mPushOnly || result.getCode() == ResultCode.NOT_MODIFIED) {
                // no update in the server side, still need to handle local changes

                Log_OC.i(TAG, "Checked " + mAccount.name + mRemotePath + " : not changed");

                preparePushOfLocalChanges();
                syncContents();

                result = new RemoteOperationResult(ResultCode.OK);

            } else {
                // fail fetching the server
                if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                    removeLocalFolder();
                }
                if (result.isException()) {
                    Log_OC.e(TAG, "Checked " + mAccount.name + mRemotePath  + " : " +
                        result.getLogMessage(), result.getException());
                } else {
                    Log_OC.e(TAG, "Checked " + mAccount.name + mRemotePath + " : " +
                        result.getLogMessage());
                }
            }

        } catch (OperationCancelledException e) {
            result = new RemoteOperationResult(e);
        }

        return result;

    }

    /**
     * Get list of files in folder from remote server, only if there were changes from the last time.
     *
     * Stored ETag of folder is used to determine if there were changes in the server from the last sync.
     *
     * @param client                            {@link OwnCloudClient} instance used to access the server.
     * @return                                  Result of the fetch, including list of remote files in the
     *                                          sync'ed folder, if changed from the last time.
     * @throws OperationCancelledException
     */
    @NonNull
    private RemoteOperationResult fetchRemoteFolder(OwnCloudClient client) throws OperationCancelledException {
        Log_OC.d(TAG, "Fetching list of files in  " + mAccount.name + mRemotePath + ", if changed");

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        ReadRemoteFolderOperation readFolderOperation = new ReadRemoteFolderOperation(
            mRemotePath,
            mLocalFolder.getEtag()
        );
        return readFolderOperation.execute(client);
    }


    private void removeLocalFolder() {
        FileDataStorageManager storageManager = getStorageManager();
        if (storageManager.fileExists(mLocalFolder.getFileId())) {
            String currentSavePath = FileStorageUtils.getSavePath(mAccount.name);
            storageManager.removeFolder(
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
     *  @param folderAndFiles   Remote folder and children files in Folder
     */
    private void mergeRemoteFolder(ArrayList<Object> folderAndFiles)
            throws OperationCancelledException {
        Log_OC.d(TAG, "Synchronizing " + mAccount.name + mRemotePath);

        FileDataStorageManager storageManager = getStorageManager();
        
        // parse data from remote folder
        OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) folderAndFiles.get(0));
        remoteFolder.setParentId(mLocalFolder.getParentId());
        remoteFolder.setFileId(mLocalFolder.getFileId());

        Log_OC.d(TAG, "Remote folder " + mLocalFolder.getRemotePath()
                + " changed - starting update of local data ");

        List<OCFile> updatedFiles = new Vector<>(folderAndFiles.size() - 1);
        mFilesForDirectDownload.clear();
        mFilesToSyncContents.clear();

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        // get current data about local contents of the folder to synchronize
        List<OCFile> localFiles = storageManager.getFolderContent(mLocalFolder);
        Map<String, OCFile> localFilesMap = new HashMap<>(localFiles.size());
        for (OCFile file : localFiles) {
            localFilesMap.put(file.getRemotePath(), file);
        }

        // loop to synchronize every child
        OCFile remoteFile, localFile, updatedLocalFile;
        RemoteFile r;
        for (int i=1; i<folderAndFiles.size(); i++) {
            /// new OCFile instance with the data from the server
            r = (RemoteFile) folderAndFiles.get(i);
            remoteFile = FileStorageUtils.fillOCFile(r);

            /// new OCFile instance to merge fresh data from server with local state
            updatedLocalFile = FileStorageUtils.fillOCFile(r);
            updatedLocalFile.setParentId(mLocalFolder.getFileId());

            /// retrieve local data for the read file
            localFile = localFilesMap.remove(remoteFile.getRemotePath());

            /// add to updatedFile data about LOCAL STATE (not existing in server)
            updatedLocalFile.setLastSyncDateForProperties(mCurrentSyncTime);
            if (localFile != null) {
                updatedLocalFile.setFileId(localFile.getFileId());
                updatedLocalFile.setAvailableOfflineStatus(localFile.getAvailableOfflineStatus());
                updatedLocalFile.setLastSyncDateForData(localFile.getLastSyncDateForData());
                updatedLocalFile.setModificationTimestampAtLastSyncForData(
                        localFile.getModificationTimestampAtLastSyncForData()
                );
                updatedLocalFile.setStoragePath(localFile.getStoragePath());
                // eTag will not be updated unless file CONTENTS are synchronized
                updatedLocalFile.setEtag(localFile.getEtag());
                if (!updatedLocalFile.isFolder() &&
                    remoteFile.isImage() &&
                    remoteFile.getModificationTimestamp() != localFile.getModificationTimestamp()) {

                    updatedLocalFile.setNeedsUpdateThumbnail(true);
                    Log.d(TAG, "Image " + remoteFile.getFileName() + " updated on the server");
                }
                updatedLocalFile.setPublicLink(localFile.getPublicLink());
                updatedLocalFile.setShareViaLink(localFile.isSharedViaLink());
                updatedLocalFile.setShareWithSharee(localFile.isSharedWithSharee());
                updatedLocalFile.setEtagInConflict(localFile.getEtagInConflict());
            } else {
                // remote eTag will not be updated unless file CONTENTS are synchronized
                updatedLocalFile.setEtag("");
            }

            /// check and fix, if needed, local storage path
            searchForLocalFileInDefaultPath(updatedLocalFile);
            
            /// classify file to sync/download contents later
            addToSyncContents(updatedLocalFile, remoteFile, false);

            updatedFiles.add(updatedLocalFile);
        }

        // save updated contents in local database
        storageManager.saveFolder(remoteFolder, updatedFiles, localFilesMap.values());

    }
    
    
    private void preparePushOfLocalChanges() throws OperationCancelledException {
        List<OCFile> children = getStorageManager().getFolderContent(mLocalFolder);
        for (OCFile child : children) {
            addToSyncContents(
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
     * @param serverUnchanged   When 'true', will assume that folder didn't change in server side, so that
     *                          synchronizations of subfolders will not check for changes again.
     */
    private void addToSyncContents(OCFile localFile, OCFile remoteFile, boolean serverUnchanged) {
        if (localFile.isFolder()) {
            /// to sync descendants
            Intent intent = new Intent(mContext, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, mAccount);
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, localFile.getRemotePath());
            intent.putExtra(OperationsService.EXTRA_PUSH_ONLY, serverUnchanged);
            mFoldersToSyncContents.add(intent);

        } else if (!localFile.isDown()) {
            mFilesForDirectDownload.add(localFile);

        } else if (!localFile.isInConflict()) {
            /// synchronization for regular files
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


    private void syncContents() throws OperationCancelledException {
        startDirectDownloads();
        startContentSynchronizations(mFilesToSyncContents);
    }

    
    private void startDirectDownloads() throws OperationCancelledException {
        for (OCFile file : mFilesForDirectDownload) {
            synchronized(mCancellationRequested) {
                if (mCancellationRequested.get()) {
                    throw new OperationCancelledException();
                }
                Intent i = new Intent(mContext, FileDownloader.class);
                i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
                i.putExtra(FileDownloader.EXTRA_FILE, file);
                mContext.startService(i);
            }
        }
    }

    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed
     * or if exists conflict due to changes both in local and remote contents of the each file.
     *
     * If download or upload is needed, request the operation to the corresponding service and goes
     * on.
     *
     * @param filesToSyncContents       Synchronization operations to execute.
     */
    private void startContentSynchronizations(List<SyncOperation> filesToSyncContents)
            throws OperationCancelledException {

        Log_OC.v(TAG, "Starting content synchronization... ");
        RemoteOperationResult contentsResult;
        for (SyncOperation op: filesToSyncContents) {
            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }
            contentsResult = op.execute(getStorageManager(), mContext);
            if (!contentsResult.isSuccess()) {
                if (contentsResult.getCode() == ResultCode.SYNC_CONFLICT) {
                    mConflictsFound++;
                } else {
                    mFailsInFileSyncsFound++;
                    if (contentsResult.getException() != null) {
                        Log_OC.e(TAG, "Error while synchronizing file : "
                            + contentsResult.getLogMessage(), contentsResult.getException());
                    } else {
                        Log_OC.e(TAG, "Error while synchronizing file : "
                            + contentsResult.getLogMessage());
                    }
                }
            }   // won't let these fails break the synchronization process
        }
        for (Intent intent: mFoldersToSyncContents) {
            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }
            mContext.startService(intent);
        }
    }

    /**
     * Scans the default location for saving local copies of files searching for
     * a 'lost' file with the same full name as the {@link com.owncloud.android.datamodel.OCFile}
     * received as parameter.
     *  
     * @param file      File to associate a possible 'lost' local file.
     */
    private void searchForLocalFileInDefaultPath(OCFile file) {
        if (file.getStoragePath() == null && !file.isFolder()) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
    }

    
    /**
     * Cancel operation
     */
    public void cancel() {
        mCancellationRequested.set(true);
    }

    public String getFolderPath() {
        String path = mLocalFolder.getStoragePath();
        if (path != null && path.length() > 0) {
            return path;
        }
        return FileStorageUtils.getDefaultSavePathFor(mAccount.name, mLocalFolder);
    }

    public String getRemotePath() {
        return mRemotePath;
    }
}
