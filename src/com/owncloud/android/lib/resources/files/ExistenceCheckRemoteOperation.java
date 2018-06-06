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
import com.owncloud.android.lib.common.http.webdav.PropfindMethod;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;

import okhttp3.HttpUrl;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Operation to check the existence or absence of a path in a remote server.
 * 
 * @author David A. Velasco
 */
public class ExistenceCheckRemoteOperation extends RemoteOperation {
    
    /** Maximum time to wait for a response from the server in MILLISECONDs.  */
    public static final int TIMEOUT = 10000;
    
    private static final String TAG = ExistenceCheckRemoteOperation.class.getSimpleName();

    private static final int FORBIDDEN_ERROR = 403;
    private static final int SERVICE_UNAVAILABLE_ERROR = 503;
    
    private String mPath;
    private boolean mSuccessIfAbsent;

    /** Sequence of redirections followed. Available only after executing the operation */
    private RedirectionPath mRedirectionPath = null;
    /**
     * Full constructor. Success of the operation will depend upon the value of successIfAbsent.
     *
     * @param remotePath        Path to append to the URL owned by the client instance.
     * @param successIfAbsent   When 'true', the operation finishes in success if the path does
     *                          NOT exist in the remote server (HTTP 404).
     */
    public ExistenceCheckRemoteOperation(String remotePath, boolean successIfAbsent) {
        mPath = (remotePath != null) ? remotePath : "";
        mSuccessIfAbsent = successIfAbsent;
    }

    @Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;

        // TODO Complete redirection stuff, although OkHttp should follow redirections by default
//        boolean previousFollowRedirects = client.getFollowRedirects();

        try {

//            client.setFollowRedirects(false);

            PropfindMethod propfindMethod = new PropfindMethod(
                    client.getOkHttpClient(),
                    HttpUrl.parse(client.getNewWebDavUri() + WebdavUtils.encodePath(mPath)),
                    0);

            int status = client.executeHttpMethod(propfindMethod);

//            if (previousFollowRedirects) {
//                mRedirectionPath = client.followRedirection(propfind);
//                status = mRedirectionPath.getLastStatus();
//            }
//            if (status != FORBIDDEN_ERROR && status != SERVICE_UNAVAILABLE_ERROR) {
//                client.exhaustResponse(propfind.getResponseBodyAsStream());
//            }

            /**
             *  PROPFIND method
             *  404 NOT FOUND: path doesn't exist,
             *  207 MULTI_STATUS: path exists.
             */
            boolean isSuccess = ((status == HttpStatus.SC_OK || status == HttpStatus.SC_MULTI_STATUS) &&
                    !mSuccessIfAbsent) ||
                    (status == HttpStatus.SC_MULTI_STATUS && !mSuccessIfAbsent) ||
                    (status == HttpStatus.SC_NOT_FOUND && mSuccessIfAbsent);

            result = isSuccess
                    ? new RemoteOperationResult(OK)
                    : new RemoteOperationResult(
                            false, propfindMethod.getRequest(), propfindMethod.getResponse()
            );

            Log_OC.d(TAG, "Existence check for " + client.getWebdavUri() +
                    WebdavUtils.encodePath(mPath) + " targeting for " +
                    (mSuccessIfAbsent ? " absence " : " existence ") +
                    "finished with HTTP status " + status + (!isSuccess?"(FAIL)":""));
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Existence check for " + client.getWebdavUri() +
                    WebdavUtils.encodePath(mPath) + " targeting for " +
                    (mSuccessIfAbsent ? " absence " : " existence ") + ": " +
                    result.getLogMessage(), result.getException());
            
        } finally {
//            client.setFollowRedirects(previousFollowRedirects);
        }
        return result;
	}


    /**
     * Gets the sequence of redirections followed during the execution of the operation.
     *
     * @return      Sequence of redirections followed, if any, or NULL if the operation was not executed.
     */
    public RedirectionPath getRedirectionPath() {
        return mRedirectionPath;
    }

    /**
     * @return      'True' if the operation was executed and at least one redirection was followed.
     */
    public boolean wasRedirected() {
        return (mRedirectionPath != null && mRedirectionPath.getRedirectionsCount() > 0);
    }
}
