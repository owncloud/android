/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.GetRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.RemoteShare;
import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.shares.db.OCShare;

/**
 * Updates an existing private share for a given file
 */

public class UpdateSharePermissionsOperation extends SyncOperation<ShareParserResult> {

    private long mShareId;
    private int mPermissions;
    private String mPath;

    /**
     * Constructor
     *
     * @param shareId       Private {@link RemoteShare} to update. Mandatory argument
     */
    public UpdateSharePermissionsOperation(long shareId) {
        mShareId = shareId;
        mPermissions = -1;
    }

    /**
     * Set permissions to update in private share.
     *
     * @param permissions   Permissions to set to the private share.
     *                      Values <= 0 result in no update applied to the permissions.
     */
    public void setPermissions(int permissions) {
        mPermissions = permissions;
    }

    @Override
    protected RemoteOperationResult<ShareParserResult> run(OwnCloudClient client) {

        OCShare share = getStorageManager().getShareById(mShareId); // ShareType.USER | ShareType.GROUP

        if (share == null) {
            // TODO try to get remote share before failing?
            return new RemoteOperationResult<>(
                    RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
        }

        mPath = share.getPath();

        // Update remote share with password
        UpdateRemoteShareOperation updateOp = new UpdateRemoteShareOperation(
                share.getRemoteId()
        );
        updateOp.setPermissions(mPermissions);
        RemoteOperationResult<ShareParserResult> result = updateOp.execute(client);

        if (result.isSuccess()) {
            GetRemoteShareOperation getShareOp = new GetRemoteShareOperation(share.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                RemoteShare remoteShare = result.getData().getShares().get(0);
                // TODO check permissions are being saved
                updateData(remoteShare);
            }
        }

        return result;
    }

    public String getPath() {
        return mPath;
    }

    private void updateData(RemoteShare share) {
        // Update DB with the response
        share.setPath(mPath);   // TODO - check if may be moved to UpdateRemoteShareOperation
        if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setFolder(true);
        } else {
            share.setFolder(false);
        }
        getStorageManager().saveShare(share);
    }

}

