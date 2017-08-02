/* ownCloud Android Library is available under MIT license
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
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

package com.owncloud.android.lib.common.authentication.oauth;

import android.net.Uri;

import com.owncloud.android.lib.common.authentication.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class OAuth2GetAccessTokenOperation extends RemoteOperation {
    
    private String mGrantType;
    private String mCode;
    private String mClientId;
    private String mClientSecret;
    private String mRedirectUri;
    private final String mAccessTokenEndpointPath;

    private Map<String, String> mResultTokenMap;


    public OAuth2GetAccessTokenOperation(
        String grantType,
        String code,
        String clientId,
        String secretId,
        String redirectUri,
        String accessTokenEndpointPath
    ) {
        mClientId = clientId;
        mClientSecret = secretId;
        mRedirectUri = redirectUri;
        mGrantType = grantType;
        mCode = code;

        mAccessTokenEndpointPath =
            accessTokenEndpointPath != null ?
                accessTokenEndpointPath :
                OwnCloudOAuth2Provider.ACCESS_TOKEN_ENDPOINT_PATH
        ;
        mResultTokenMap = null;
    }

    /*
    public Map<String, String> getResultTokenMap() {
        return mResultTokenMap;
    }
    */
    
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        PostMethod postMethod = null;
        
        try {
            NameValuePair[] nameValuePairs = new NameValuePair[4];
            nameValuePairs[0] = new NameValuePair(OAuth2Constants.KEY_GRANT_TYPE, mGrantType);
            nameValuePairs[1] = new NameValuePair(OAuth2Constants.KEY_CODE, mCode);
            nameValuePairs[2] = new NameValuePair(OAuth2Constants.KEY_REDIRECT_URI, mRedirectUri);
            nameValuePairs[3] = new NameValuePair(OAuth2Constants.KEY_CLIENT_ID, mClientId);

            Uri.Builder uriBuilder = client.getBaseUri().buildUpon();
            uriBuilder.appendEncodedPath(mAccessTokenEndpointPath);

            postMethod = new PostMethod(uriBuilder.build().toString());
            postMethod.setRequestBody(nameValuePairs);

            OwnCloudCredentials oauthCredentials = new OwnCloudBasicCredentials(
                mClientId,
                mClientSecret
            );
            OwnCloudCredentials oldCredentials = switchClientCredentials(oauthCredentials);

            client.executeMethod(postMethod);
            switchClientCredentials(oldCredentials);

            String response = postMethod.getResponseBodyAsString();
            if (response != null && response.length() > 0) {
                JSONObject tokenJson = new JSONObject(response);
                parseAccessTokenResult(tokenJson);
                if (mResultTokenMap.get(OAuth2Constants.KEY_ERROR) != null ||
                        mResultTokenMap.get(OAuth2Constants.KEY_ACCESS_TOKEN) == null) {
                    result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR);

                } else {
                    result = new RemoteOperationResult(true, postMethod);
                    ArrayList<Object> data = new ArrayList<>();
                    data.add(mResultTokenMap);
                    result.setData(data);
                }

            } else {
                result = new RemoteOperationResult(false, postMethod);
                client.exhaustResponse(postMethod.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            
        } finally {
            if (postMethod != null)
                postMethod.releaseConnection();    // let the connection available for other methods

            /*
            if (result.isSuccess()) {
                Log_OC.i(TAG, "OAuth2 TOKEN REQUEST with auth code " +
                        mCode + " to " +
                        client.getWebdavUri() + ": " + result.getLogMessage());
            
            } else if (result.getException() != null) {
                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " +
                        mCode + " to " + client.
                        getWebdavUri() + ": " + result.getLogMessage(), result.getException());
                
            } else if (result.getCode() == ResultCode.OAUTH2_ERROR) {
                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " +
                        mCode + " to " + client.
                        getWebdavUri() + ": " + ((mResultTokenMap != null) ? mResultTokenMap.
                        get(OAuth2Constants.KEY_ERROR) : "NULL"));
                    
            } else {
                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " +
                        mCode + " to " + client.
                        getWebdavUri() + ": " + result.getLogMessage());
            }
            */
        }
        
        return result;
    }

    private OwnCloudCredentials switchClientCredentials(OwnCloudCredentials newCredentials) {
        // work-around for POC with owncloud/oauth2 app, that doesn't allow client
        OwnCloudCredentials previousCredentials = getClient().getCredentials();
        getClient().setCredentials(newCredentials);
        return previousCredentials;
    }


    private void parseAccessTokenResult (JSONObject tokenJson) throws JSONException {
        mResultTokenMap = new HashMap<>();
        
        if (tokenJson.has(OAuth2Constants.KEY_ACCESS_TOKEN)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ACCESS_TOKEN, tokenJson.
                    getString(OAuth2Constants.KEY_ACCESS_TOKEN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_TOKEN_TYPE)) {
            mResultTokenMap.put(OAuth2Constants.KEY_TOKEN_TYPE, tokenJson.
                    getString(OAuth2Constants.KEY_TOKEN_TYPE));
        }
        if (tokenJson.has(OAuth2Constants.KEY_EXPIRES_IN)) {
            mResultTokenMap.put(OAuth2Constants.KEY_EXPIRES_IN, tokenJson.
                    getString(OAuth2Constants.KEY_EXPIRES_IN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_REFRESH_TOKEN)) {
            mResultTokenMap.put(OAuth2Constants.KEY_REFRESH_TOKEN, tokenJson.
                    getString(OAuth2Constants.KEY_REFRESH_TOKEN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_SCOPE)) {
            mResultTokenMap.put(OAuth2Constants.KEY_SCOPE, tokenJson.
                    getString(OAuth2Constants.KEY_SCOPE));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ERROR, tokenJson.
                    getString(OAuth2Constants.KEY_ERROR));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR_DESCRIPTION)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ERROR_DESCRIPTION, tokenJson.
                    getString(OAuth2Constants.KEY_ERROR_DESCRIPTION));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR_URI)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ERROR_URI, tokenJson.
                    getString(OAuth2Constants.KEY_ERROR_URI));
        }

        if (tokenJson.has(OAuth2Constants.KEY_USER_ID)) {   // not standard
            mResultTokenMap.put(OAuth2Constants.KEY_USER_ID, tokenJson.
                    getString(OAuth2Constants.KEY_USER_ID));
        }
    }
}
