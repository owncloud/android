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

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.MoveRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.services.observer.FileObserverService;
import com.owncloud.android.utils.FileStorageUtils;

import android.accounts.Account;


/**
 * Operation mmoving an {@link OCFile} to a different folder.
 */
public class MoveFileOperation extends SyncOperation {
    
    //private static final String TAG = MoveFileOperation.class.getSimpleName();
    
    private String mSrcPath;
    private String mTargetParentPath;
    
    private OCFile mFile;

    
    
    /**
     * Constructor
     * 
     * @param srcPath           Remote path of the {@link OCFile} to move.
     * @param targetParentPath  Path to the folder where the file will be moved into.
     * @param account           OwnCloud account containing both the file and the target folder 
     */
    public MoveFileOperation(String srcPath, String targetParentPath, Account account) {
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
        RemoteOperationResult result = null;
        
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
                pauseObservation();
            }

            getStorageManager().moveLocalFile(mFile, targetPath, mTargetParentPath);

            // resume observation of file after rename
            if (isAvailableOffline) {
                resumeObservation(targetPath);
            }
        }
        // TODO handle ResultCode.PARTIAL_MOVE_DONE in client Activity, for the moment
        
        return result;
    }


    private void pauseObservation() {
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
