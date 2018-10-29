/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.ArrayList;

/**
 * Removes an existing OCShare, known its LOCAL share id.
 */
public class RemoveShareOperation extends SyncOperation {

    private static final String TAG = RemoveShareOperation.class.getSimpleName();

    private long mLocalId;


    public RemoveShareOperation(long localId) {
        mLocalId = localId;
    }

    @Override
    protected RemoteOperationResult<ShareParserResult> run(OwnCloudClient client) {
        RemoteOperationResult result;
        
        // Get OCShare from local storage
        OCShare share = getStorageManager().getShareById(mLocalId);

        if (share != null) {
            // Delete remote share
            RemoveRemoteShareOperation operation =
                    new RemoveRemoteShareOperation((int) share.getRemoteId());
            result = operation.execute(client);

            // Update local storage
            OCFile file = getStorageManager().getFileByPath(share.getPath());
            if (result.isSuccess()) {
                Log_OC.d(TAG, "Share id = " + share.getRemoteId() + " deleted");

                ShareType shareType = share.getShareType();
                if (ShareType.PUBLIC_LINK.equals(shareType)) {

                    // Check if it is the last public share
                    ArrayList<OCShare> publicShares = getStorageManager().
                            getPublicSharesForAFile(share.getPath(),
                                    getStorageManager().getAccount().name);

                    if (publicShares.size() == 1) {
                        file.setSharedViaLink(false);
                    }

                } else if (ShareType.USER.equals(shareType) || ShareType.GROUP.equals(shareType)
                    || ShareType.FEDERATED.equals(shareType)){

                    // Check if it is the last private share
                    ArrayList <OCShare> sharesWith = getStorageManager().
                        getPrivateSharesForAFile(share.getPath(),
                            getStorageManager().getAccount().name);

                    if (sharesWith.size() == 1) {
                        file.setSharedWithSharee(false);
                    }
                }

                getStorageManager().saveFile(file);
                getStorageManager().removeShare(share);

            } else if (result.getCode() != ResultCode.SERVICE_UNAVAILABLE &&
                    notExistFile(client, share.getPath())) {
                // unshare failed because file was deleted before
                getStorageManager().removeFile(file, true, true);
            }

        } else {
            result = new RemoteOperationResult<>(ResultCode.SHARE_NOT_FOUND);
        }

        return result;
    }
    
    private boolean notExistFile(OwnCloudClient client, String remotePath){
        ExistenceCheckRemoteOperation existsOperation =
                new ExistenceCheckRemoteOperation(remotePath, true);
        RemoteOperationResult result = existsOperation.execute(client);
        return result.isSuccess();
    }

}
