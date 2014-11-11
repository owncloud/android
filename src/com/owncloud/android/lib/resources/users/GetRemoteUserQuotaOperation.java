package com.owncloud.android.lib.resources.users;

import android.util.Base64;
import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Created by marcello on 11/11/14.
 */
public class GetRemoteUserQuotaOperation extends RemoteOperation {

    private static final String TAG = GetRemoteUserNameOperation.class.getSimpleName();


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
        int status = -1;
        GetMethod get = null;


        //Get the user
        try {
            OwnCloudBasicCredentials credentials = (OwnCloudBasicCredentials) client.getCredentials();
            String url = client.getBaseUri() + OCS_ROUTE + credentials.getUsername();

            get = new GetMethod(url);
            get.addRequestHeader(createAuthenticationHeader(credentials));
            get.setQueryString(new NameValuePair[]{new NameValuePair("format","json")});
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
                data.add(quotaFree);
                data.add(quotaUsed);
                data.add(quotaTotal);
                data.add(quotaRelative);
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
            get.releaseConnection();
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }

    private Header createAuthenticationHeader(OwnCloudCredentials credentials){
        Header h = new Header();
        h.setName("Authorization");
        String authString = credentials.getUsername()+":"+credentials.getAuthToken();
        String encodedString = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
        h.setValue("Basic "+encodedString);
        return h;
    }


}
