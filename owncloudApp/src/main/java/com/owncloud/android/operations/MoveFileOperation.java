/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.MoveRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.RemoteFileUtils;

import java.io.File;

/**
 * Operation moving an {@link OCFile} to a different folder.
 */
public class MoveFileOperation extends SyncOperation {

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
        if (!mTargetParentPath.endsWith(File.separator)) {
            mTargetParentPath += File.separator;
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
            return new RemoteOperationResult<>(ResultCode.INVALID_MOVE_INTO_DESCENDANT);
        }
        mFile = getStorageManager().getFileByPath(mSrcPath);
        if (mFile == null) {
            return new RemoteOperationResult<>(ResultCode.FILE_NOT_FOUND);
        }

        /// 2. remote move
        String targetRemotePath = mTargetParentPath + mFile.getFileName();
        // Check if target remote path already exists on server or add suffix (2), (3) ... otherwise
        String finalRemotePath = RemoteFileUtils.Companion.getAvailableRemotePath(client, targetRemotePath);
        if (mFile.isFolder()) {
            finalRemotePath += File.separator;
        }
        MoveRemoteFileOperation operation = new MoveRemoteFileOperation(
                mSrcPath,
                finalRemotePath,
                false
        );
        result = operation.execute(client);

        /// 3. local move
        if (result.isSuccess()) {
            getStorageManager().moveLocalFile(mFile, finalRemotePath, mTargetParentPath);

            // adjust available offline status after move resume observation of file after rename
            OCFile updatedFile = getStorageManager().getFileById(mFile.getId());
            // FIXME: 13/10/2020 : New_arch: Av.Offline and Move
//            OCFile.AvailableOfflineStatus updatedAvOffStatus = updatedFile.getAvailableOfflineStatus();
        }
        // TODO handle ResultCode.PARTIAL_MOVE_DONE in client Activity, for the moment

        return result;
    }
}
