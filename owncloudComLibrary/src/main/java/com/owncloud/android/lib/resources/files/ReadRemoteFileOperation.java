/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
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
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils;
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import timber.log.Timber;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.owncloud.android.lib.common.http.methods.webdav.DavConstants.DEPTH_0;
import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Remote operation performing the read a file from the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 */

public class ReadRemoteFileOperation extends RemoteOperation<RemoteFile> {

    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;

    private String mRemotePath;

    /**
     * Constructor
     *
     * @param remotePath Remote path of the file.
     */
    public ReadRemoteFileOperation(String remotePath) {
        mRemotePath = remotePath;
    }

    /**
     * Performs the read operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult<RemoteFile> run(OwnCloudClient client) {
        PropfindMethod propfind;
        RemoteOperationResult<RemoteFile> result;

        /// take the duty of check the server for the current state of the file there
        try {
            // remote request
            propfind = new PropfindMethod(client,
                    new URL(client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mRemotePath)),
                    DEPTH_0,
                    DavUtils.getAllPropset());

            propfind.setReadTimeout(SYNC_READ_TIMEOUT, TimeUnit.SECONDS);
            propfind.setConnectionTimeout(SYNC_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
            final int status = client.executeHttpMethod(propfind);

            if (status == HttpConstants.HTTP_MULTI_STATUS
                    || status == HttpConstants.HTTP_OK) {

                final RemoteFile file = new RemoteFile(propfind.getRoot(), AccountUtils.getUserId(mAccount, mContext));

                result = new RemoteOperationResult<>(OK);
                result.setData(file);

            } else {
                result = new RemoteOperationResult<>(propfind);
                client.exhaustResponse(propfind.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Timber.e(e, "Synchronizing  file %s", mRemotePath);
        }

        return result;
    }
}