/**
 *   ownCloud Android client application
 *
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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.chunks.CreateRemoteChunkFolderOperation;

public class CreateChunksFolderOperation extends CreateFolderOperation {

    private static final String TAG = CreateChunksFolderOperation.class.getSimpleName();

    /**
     * Constructor
     *
     * @param remotePath         Path in which create the chunks folder in server
     */
    public CreateChunksFolderOperation(String remotePath) {
        super(remotePath, false);
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateRemoteChunkFolderOperation createRemoteChunkFolderOperation = new CreateRemoteChunkFolderOperation(
                mRemotePath,
                mCreateFullPath
        );

        RemoteOperationResult result =  createRemoteChunkFolderOperation.execute(client);

        if (result.isSuccess()) {
            Log_OC.w(TAG, "Remote chunks folder " + mRemotePath + " was created");
        } else {
            Log_OC.e(TAG, mRemotePath + " hasn't been created");
        }

        return result;
    }
}