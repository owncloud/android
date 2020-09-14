/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author masensio
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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.domain.files.model.MimeTypeConstantsKt;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;
import timber.log.Timber;

import java.io.File;

/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server.
 * Save the new folder in Database
 */
public class CreateFolderOperation extends SyncOperation {

    protected String mRemotePath;
    protected boolean mCreateFullPath;

    /**
     * Constructor
     *
     * @param createFullPath        'True' means that all the ancestor folders should be created
     *                              if don't exist yet.
     */
    public CreateFolderOperation(String remotePath, boolean createFullPath) {
        mRemotePath = remotePath;
        mCreateFullPath = createFullPath;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateRemoteFolderOperation createRemoteFolderOperation = new CreateRemoteFolderOperation(
                mRemotePath,
                mCreateFullPath,
                false
        );
        RemoteOperationResult result = createRemoteFolderOperation.execute(client);

        if (result.isSuccess()) {
            OCFile newDir = saveFolderInDB();
            String localPath = FileStorageUtils.getDefaultSavePathFor(
                    getStorageManager().getAccount().name, newDir
            );
            File localFile = new File(localPath);
            boolean created = localFile.mkdirs();
            if (!created) {
                Timber.w("Local folder " + localPath + " was not fully created");
            }
        } else {
            Timber.e("%s hasn't been created", mRemotePath);
        }

        return result;
    }

    /**
     * Save new directory in local database
     *
     * @return Instance of {@link OCFile} just created
     */
    private OCFile saveFolderInDB() {
        OCFile newDir = null;
        if (mCreateFullPath && getStorageManager().
                getFileByPath(FileStorageUtils.getParentPath(mRemotePath)) == null) {// When parent
            // of remote path
            // is not created
            String[] subFolders = mRemotePath.split("/");
            String composedRemotePath = "/";

            // Create all the ancestors
            for (String subFolder : subFolders) {
                if (!subFolder.isEmpty()) {
                    composedRemotePath = composedRemotePath + subFolder + "/";
                    mRemotePath = composedRemotePath;
                    newDir = saveFolderInDB();
                }
            }
        } else { // Create directory on DB
            newDir = new OCFile(mRemotePath);
            newDir.setMimetype(MimeTypeConstantsKt.MIME_DIR);
            long parentId = getStorageManager().
                    getFileByPath(FileStorageUtils.getParentPath(mRemotePath)).getFileId();
            newDir.setParentId(parentId);
            newDir.setModificationTimestamp(System.currentTimeMillis());
            getStorageManager().saveFile(newDir);

            Timber.d("Create directory " + mRemotePath + " in Database");
        }
        return newDir;
    }
}
