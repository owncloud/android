/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author Christian Schabesberger
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
import android.support.v4.util.Pair;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
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
 *  Operation performing the synchronization of the list of files contained
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

    /** Counter of conflicts found between local and remote files */
    private int mConflictsFound;

    /** Counter of failed operations in synchronization of kept-in-sync files */
    private int mFailsInFileSyncsFound;

    /**
     * Map of remote and local paths to files that where locally stored in a location
     * out of the ownCloud folder and couldn't be copied automatically into it
     **/
    private Map<String, String> mForgottenLocalFiles;

    private List<SynchronizeFileOperation> mOperationsOfFilesToSync;

    private List<Intent> mIntentListForFurtherFolderSync;

    private final AtomicBoolean mCancellationRequested;

    /** Files and folders contained in the synchronized folder after a successful operation */
    private List<Pair<OCFile, Boolean>> mFoldersToVisit;

    /**
     * When 'true', will assume that folder did not change in the server and
     * will focus only in push any local change to the server (carefully).
     */
    private boolean mPushOnly;

    /** 'True' means that this operation is part of a full account synchronization */
    private boolean mSyncFullAccount;

    /** 'True' means that the contents of all the files in the folder will be synchronized;
     * otherwise, only contents of available offline files will be synchronized. */
    private final boolean mSyncContentOfRegularFiles;

    /**
     * Creates a new instance of {@link SynchronizeFolderOperation}.
     *
     * @param   context                     Application context.
     * @param   remotePath                  Path to synchronize.
     * @param   account                     ownCloud account where the folder is located.
     * @param   currentSyncTime             Time stamp for the synchronization process in progress.
     * @param   pushOnly                    When 'true', will assume that folder did not change in the server and
     *                                      will focus only in push any local change to the server (carefully).
     * @param   syncFullAccount             'True' means that this operation is part of a full account
     *                                      synchronization.
     * @param   syncContentOfRegularFiles   When 'true', the contents of all the files in the folder will
     *                                      be synchronized; otherwise, only contents of available offline files
     *                                      will be synchronized.
     */
    public SynchronizeFolderOperation(
        Context context,
        String remotePath,
        Account account,
        long currentSyncTime,
        boolean pushOnly,
        boolean syncFullAccount,
        boolean syncContentOfRegularFiles
    ) {
        mRemotePath = remotePath;
        mCurrentSyncTime = currentSyncTime;
        mAccount = account;
        mContext = context;
        mOperationsOfFilesToSync = new Vector<>();
        mIntentListForFurtherFolderSync = new Vector<>();
        mForgottenLocalFiles = new HashMap<>();
        mCancellationRequested = new AtomicBoolean(false);
        mPushOnly = pushOnly;
        mSyncFullAccount = syncFullAccount;
        mSyncContentOfRegularFiles = syncContentOfRegularFiles;
    }


    public int getConflictsFound() {
        return mConflictsFound;
    }

    public int getFailsInFileSyncsFound() {
        return mFailsInFileSyncsFound;
    }

    public Map<String, String> getForgottenLocalFiles() {
        return mForgottenLocalFiles;
    }



    /**
     * Returns the list of subfolders after the refresh, in a {@link Pair} with a boolean
     * indicating if was detected as changed in the server or not.
     *
     * @return  List of pairs of subfolders and boolean flags set to 'true' if there are pending
     *          changes in the server side.
     */
    public List<Pair<OCFile, Boolean>> getFoldersToVisit() {
        return mFoldersToVisit;
    }

    /**
     * Performs the synchronization.
     *
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        mFailsInFileSyncsFound = 0;
        mConflictsFound = 0;
        mForgottenLocalFiles.clear();

        try {
            // get locally cached information about folder
            mLocalFolder = getStorageManager().getFileByPath(mRemotePath);

            if (mPushOnly) {
                // assuming there is no update in the server side, still need to handle local changes
                Log_OC.i(TAG, "Push only sync of " + mAccount.name + mRemotePath);
                preparePushOfLocalChanges();
                syncContents();
                //pushOnlySync();
                result = new RemoteOperationResult(ResultCode.OK);

            } else {
                // get list of files in folder from remote server
                result = fetchRemoteFolder(client);

                if (result.isSuccess()) {
                    // success - merge updates in server with local state
                    final int result_size = result.getData().size();
                    mergeRemoteFolder((RemoteFile) result.getData().get(0),
                            result.getData().subList(1, result_size));
                    syncContents();

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
            }

        } catch (OperationCancelledException e) {
            result = new RemoteOperationResult(e);
        }

        return result;

    }


    /**
     * Get list of files in folder from remote server.
     *
     * @param client      {@link OwnCloudClient} instance used to access the server.
     * @return            Result of the fetch, including list of remote files in the sync'ed folder.
     * @throws OperationCancelledException
     */
    @NonNull
    private RemoteOperationResult fetchRemoteFolder(OwnCloudClient client) throws OperationCancelledException {
        Log_OC.d(TAG, "Fetching list of files in  " + mAccount.name + mRemotePath + ", if changed");

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        ReadRemoteFolderOperation readFolderOperation = new ReadRemoteFolderOperation(mRemotePath);
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
        return (!mLocalFolder.getTreeEtag().equals(remoteFolder.getEtag()));
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
     *  Grants that mFoldersToVisit is updated with fresh data after execution.
     *
     * @param remoteRootFolder remote folder that should be updated
     * @param remoteFiles files of that remote folder
     */
    private void mergeRemoteFolder(RemoteFile remoteRootFolder, List<Object> remoteFiles) {
        Log_OC.d(TAG, "Synchronizing " + mAccount.name + mRemotePath);

        final OCFile rootFolder = FileStorageUtils.createOCFileFrom(remoteRootFolder);
        rootFolder.copyLocalPropertiesFrom(mLocalFolder);

        Log_OC.d(TAG, "Remote folder " + mLocalFolder.getRemotePath()
                + " changed - starting update of local data ");

        final FileDataStorageManager storageManager = getStorageManager();

        final List<OCFile> updatedFiles = new ArrayList<>();
        final Map<String, OCFile> localFileListMap = storageManager.getFolderContentAsRemoteIdMap(rootFolder);
        boolean hasFoldersToExpand = false;
        for(final Object remoteFileRawObject : remoteFiles) {

            final RemoteFile remoteFile = (RemoteFile) remoteFileRawObject;
            final OCFile ocRemoteFile = FileStorageUtils.createOCFileFrom(remoteFile);
            final OCFile ocLocalFile = localFileListMap.get(remoteFile.getRemoteId());

            final OCFile updatedLocalFile = updateFileInfo(ocLocalFile, remoteFile);

            final boolean serverFileUnchanged = isServerFileUnchanged(updatedLocalFile, ocRemoteFile);

            if (updatedLocalFile.isFolder() && !serverFileUnchanged) {
                hasFoldersToExpand = true;
            }

            addToSyncContent(updatedLocalFile, ocRemoteFile);
            updatedFiles.add(updatedLocalFile);
        }

        // save updated contents in local database
        if (!hasFoldersToExpand) {
            rootFolder.setTreeEtag(rootFolder.getEtag());
        }

        storageManager.saveFolder(
                rootFolder,
                updatedFiles,
                getFilesToBeRemoved(rootFolder, updatedFiles));
    }

    /**
     * Returns the updated local file
     * @param localFile the local file that should be updated
     * @param remoteFile the remote file from which the data should be taken to update the local file
     * @return the new updated local file
     */
    private OCFile updateFileInfo(OCFile localFile, RemoteFile remoteFile) {
        final OCFile newFile = FileStorageUtils.createOCFileFrom(remoteFile);
        final OCFile ocRemoteFile = FileStorageUtils.createOCFileFrom(remoteFile);

        newFile.setLastSyncDateForProperties(mCurrentSyncTime);
        if(localFile != null) {
            newFile.copyLocalPropertiesFrom(localFile);
            newFile.setFileName(ocRemoteFile.getFileName());
            // remote eTag will not be set unless file CONTENTS are synchronized
            newFile.setEtag(localFile.getEtag());
            if (!newFile.isFolder() &&
                    ocRemoteFile.isImage() &&
                    ocRemoteFile.getModificationTimestamp() != localFile.getModificationTimestamp()) {
                newFile.setNeedsUpdateThumbnail(true);
            }
        } else {
            newFile.setParentId(mLocalFolder.getFileId());
            // remote eTag will not be set unless file CONTENTS are synchronized
            newFile.setEtag("");
            // new files need to check av-off status of parent folder!
            if (newFile.isAvailableOffline()) {
                newFile.setAvailableOfflineStatus(
                        OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT);
            }
        }

        return findAndUpdateLocalFileInDefaultPathIfLost(newFile);
    }

    private List<OCFile> getFilesToBeRemoved(OCFile localFolder,
                                             List<OCFile> updatedFiles) {
        final List<OCFile> filesInLocalFolder = getStorageManager().getFolderContent(localFolder);
        final ArrayList<String> updateFilesRemoteIds = new ArrayList<>(updatedFiles.size());
        for(OCFile updatedFile : updatedFiles) {
            updateFilesRemoteIds.add(updatedFile.getRemoteId());
        }

        final ArrayList<OCFile> filesToBeRemoved = new ArrayList<>();
        for(OCFile fileToCheck : filesInLocalFolder) {
            if(!updateFilesRemoteIds.contains(fileToCheck.getRemoteId())) {
                filesToBeRemoved.add(fileToCheck);
            }
        }
        return filesToBeRemoved;
    }

    private boolean isServerFileUnchanged(OCFile localFile, OCFile remoteFile) {
        return localFile.isFolder()
                ? (remoteFile == null)
                || localFile.getTreeEtag().equals(remoteFile.getEtag())
                : (remoteFile == null)
                || localFile.getEtag().equals(remoteFile.getEtag());
    }

    private boolean folderNeedsToBeVisited(OCFile localFile) {
        return localFile.isFolder() && mSyncFullAccount;
    }

    private boolean needToSyncFolder(OCFile localFile) {
        final boolean shouldSyncContents = (mSyncContentOfRegularFiles || localFile.isAvailableOffline());
        return localFile.isFolder() && shouldSyncContents && !mSyncFullAccount;
    }

    private boolean needToSyncFile(OCFile localFile) {
        final boolean shouldSyncContents = (mSyncContentOfRegularFiles || localFile.isAvailableOffline());
        return !localFile.isFolder() && shouldSyncContents && !isBlockedForAutomatedSync(localFile);
    }

    private SynchronizeFileOperation getFileSyncOperation(OCFile localFile, OCFile remoteFile) {
        return new SynchronizeFileOperation(
                localFile,
                remoteFile,
                mAccount,
                isServerFileUnchanged(localFile, remoteFile),
                mContext);
    }

    private Intent getSyncIntent(Context context,
                                 Account account,
                                 boolean serverUnchanged,
                                 String remotePath,
                                 boolean syncContentOfRegularFiles) {
        return new Intent(context, OperationsService.class)
                .setAction(OperationsService.ACTION_SYNC_FOLDER)
                .putExtra(OperationsService.EXTRA_ACCOUNT, account)
                .putExtra(OperationsService.EXTRA_REMOTE_PATH, remotePath)
                .putExtra(OperationsService.EXTRA_PUSH_ONLY, serverUnchanged)
                .putExtra(OperationsService.EXTRA_SYNC_REGULAR_FILES, syncContentOfRegularFiles);
    }

    private void preparePushOfLocalChanges() {
        List<OCFile> children = getStorageManager().getFolderContent(mLocalFolder);
        mFoldersToVisit = new Vector<>(children.size());
        for (OCFile child : children) {
            addToSyncContent(
                child,
                null
            );
        }
    }


    /**
     * Generates the appropriate operations to later sync the contents of localFile with the server.
     *
     * Stores the operations in mFoldersToSyncContents and mFilesToSyncContents.
     *
     * @param localFile         Local information about the file which contents might be sync'ed.
     * @param remoteFile        Server information of the file.
     * @return                  'True' when the received file was not changed in the server side from the
     *                          last synchronization.
     */
    private void addToSyncContent(OCFile localFile, OCFile remoteFile) {
        final boolean serverFileUnchanged = isServerFileUnchanged(localFile, remoteFile);

        if(folderNeedsToBeVisited(localFile)) {
            // this operation is part of a full-account synchronization driven
            // by {@link FileSyncAdapter}; subfolders will not be expanded by
            // this operation, but by {@link FileSyncAdapter}
            mFoldersToVisit.add(new Pair<>(localFile, !serverFileUnchanged));
        }

        if(needToSyncFolder(localFile)) {
            getSyncIntent(mContext,
                    mAccount,
                    serverFileUnchanged,
                    localFile.getRemotePath(),
                    mSyncContentOfRegularFiles);
        }

        if(needToSyncFile(localFile)) {
            mOperationsOfFilesToSync.add(getFileSyncOperation(localFile, remoteFile));
        } else if(needToSyncFolder(localFile)) {
            mIntentListForFurtherFolderSync.add(getSyncIntent(mContext,
                    mAccount,
                    serverFileUnchanged,
                    remoteFile.getRemotePath(),
                    mSyncContentOfRegularFiles));
        }
    }

    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed
     * or if exists conflict due to changes both in local and remote contents of the each file.
     *
     * If download or upload is needed, request the operation to the corresponding service and goes
     * on.
     */
    private void syncContents() throws OperationCancelledException {

        Log_OC.v(TAG, "Starting content synchronization... ");
        for (SyncOperation op: mOperationsOfFilesToSync) {
            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }
            final RemoteOperationResult contentsResult = op.execute(getStorageManager(), mContext);
            if (!contentsResult.isSuccess()) {
                if (contentsResult.getCode() == ResultCode.SYNC_CONFLICT) {
                    mConflictsFound++;
                } else {
                    mFailsInFileSyncsFound++;
                    if (contentsResult.getException() != null) {
                        Log_OC.e(TAG, "Error while synchronizing file : "   // Vs " av-off file"
                            + contentsResult.getLogMessage(), contentsResult.getException());
                    } else {
                        Log_OC.e(TAG, "Error while synchronizing file : "
                            + contentsResult.getLogMessage());
                    }
                }
            }   // won't let these fails break the synchronization process
        }
        for (Intent intent: mIntentListForFurtherFolderSync) {
            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }
            mContext.startService(intent);
        }
    }

    /**
     /**
     * Scans the default location for saving local copies of files searching for
     * a 'lost' file with the same full name as the {@link OCFile} received as
     * parameter.
     *
     * This method helps to keep linked local copies of the files when the app is uninstalled, and then
     * reinstalled in the device. OR after the cache of the app was deleted in system settings.
     *
     * The method is assuming that all the local changes in the file where synchronized in the past. This is dangerous,
     * but assuming the contrary could lead to massive unnecessary synchronizations of downloaded file after deleting
     * the app cache.
     *
     * This should be changed in the near future to avoid any chance of data loss, but we need to add some options
     * to limit hard automatic synchronizations to wifi, unless the user wants otherwise.
     *
     * @param file      File to associate a possible 'lost' local file.
     */
    private OCFile findAndUpdateLocalFileInDefaultPathIfLost(final OCFile file) {
        if (file.getStoragePath() == null && !file.isFolder()) {
            final File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
        return file;
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

    /**
     * Checks the last upload of a file and determines if automated synchronization needs to wait for
     * user action or not.
     *
     * @param file      ownCloud file to check.
     * @return          'True' if the received file should not be automatically sync'ed due to a previous
     *                  upload error that requires an user action.
     */
    private boolean isBlockedForAutomatedSync(OCFile file) {
        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(mContext.getContentResolver());
        OCUpload failedUpload = uploadsStorageManager.getLastUploadFor(file, mAccount.name);
        if (failedUpload != null) {
            switch (failedUpload.getLastResult()) {
                case CREDENTIAL_ERROR:
                case FOLDER_ERROR:
                case FILE_NOT_FOUND:
                case FILE_ERROR:
                case PRIVILEDGES_ERROR:
                case CONFLICT_ERROR:
                    return true;
            }
        }
        return false;
    }


    public String getRemotePath() {
        return mRemotePath;
    }

}
