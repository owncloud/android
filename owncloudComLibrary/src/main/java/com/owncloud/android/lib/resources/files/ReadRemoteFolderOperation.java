/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

import at.bitfire.dav4jvm.Response;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.DavConstants;
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils;
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import timber.log.Timber;

import java.net.URL;
import java.util.ArrayList;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Remote operation performing the read of remote file or folder in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 */

public class ReadRemoteFolderOperation extends RemoteOperation<ArrayList<RemoteFile>> {

    private String mRemotePath;

    /**
     * Constructor
     *
     * @param remotePath Remote path of the file.
     */
    public ReadRemoteFolderOperation(String remotePath) {
        mRemotePath = remotePath;
    }

    /**
     * Performs the read operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult<ArrayList<RemoteFile>> run(OwnCloudClient client) {
        RemoteOperationResult<ArrayList<RemoteFile>> result = null;

        try {
            PropfindMethod propfindMethod = new PropfindMethod(
                    new URL(client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mRemotePath)),
                    DavConstants.DEPTH_1,
                    DavUtils.getAllPropset());

            client.setFollowRedirects(true);

            int status = client.executeHttpMethod(propfindMethod);

            if (isSuccess(status)) {
                ArrayList<RemoteFile> mFolderAndFiles = new ArrayList<>();

                // parse data from remote folder
                mFolderAndFiles.add(
                        new RemoteFile(propfindMethod.getRoot(), AccountUtils.getUserId(mAccount, mContext))
                );

                // loop to update every child
                for (Response resource : propfindMethod.getMembers()) {
                    RemoteFile file = new RemoteFile(resource, AccountUtils.getUserId(mAccount, mContext));
                    mFolderAndFiles.add(file);
                }

                // Result of the operation
                result = new RemoteOperationResult<>(OK);
                result.setData(mFolderAndFiles);

            } else { // synchronization failed
                result = new RemoteOperationResult<>(propfindMethod);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
        } finally {
            if (result == null) {
                Timber.e("Synchronized " + mRemotePath + ": result is null");
            } else if (result.isSuccess()) {
                Timber.i("Synchronized " + mRemotePath + ": " + result.getLogMessage());
            } else {
                if (result.isException()) {
                    Timber.e(result.getException(), "Synchronized " + mRemotePath + ": " + result.getLogMessage());
                } else {
                    Timber.e("Synchronized " + mRemotePath + ": " + result.getLogMessage());
                }
            }
        }
        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpConstants.HTTP_MULTI_STATUS || status == HttpConstants.HTTP_OK;
    }
}
