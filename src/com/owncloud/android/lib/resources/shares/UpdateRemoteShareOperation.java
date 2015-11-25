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

        import java.text.DateFormat;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.List;


/**
 * Updates parameters of an existing Share resource, known its remote ID.
 *
 * Allow updating several parameters, triggering a request to the server per parameter.
 */

public class UpdateRemoteShareOperation extends RemoteOperation {

    private static final String TAG = GetRemoteShareOperation.class.getSimpleName();

    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_EXPIRATION_DATE = "expireDate";
    private static final String FORMAT_EXPIRATION_DATE = "yyyy-MM-dd";
    private static final String ENTITY_CONTENT_TYPE  = "application/x-www-form-urlencoded";
    private static final String ENTITY_CHARSET = "UTF-8";


    /** Identifier of the share to update */
    private long mRemoteId;

    /** Password to set for the public link */
    private String mPassword;

    /** Expiration date to set for the public link */
    private long mExpirationDateInMillis;


    /**
     * Constructor. No update is initialized by default, need to be applied with setters below.
     *
     * @param remoteId  Identifier of the share to update.
     */
    public UpdateRemoteShareOperation(long remoteId) {
        mRemoteId = remoteId;
        mPassword = null;               // no update
        mExpirationDateInMillis = 0;    // no update
    }


    /**
     * Set password to update in Share resource.
     *
     * @param password      Password to set to the target share.
     *                      Empty string clears the current password.
     *                      Null results in no update applied to the password.
     */
    public void setPassword(String password) {
        mPassword = password;
    }


    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis    Expiration date to set to the public link.
     *                                  A negative value clears the current expiration date.
     *                                  Zero value (start-of-epoch) results in no update done on
     *                                  the expiration date.
     */
    public void setExpirationDate(long expirationDateInMillis) {
        mExpirationDateInMillis = expirationDateInMillis;
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status = -1;

        /// prepare array of parameters to update
        List<Pair<String, String>> parametersToUpdate = new ArrayList<Pair<String, String>>();
        if (mPassword != null) {
            parametersToUpdate.add(new Pair<String, String>(PARAM_PASSWORD, mPassword));
        }
        if (mExpirationDateInMillis < 0) {
            // clear expiration date
            parametersToUpdate.add(new Pair(PARAM_EXPIRATION_DATE, ""));

        } else if (mExpirationDateInMillis > 0) {
            // set expiration date
            DateFormat dateFormat = new SimpleDateFormat(FORMAT_EXPIRATION_DATE);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.setTimeInMillis(mExpirationDateInMillis);
            String formattedExpirationDate = dateFormat.format(expirationDate.getTime());
            parametersToUpdate.add(new Pair(PARAM_EXPIRATION_DATE, formattedExpirationDate));

        } // else, ignore - no update

        /* TODO complete rest of parameters
        if (mPermissions > 0) {
            parametersToUpdate.add(new Pair("permissions", Integer.toString(mPermissions)));
        }
        if (mPublicUpload != null) {
            parametersToUpdate.add(new Pair("publicUpload", mPublicUpload.toString());
        }
        */

        /// perform required PUT requests
        PutMethod put = null;
        String uriString = null;

        try{
            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH.substring(1));
            uriBuilder.appendEncodedPath(Long.toString(mRemoteId));
            uriString = uriBuilder.build().toString();

            for (Pair<String, String> parameter : parametersToUpdate) {
                if (put != null) {
                    put.releaseConnection();
                }
                put = new PutMethod(uriString);
                put.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
                put.setRequestEntity(new StringRequestEntity(
                    parameter.first + "=" + parameter.second,
                    ENTITY_CONTENT_TYPE,
                    ENTITY_CHARSET
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

}
