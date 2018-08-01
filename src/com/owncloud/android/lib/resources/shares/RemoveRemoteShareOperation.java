/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.shares;

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

/**
 * Remove a share
 */

public class RemoveRemoteShareOperation extends RemoteOperation {

    private static final String TAG = RemoveRemoteShareOperation.class.getSimpleName();

    private int mRemoteShareId;

    /**
     * Constructor
     *
     * @param remoteShareId Share ID
     */

    public RemoveRemoteShareOperation(int remoteShareId) {
        mRemoteShareId = remoteShareId;

    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        int status;

        DeleteMethod delete = null;

        try {
            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH);
            uriBuilder.appendEncodedPath(String.valueOf(mRemoteShareId));

            delete = new DeleteMethod(uriBuilder.build().toString());

            delete.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            status = client.executeMethod(delete);

            if (isSuccess(status)) {
                String response = delete.getResponseBodyAsString();

                // Parse xml response and obtain the list of shares
                ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                    new ShareXMLParser()
                );
                result = parser.parse(response);

                Log_OC.d(TAG, "Unshare " + mRemoteShareId + ": " + result.getLogMessage());

            } else {
                result = new RemoteOperationResult(false, delete);
            }
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Unshare Link Exception " + result.getLogMessage(), e);

        } finally {
            if (delete != null)
                delete.releaseConnection();
        }
        return result;
    }


    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
}