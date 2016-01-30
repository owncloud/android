/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;

import android.content.Context;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Operation to check the existence or absence of a path in a remote server.
 * 
 * @author David A. Velasco
 */
public class ExistenceCheckRemoteOperation extends RemoteOperation {
    
    /** Maximum time to wait for a response from the server in MILLISECONDs.  */
    public static final int TIMEOUT = 10000;
    
    private static final String TAG = ExistenceCheckRemoteOperation.class.getSimpleName();
    
    private String mPath;
    private boolean mSuccessIfAbsent;

    /** Sequence of redirections followed. Available only after executing the operation */
    private RedirectionPath mRedirectionPath = null;
        // TODO move to {@link RemoteOperation}, that needs a nice refactoring

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

    /**
     * Full constructor. Success of the operation will depend upon the value of successIfAbsent.
     * 
     * @param remotePath        Path to append to the URL owned by the client instance.
     * @param context           Android application context.
     * @param successIfAbsent   When 'true', the operation finishes in success if the path does
     *                          NOT exist in the remote server (HTTP 404).
     * @deprecated
     */
    public ExistenceCheckRemoteOperation(String remotePath, Context context, boolean successIfAbsent) {
        this(remotePath, successIfAbsent);
    }

    @Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        HeadMethod head = null;
        boolean previousFollowRedirects = client.getFollowRedirects();
        try {
            head = new HeadMethod(client.getWebdavUri() + WebdavUtils.encodePath(mPath));
            client.setFollowRedirects(false);
            int status = client.executeMethod(head, TIMEOUT, TIMEOUT);
            if (previousFollowRedirects) {
                mRedirectionPath = client.followRedirection(head);
                status = mRedirectionPath.getLastStatus();
            }
            client.exhaustResponse(head.getResponseBodyAsStream());
            boolean success = (status == HttpStatus.SC_OK && !mSuccessIfAbsent) ||
                    (status == HttpStatus.SC_NOT_FOUND && mSuccessIfAbsent);
            result = new RemoteOperationResult(success, status, head.getResponseHeaders());
            Log_OC.d(TAG, "Existence check for " + client.getWebdavUri() +
                    WebdavUtils.encodePath(mPath) + " targeting for " +
                    (mSuccessIfAbsent ? " absence " : " existence ") +
                    "finished with HTTP status " + status + (!success?"(FAIL)":""));
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Existence check for " + client.getWebdavUri() +
                    WebdavUtils.encodePath(mPath) + " targeting for " +
                    (mSuccessIfAbsent ? " absence " : " existence ") + ": " +
                    result.getLogMessage(), result.getException());
            
        } finally {
            if (head != null)
                head.releaseConnection();
            client.setFollowRedirects(previousFollowRedirects);
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
