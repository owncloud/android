/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2018 ownCloud GmbH.
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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareParserResult;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

/**
 * Creates a new public share for a given file
 */
public class CreateShareViaLinkOperation extends SyncOperation<ShareParserResult> {

    private String mPath;
    private String mName;
    private String mPassword;
    private long mExpirationDateInMillis;
    private Boolean mPublicUpload;
    private int mPermissions;

    /**
     * Constructor
     *
     * @param path Full path of the file/folder being shared. Mandatory argument
     *             Only available for public link shares
     */
    public CreateShareViaLinkOperation(
            String path
    ) {

        mPath = path;
        mName = null;
        mPassword = null;
        mExpirationDateInMillis = 0;
        mPublicUpload = null;
        mPermissions = OCShare.DEFAULT_PERMISSION;
    }

    /**
     * Set name to update in public link.
     *
     * @param name Name to set to the public link.
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Set password to update in public link.
     *
     * @param password Password to set to the public link.
     */
    public void setPassword(String password) {
        this.mPassword = password;
    }

    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis    Expiration date to set to the public link.
     */
    public void setExpirationDateInMillis(long expirationDateInMillis) {
        this.mExpirationDateInMillis = expirationDateInMillis;
    }

    /**
     * Enable upload permissions to update in Share resource.
     * @param publicUpload Upload Permission to set to the public link.
     */
    public void setPublicUpload(Boolean publicUpload) {
        this.mPublicUpload = publicUpload;
    }

    /**
     * Set permissions to allow or not specific actions in the share
     *
     * @param permissions permissions to set to the public link.
     */
    public void setPermissions (int permissions) {
        this.mPermissions = permissions;
    }

    public String getName() {
        return mName;
    }

    public Boolean getPublicUpload() {
        return mPublicUpload;
    }

    public int getPermissions() {
        return mPermissions;
    }

    public long getExpirationDateInMillis() {
        return mExpirationDateInMillis;
    }

    public String getPath() {
        return mPath;
    }

    public String getPassword() {
        return mPassword;
    }

    @Override
    protected RemoteOperationResult<ShareParserResult> run(OwnCloudClient client) {
        CreateRemoteShareOperation createOp = new CreateRemoteShareOperation(
                mPath,
                ShareType.PUBLIC_LINK,
                "",
                mPublicUpload,
                mPassword,
                mPermissions
        );
        createOp.setGetShareDetails(true);
        createOp.setName(mName);
        createOp.setPublicUpload(mPublicUpload);
        createOp.setPermissions(mPermissions);
        createOp.setExpirationDate(mExpirationDateInMillis);
        RemoteOperationResult<ShareParserResult> result = createOp.execute(client);

        if (result.isSuccess()) {
            if (result.getData().getShares().size() > 0) {
                ShareParserResult shareParserResult = result.getData();
                if(shareParserResult.getShares().size() != 0) {
                    updateData(shareParserResult.getShares().get(0));
                } else {
                    result = new RemoteOperationResult<>(
                            RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
                    result.setData(shareParserResult);
                }
            } else {
                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
            }
        }

        return result;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(mPath);
        if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setIsFolder(true);
        } else {
            share.setIsFolder(false);
        }

        getStorageManager().saveShare(share);

        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(mPath);
        if (file != null) {
            file.setSharedViaLink(true);
            getStorageManager().saveFile(file);
        }
    }
}