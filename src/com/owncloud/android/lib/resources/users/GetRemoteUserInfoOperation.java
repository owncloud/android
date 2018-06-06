/* ownCloud Android Library is available under MIT license
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

package com.owncloud.android.lib.resources.users;

import java.util.ArrayList;
import org.json.JSONObject;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.common.http.nonwebdav.GetMethod;

import okhttp3.Request;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;


/**
 * Gets information (id, display name, and e-mail address) about the user logged in.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */

public class GetRemoteUserInfoOperation extends RemoteOperation {

    private static final String TAG = GetRemoteUserInfoOperation.class.getSimpleName();

    // OCS Route
    private static final String OCS_ROUTE = "/ocs/v2.php/cloud/user?format=json";

    // JSON Node names
    private static final String NODE_OCS = "ocs";
    private static final String NODE_DATA = "data";
    private static final String NODE_ID = "id";
    private static final String NODE_DISPLAY_NAME = "display-name";
    private static final String NODE_EMAIL = "email";

    public GetRemoteUserInfoOperation() {
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;

        //Get the user
        try {
            final Request request = new Request.Builder()
                    .url(client.getBaseUri() + OCS_ROUTE)
                    .addHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
                    .build();

            GetMethod getMethod = new GetMethod(client.getOkHttpClient(), request);
            int status = client.executeHttpMethod(getMethod);
            String response = getMethod.getResponse().body().string();

            if (isSuccess(status)) {
                Log_OC.d(TAG, "Successful response");

                JSONObject respJSON = new JSONObject(response);
                JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
                JSONObject respData = respOCS.getJSONObject(NODE_DATA);

                UserInfo userInfo = new UserInfo();
                userInfo.mId = respData.getString(NODE_ID);
                userInfo.mDisplayName = respData.getString(NODE_DISPLAY_NAME);
                userInfo.mEmail = respData.getString(NODE_EMAIL);

                result = new RemoteOperationResult(OK);

                ArrayList<Object> data = new ArrayList<>();
                data.add(userInfo);
                result.setData(data);

            } else {
                result = new RemoteOperationResult(false, getMethod.getRequest(), getMethod.getResponse());
                Log_OC.e(TAG, "Failed response while getting user information ");
                if (response != null) {
                    Log_OC.e(TAG, "*** status code: " + status + " ; response message: " + response);
                } else {
                    Log_OC.e(TAG, "*** status code: " + status);
                }
            }
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while getting OC user information", e);
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpConstants.HTTP_OK);
    }

    public static class UserInfo {
        public String mId = "";
        public String mDisplayName = "";
        public String mEmail = "";
    }
}