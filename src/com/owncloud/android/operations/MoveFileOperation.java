/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.MoveRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.services.observer.FileObserverService;
import com.owncloud.android.utils.FileStorageUtils;


/**
 * Operation moving an {@link OCFile} to a different folder.
 */
public class MoveFileOperation extends SyncOperation {
    
    //private static final String TAG = MoveFileOperation.class.getSimpleName();
    
    protected String mSrcPath;
    protected String mTargetParentPath;
    protected OCFile mFile;
    
    /**
     * Constructor
     * 
     * @param srcPath           Remote path of the {@link OCFile} to move.
     * @param targetParentPath  Path to the folder where the file will be moved into.
     */
    public MoveFileOperation(String srcPath, String targetParentPath) {
        mSrcPath = srcPath;
        mTargetParentPath = targetParentPath;
        if (!mTargetParentPath.endsWith(OCFile.PATH_SEPARATOR)) {
            mTargetParentPath += OCFile.PATH_SEPARATOR;
        }
        
        mFile = null;
    }
  
    /**
     * Performs the operation.
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        
        /// 1. check move validity
        if (mTargetParentPath.startsWith(mSrcPath)) {
            return new RemoteOperationResult(ResultCode.INVALID_MOVE_INTO_DESCENDANT);
        }
        mFile = getStorageManager().getFileByPath(mSrcPath);
        if (mFile == null) {
            return new RemoteOperationResult(ResultCode.FILE_NOT_FOUND);
        }
        
        /// 2. remote move
        String targetPath = mTargetParentPath + mFile.getFileName();
        if (mFile.isFolder()) {
            targetPath += OCFile.PATH_SEPARATOR;
        }
        MoveRemoteFileOperation operation = new MoveRemoteFileOperation(
                mSrcPath, 
                targetPath, 
                false
        );
        result = operation.execute(client);
        
        /// 3. local move
        if (result.isSuccess()) {
            // stop observing changes if available offline
            boolean isAvailableOffline = mFile.getAvailableOfflineStatus().equals(
                OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE
            );
                // OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT requires no action
            if (isAvailableOffline) {
                stopObservation();
            }

            getStorageManager().moveLocalFile(mFile, targetPath, mTargetParentPath);

            // adjust available offline status after move resume observation of file after rename
            OCFile updatedFile = getStorageManager().getFileById(mFile.getFileId());
            OCFile.AvailableOfflineStatus updatedAvOffStatus = updatedFile.getAvailableOfflineStatus();

            if (updatedAvOffStatus == OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE) {
                resumeObservation(targetPath);

            } else if (updatedAvOffStatus == OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT) {
                // enforce ancestor to rescan subfolders for immediate observation
                OCFile ancestor = getStorageManager().getAvailableOfflineAncestorOf(updatedFile);
                FileObserverService.observeFile(
                    MainApp.getAppContext(),
                    ancestor,
                    getStorageManager().getAccount(),
                    true
                );
            }

        }
        // TODO handle ResultCode.PARTIAL_MOVE_DONE in client Activity, for the moment
        
        return result;
    }

    private void stopObservation() {
        FileObserverService.observeFile(
            MainApp.getAppContext(),
            mFile,
            getStorageManager().getAccount(),
            false
        );
    }

    private void resumeObservation(String targetPath) {
        OCFile updatedFile = new OCFile(targetPath);
        updatedFile.setMimetype(mFile.getMimetype());
        updatedFile.setFileId(mFile.getFileId());
        updatedFile.setStoragePath(
            FileStorageUtils.getDefaultSavePathFor(
                getStorageManager().getAccount().name,
                updatedFile
            )
        );
        FileObserverService.observeFile(
            MainApp.getAppContext(),
            updatedFile,
            getStorageManager().getAccount(),
            true
        );
    }
}