/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;

/**
 * Get files contained in a specific folder
 */

public class GetFolderFilesOperation extends RemoteOperation {

    private static final String TAG = GetFolderFilesOperation.class.getSimpleName();

    private String mRemotePath;

    /**
     * Constructor
     *
     * @param remotePath remote path of the folder to get the files
     */
    public GetFolderFilesOperation(String remotePath) {
        mRemotePath = remotePath;
    }

    public String getRemotePath() {
        return mRemotePath;
    }

    /**
     * Performs the operation.
     *
     * Target user account is implicit in 'client'.
     *
     * @return
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        ReadRemoteFolderOperation readRemoteFolder = new ReadRemoteFolderOperation(mRemotePath);
        RemoteOperationResult result = readRemoteFolder.execute(client);

        return result;
    }
}