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

package com.owncloud.android.lib.resources.users;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import org.json.JSONObject;
import timber.log.Timber;

import java.net.URL;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Gets information (id, display name, and e-mail address) about the user logged in.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */

public class GetRemoteUserInfoOperation extends RemoteOperation<GetRemoteUserInfoOperation.UserInfo> {

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
    protected RemoteOperationResult<UserInfo> run(OwnCloudClient client) {
        RemoteOperationResult<UserInfo> result;

        //Get the user
        try {
            GetMethod getMethod = new GetMethod(new URL(client.getBaseUri() + OCS_ROUTE));

            int status = client.executeHttpMethod(getMethod);

            if (isSuccess(status)) {
                Timber.d("Successful response");

                JSONObject respJSON = new JSONObject(getMethod.getResponseBodyAsString());
                JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
                JSONObject respData = respOCS.getJSONObject(NODE_DATA);

                UserInfo userInfo = new UserInfo();
                userInfo.mId = respData.getString(NODE_ID);
                userInfo.mDisplayName = respData.getString(NODE_DISPLAY_NAME);
                userInfo.mEmail = respData.getString(NODE_EMAIL);

                result = new RemoteOperationResult<>(OK);

                result.setData(userInfo);

            } else {
                result = new RemoteOperationResult<>(getMethod);
                String response = getMethod.getResponseBodyAsString();
                Timber.e("Failed response while getting user information ");
                Timber.e("*** status code: " + status + " ; response message: " + response);
            }
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Timber.e(e, "Exception while getting OC user information");
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