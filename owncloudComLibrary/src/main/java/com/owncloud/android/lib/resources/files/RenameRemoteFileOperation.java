/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2019 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.files;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.MoveMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import timber.log.Timber;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Remote operation performing the rename of a remote file or folder in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 */
public class RenameRemoteFileOperation extends RemoteOperation {

    private static final int RENAME_READ_TIMEOUT = 600000;
    private static final int RENAME_CONNECTION_TIMEOUT = 5000;

    private String mOldName;
    private String mOldRemotePath;
    private String mNewName;
    private String mNewRemotePath;

    /**
     * Constructor
     *
     * @param oldName       Old name of the file.
     * @param oldRemotePath Old remote path of the file.
     * @param newName       New name to set as the name of file.
     * @param isFolder      'true' for folder and 'false' for files
     */
    public RenameRemoteFileOperation(String oldName, String oldRemotePath, String newName,
                                     boolean isFolder) {
        mOldName = oldName;
        mOldRemotePath = oldRemotePath;
        mNewName = newName;

        String parent = (new File(mOldRemotePath)).getParent();
        parent = (parent.endsWith(File.separator)) ? parent : parent + File.separator;
        mNewRemotePath = parent + mNewName;
        if (isFolder) {
            mNewRemotePath += File.separator;
        }
    }

    /**
     * Performs the rename operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        try {
            if (mNewName.equals(mOldName)) {
                return new RemoteOperationResult<>(ResultCode.OK);
            }

            if (targetPathIsUsed(client)) {
                return new RemoteOperationResult<>(ResultCode.INVALID_OVERWRITE);
            }

            final MoveMethod move = new MoveMethod(
                    new URL(client.getUserFilesWebDavUri() +
                    WebdavUtils.encodePath(mOldRemotePath)),
                    client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mNewRemotePath), false);

            move.setReadTimeout(RENAME_READ_TIMEOUT, TimeUnit.SECONDS);
            move.setConnectionTimeout(RENAME_READ_TIMEOUT, TimeUnit.SECONDS);

            final int status = client.executeHttpMethod(move);
            final RemoteOperationResult result =
                    (status == HttpConstants.HTTP_CREATED || status == HttpConstants.HTTP_NO_CONTENT)
                            ? new RemoteOperationResult<>(ResultCode.OK)
                            : new RemoteOperationResult<>(move);

            Timber.i("Rename " + mOldRemotePath + " to " + mNewRemotePath + ": " + result.getLogMessage());
            client.exhaustResponse(move.getResponseBodyAsStream());
            return result;
        } catch (Exception e) {
            final RemoteOperationResult result = new RemoteOperationResult<>(e);
            Timber.e(e,
                    "Rename " + mOldRemotePath + " to " + ((mNewRemotePath == null) ? mNewName : mNewRemotePath) + ":" +
                            " " + result.getLogMessage());
            return result;
        }
    }

    /**
     * Checks if a file with the new name already exists.
     *
     * @return 'True' if the target path is already used by an existing file.
     */
    private boolean targetPathIsUsed(OwnCloudClient client) {
        CheckPathExistenceRemoteOperation checkPathExistenceRemoteOperation =
                new CheckPathExistenceRemoteOperation(mNewRemotePath, false);
        RemoteOperationResult exists = checkPathExistenceRemoteOperation.execute(client);
        return exists.isSuccess();
    }
}