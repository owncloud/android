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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils;
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import timber.log.Timber;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Operation to check the existence or absence of a path in a remote server.
 *
 * @author David A. Velasco
 * @author David González Verdugo
 */
public class ExistenceCheckRemoteOperation extends RemoteOperation {

    /**
     * Maximum time to wait for a response from the server in MILLISECONDs.
     */
    public static final int TIMEOUT = 10000;

    private String mPath;
    private boolean mSuccessIfAbsent;
    private boolean mIsLogin;

    /**
     * Sequence of redirections followed. Available only after executing the operation
     */
    private RedirectionPath mRedirectionPath = null;

    /**
     * Full constructor. Success of the operation will depend upon the value of successIfAbsent.
     *
     * @param remotePath      Path to append to the URL owned by the client instance.
     * @param successIfAbsent When 'true', the operation finishes in success if the path does
     *                        NOT exist in the remote server (HTTP 404).
     * @param isLogin         When `true`, the username won't be added at the end of the PROPFIND url since is not
     *                        needed to check user credentials
     */
    public ExistenceCheckRemoteOperation(String remotePath, boolean successIfAbsent, boolean isLogin) {
        mPath = (remotePath != null) ? remotePath : "";
        mSuccessIfAbsent = successIfAbsent;
        mIsLogin = isLogin;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        boolean previousFollowRedirects = client.followRedirects();

        try {
            String stringUrl = mIsLogin ?
                    client.getBaseFilesWebDavUri().toString() :
                    client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mPath);
            PropfindMethod propfindMethod = new PropfindMethod(
                    new URL(stringUrl),
                    0,
                    DavUtils.getAllPropset()
            );
            propfindMethod.setReadTimeout(TIMEOUT, TimeUnit.SECONDS);
            propfindMethod.setConnectionTimeout(TIMEOUT, TimeUnit.SECONDS);

            client.setFollowRedirects(false);
            int status = client.executeHttpMethod(propfindMethod);

            if (previousFollowRedirects) {
                mRedirectionPath = client.followRedirection(propfindMethod);
                status = mRedirectionPath.getLastStatus();
            }

            /*
             *  PROPFIND method
             *  404 NOT FOUND: path doesn't exist,
             *  207 MULTI_STATUS: path exists.
             */

            Timber.d("Existence check for " + stringUrl + WebdavUtils.encodePath(mPath) +
                    " targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") +
                    "finished with HTTP status " + status + (!isSuccess(status) ? "(FAIL)" : ""));

            return isSuccess(status)
                    ? new RemoteOperationResult<>(OK)
                    : new RemoteOperationResult<>(propfindMethod);

        } catch (Exception e) {
            final RemoteOperationResult result = new RemoteOperationResult<>(e);
            Timber.e(result.getException(),
                    "Existence check for " + client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mPath) + " " +
                            "targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") + ": " + result.getLogMessage());
            return result;
        } finally {
            client.setFollowRedirects(previousFollowRedirects);
        }
    }

    /**
     * Gets the sequence of redirections followed during the execution of the operation.
     *
     * @return Sequence of redirections followed, if any, or NULL if the operation was not executed.
     */
    public RedirectionPath getRedirectionPath() {
        return mRedirectionPath;
    }

    /**
     * @return 'True' if the operation was executed and at least one redirection was followed.
     */
    public boolean wasRedirected() {
        return (mRedirectionPath != null && mRedirectionPath.getRedirectionsCount() > 0);
    }

    private boolean isSuccess(int status) {
        return ((status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS) && !mSuccessIfAbsent)
                || (status == HttpConstants.HTTP_MULTI_STATUS && !mSuccessIfAbsent)
                || (status == HttpConstants.HTTP_NOT_FOUND && mSuccessIfAbsent);
    }
}