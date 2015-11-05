/* ownCloud Android Library is available under MIT license
 *   @author David A. Velasco
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

package com.owncloud.android.lib.resources.shares;

        import android.net.Uri;
        import android.util.Pair;

        import com.owncloud.android.lib.common.OwnCloudClient;
        import com.owncloud.android.lib.common.operations.RemoteOperation;
        import com.owncloud.android.lib.common.operations.RemoteOperationResult;
        import com.owncloud.android.lib.common.utils.Log_OC;

        import org.apache.commons.httpclient.methods.PutMethod;
        import org.apache.commons.httpclient.methods.StringRequestEntity;
        import org.apache.http.HttpStatus;

        import java.util.ArrayList;
        import java.util.List;


/**
 * Updates parameters of an existing Share resource, known its remote ID.
 *
 * Allow updating several parameters, triggering a request to the server per parameter.
 */

public class UpdateRemoteShareOperation extends RemoteOperation {

    private static final String TAG = GetRemoteShareOperation.class.getSimpleName();

    private long mRemoteId;

    private String mPassword;


    public UpdateRemoteShareOperation(long remoteId) {
        mRemoteId = remoteId;
        mPassword = null;
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status = -1;

        /// prepare array of parameters to update
        List<Pair<String, String>> parametersToUpdate = new ArrayList<Pair<String, String>>();
        if (mPassword != null) {
            parametersToUpdate.add(new Pair<String, String>("password", mPassword));
        }
        /* TODO complete rest of parameters
        if (mPermissions > 0) {
            parametersToUpdate.add(new Pair("permissions", Integer.toString(mPermissions)));
        }
        if (mPublicUpload != null) {
            parametersToUpdate.add(new Pair("publicUpload", mPublicUpload.toString());
        }

        if (mExpireDate != null) {
            parametersToUpdate.add(new Pair("expireData", mExpireData.toString()));
        }
        */

        /// perform required PUT requests
        PutMethod put = null;

        try{
            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH.substring(1));
            uriBuilder.appendEncodedPath(Long.toString(mRemoteId));

            for (Pair<String, String> parameter : parametersToUpdate) {
                if (put != null) {
                    put.releaseConnection();
                }
                // TODO check if uriBuilder may be reused
                String uriString = uriBuilder.build().toString();
                put = new PutMethod(uriString);
                put.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
                put.setRequestEntity(new StringRequestEntity(
                    parameter.first + "=" + parameter.second,
                    "application/x-www-form-urlencoded",
                    "UTF-8"
                ));

                status = client.executeMethod(put);

                if (status == HttpStatus.SC_OK) {
                    String response = put.getResponseBodyAsString();

                    // Parse xml response
                    ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                            new ShareXMLParser()
                    );
                    parser.setOwnCloudVersion(client.getOwnCloudVersion());
                    parser.setServerBaseUri(client.getBaseUri());
                    result = parser.parse(response);

                } else {
                    result = new RemoteOperationResult(false, status, put.getResponseHeaders());
                }
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while updating remote share ", e);
            if (put != null) {
                put.releaseConnection();
            }

        } finally {
            if (put != null) {
                put.releaseConnection();
            }
        }
        return result;
    }

    /**
     * Set password to update in Share resource.
     *
     * @param password      Password to set to the target share. The empty string clears the password.
     *                      Null results in no update applied to the password.
     */
    public void setPassword(String password) {
        mPassword = password;
    }
}
