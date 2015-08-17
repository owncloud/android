/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import android.accounts.Account;
import android.content.Context;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.CopyRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Operation copying an {@link OCFile} to a different folder.
 *
 * @author David A. Velasco
 */
public class CopyFileOperation extends SyncOperation {

    //private static final String TAG = MoveFileOperation.class.getSimpleName();

    private String mSrcPath;
    private String mTargetParentPath;

    private OCFile mFile;
    private final Context mContext;


    /**
     * Constructor
     *
     * @param context          Context of the application.
     * @param srcPath          Remote path of the {@link OCFile} to move.
     * @param targetParentPath Path to the folder where the file will be copied into.
     * @param account          OwnCloud account containing both the file and the target folder
     */
    public CopyFileOperation(Context context, String srcPath, String targetParentPath, Account account) {
        mContext = context;
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
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;

        /// 0. Check if server is reachable. Otherwise, the storage manager won't be able to find
        // the remote file and will confound network errors with path-related ones
        if (!new GetServerInfoOperation(getClient().getBaseUri().toString(),
                mContext).run
                (getClient()).isSuccess()) {
            return new RemoteOperationResult(ResultCode.HOST_NOT_AVAILABLE);
        }

        /// 1. check copy validity
        {
            if (mTargetParentPath.startsWith(mSrcPath)) {
                return new RemoteOperationResult(ResultCode.INVALID_COPY_INTO_DESCENDANT);
            }
        }
        mFile = getStorageManager().getFileByPath(mSrcPath);
        if (mFile == null) {
            return new RemoteOperationResult(ResultCode.FILE_NOT_FOUND);
        }

        /// 2. remote copy
        String targetPath = mTargetParentPath + mFile.getFileName();
        if (mFile.isFolder()) {
            targetPath += OCFile.PATH_SEPARATOR;
        }
        CopyRemoteFileOperation operation = new CopyRemoteFileOperation(
                mContext,
                mSrcPath,
                targetPath,
                false
        );
        result = operation.execute(client);

        /// 3. local copy
        if (result.isSuccess()) {
            getStorageManager().copyLocalFile(mFile, targetPath);
        }
        // TODO Implement and handle ResultCode.PARTIAL_COPY_DONE in client Activity, for the moment

        return result;
    }


}
