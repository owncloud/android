/* ownCloud Android Library is available under MIT license
 *
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2015 Bartosz Przybylski
 *   Copyright (C) 2014 Marcello Steiner
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

package com.owncloud.android.lib.resources.users;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author marcello
 */
public class GetRemoteUserQuotaOperation extends RemoteOperation {

    static public class Quota {
        long mFree, mUsed, mTotal;
        double mRelative;

        public Quota(long free, long used, long total, double relative) {
            mFree = free;
            mUsed = used;
            mTotal = total;
            mRelative = relative;
        }

        public long getFree() { return mFree; }
        public long getUsed() { return mUsed; }
        public long getTotal() { return mTotal; }
        public double getRelative() { return mRelative; }
    }

    private static final String TAG = GetRemoteUserQuotaOperation.class.getSimpleName();

    private static final String NODE_OCS = "ocs";
    private static final String NODE_DATA = "data";
    private static final String NODE_QUOTA = "quota";
    private static final String NODE_QUOTA_FREE = "free";
    private static final String NODE_QUOTA_USED = "used";
    private static final String NODE_QUOTA_TOTAL = "total";
    private static final String NODE_QUOTA_RELATIVE = "relative";

    // OCS Route
    private static final String OCS_ROUTE ="/ocs/v1.php/cloud/users/";

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status;
        GetMethod get = null;


        //Get the user
        try {
            OwnCloudCredentials credentials = client.getCredentials();
            String url = client.getBaseUri() + OCS_ROUTE + credentials.getUsername();

            get = new GetMethod(url);
            get.setQueryString(new NameValuePair[]{new NameValuePair("format","json")});
            get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
            status = client.executeMethod(get);

            if(isSuccess(status)) {
                String response = get.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
                JSONObject respData = respOCS.getJSONObject(NODE_DATA);
                JSONObject quota    = respData.getJSONObject(NODE_QUOTA);
                final Long quotaFree = quota.getLong(NODE_QUOTA_FREE);
                final Long quotaUsed = quota.getLong(NODE_QUOTA_USED);
                final Long quotaTotal = quota.getLong(NODE_QUOTA_TOTAL);
                final Double quotaRelative = quota.getDouble(NODE_QUOTA_RELATIVE);


                // Result
                result = new RemoteOperationResult(true, status, get.getResponseHeaders());
                //Quota data in data collection
                ArrayList<Object> data = new ArrayList<Object>();
                data.add(new Quota(quotaFree, quotaUsed, quotaTotal, quotaRelative));
                result.setData(data);

            } else {
                result = new RemoteOperationResult(false, status, get.getResponseHeaders());
                String response = get.getResponseBodyAsString();
                Log_OC.e(TAG, "Failed response while getting user quota information ");
                if (response != null) {
                    Log_OC.e(TAG, "*** status code: " + status + " ; response message: " + response);
                } else {
                    Log_OC.e(TAG, "*** status code: " + status);
                }
            }
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while getting OC user information", e);

        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }


}
