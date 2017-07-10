/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.operations;

import com.owncloud.android.authentication.OAuth2Constants;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.methods.PostMethod;

import java.util.Map;

public class OAuth2RefreshAccessToken extends RemoteOperation {

    private static final String TAG = OAuth2GetAccessToken.class.getSimpleName();

    private String mClientId;
    private String mSecretId;
    private String mGrantType;
    private String mRefreshToken;

    private String mOAuth2RefreshAccessTokenQueryParams;

    private Map<String, String> mOAuth2ParsedRefreshAccessTokenQueryParams;

    public OAuth2RefreshAccessToken(
            String clientId,
            String secretId,
            String grantType,
            String refreshToken
    ) {

        mClientId = clientId;
        mSecretId = secretId;
        mGrantType = grantType;
        mRefreshToken = refreshToken;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        RemoteOperationResult result = null;
        PostMethod postMethod = null;

        try {
            parseAuthorizationResponse();

            if (mOAuth2ParsedRefreshAccessTokenQueryParams.keySet().contains(OAuth2Constants.
                    KEY_ERROR)) {
                if (OAuth2Constants.VALUE_ERROR_ACCESS_DENIED.equals(
                        mOAuth2ParsedRefreshAccessTokenQueryParams.get(OAuth2Constants.KEY_ERROR))) {
                    result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR_ACCESS_DENIED);
                } else {
                    result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR);
                }
            }
//
//            if (result == null) {
//                NameValuePair[] nameValuePairs = new NameValuePair[4];
//                nameValuePairs[0] = new NameValuePair(OAuth2Constants.KEY_GRANT_TYPE, mGrantType);
//                nameValuePairs[1] = new NameValuePair(OAuth2Constants.KEY_CODE,
//                        mOAuth2ParsedAuthorizationResponse.get(OAuth2Constants.KEY_CODE));
//                nameValuePairs[2] = new NameValuePair(OAuth2Constants.KEY_REDIRECT_URI, mRedirectUri);
//                nameValuePairs[3] = new NameValuePair(OAuth2Constants.KEY_CLIENT_ID, mClientId);
//
//                Uri.Builder uriBuilder = client.getBaseUri().buildUpon();
//
//                postMethod = new PostMethod(uriBuilder.build().toString());
//                postMethod.setRequestBody(nameValuePairs);
//
//                OwnCloudCredentials oauthCredentials = new OwnCloudBasicCredentials(
//                        mClientId,
//                        mSecretId
//                );
//                OwnCloudCredentials oldCredentials = switchClientCredentials(oauthCredentials);
//
//                // TODO Handle this status
//                int status = client.executeMethod(postMethod);
//                switchClientCredentials(oldCredentials);
//
//                String response = postMethod.getResponseBodyAsString();
//                Log_OC.d(TAG, "OAUTH2: raw response from POST TOKEN: " + response);
//                if (response != null && response.length() > 0) {
//                    JSONObject tokenJson = new JSONObject(response);
//                    parseAccessTokenResult(tokenJson);
//                    if (mResultTokenMap.get(OAuth2Constants.KEY_ERROR) != null ||
//                            mResultTokenMap.get(OAuth2Constants.KEY_ACCESS_TOKEN) == null) {
//                        result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR);
//
//                    } else {
//                        result = new RemoteOperationResult(true, postMethod);
//                        ArrayList<Object> data = new ArrayList<>();
//                        data.add(mResultTokenMap);
//                        result.setData(data);
//                    }
//
//                } else {
//                    result = new RemoteOperationResult(false, postMethod);
//                    client.exhaustResponse(postMethod.getResponseBodyAsStream());
//                }
//            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);

        } finally {
//            if (postMethod != null)
//                postMethod.releaseConnection();    // let the connection available for other methods
//
//            if (result.isSuccess()) {
//                Log_OC.i(TAG, "OAuth2 TOKEN REQUEST with auth code " +
//                        mOAuth2ParsedAuthorizationResponse.get("code") + " to " +
//                        client.getWebdavUri() + ": " + result.getLogMessage());
//
//            } else if (result.getException() != null) {
//                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " +
//                        mOAuth2ParsedAuthorizationResponse.get("code") + " to " + client.
//                        getWebdavUri() + ": " + result.getLogMessage(), result.getException());
//
//            } else if (result.getCode() == ResultCode.OAUTH2_ERROR) {
//                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " +
//                        mOAuth2ParsedAuthorizationResponse.get("code") + " to " + client.
//                        getWebdavUri() + ": " + ((mResultTokenMap != null) ? mResultTokenMap.
//                        get(OAuth2Constants.KEY_ERROR) : "NULL"));
//
//            } else {
//                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " +
//                        mOAuth2ParsedAuthorizationResponse.get("code") + " to " + client.
//                        getWebdavUri() + ": " + result.getLogMessage());
//            }
        }

        return result;
    }

    private void parseAuthorizationResponse() {
        String[] pairs = mOAuth2RefreshAccessTokenQueryParams.split("&");
        int i = 0;
        String key = "";
        String value = "";
        StringBuilder sb = new StringBuilder();
        while (pairs.length > i) {
            int j = 0;
            String[] part = pairs[i].split("=");
            while (part.length > j) {
                String p = part[j];
                if (j == 0) {
                    key = p;
                    sb.append(key + " = ");
                } else if (j == 1) {
                    value = p;
                    mOAuth2ParsedRefreshAccessTokenQueryParams.put(key, value);
                    sb.append(value + "\n");
                }

                Log_OC.v(TAG, "[" + i + "," + j + "] = " + p);
                j++;
            }
            i++;
        }
    }
}
