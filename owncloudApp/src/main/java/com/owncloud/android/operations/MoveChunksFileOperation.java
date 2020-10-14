/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.chunks.MoveRemoteChunksFileOperation;

/**
 * Operation moving a {@link OCFile} to its final destination after being upload in chunks
 */
public class MoveChunksFileOperation extends MoveFileOperation {

    private String mFileLastModifTimestamp;
    private long mFileLength;

    /**
     * Constructor
     *
     * @param srcPath                Remote path of the {@link OCFile} to move.
     * @param targetParentPath       Path to the folder where the file will be moved into.
     * @param fileLastModifTimestamp Timestamp of last modification of file to move.
     * @param fileLength             Total length of the file to move.
     */
    public MoveChunksFileOperation(String srcPath, String targetParentPath, String fileLastModifTimestamp,
                                   long fileLength) {
        super(srcPath, targetParentPath);
        mFileLastModifTimestamp = fileLastModifTimestamp;
        mFileLength = fileLength;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        MoveRemoteChunksFileOperation operation = new MoveRemoteChunksFileOperation(
                mSrcPath,
                mTargetParentPath,
                false,
                mFileLastModifTimestamp,
                mFileLength
        );

        return operation.execute(client);
    }
}