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

    private final OAuth2ResponseParser mResponseParser;


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

        mResponseParser = new OAuth2ResponseParser();
    }

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
                Map<String, String> accessTokenResult =
                    mResponseParser.parseAccessTokenResult(tokenJson);
                if (accessTokenResult.get(OAuth2Constants.KEY_ERROR) != null ||
                        accessTokenResult.get(OAuth2Constants.KEY_ACCESS_TOKEN) == null) {
                    result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR);

                } else {
                    result = new RemoteOperationResult(true, postMethod);
                    ArrayList<Object> data = new ArrayList<>();
                    data.add(accessTokenResult);
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
        }
        
        return result;
    }

    private OwnCloudCredentials switchClientCredentials(OwnCloudCredentials newCredentials) {
        OwnCloudCredentials previousCredentials = getClient().getCredentials();
        getClient().setCredentials(newCredentials);
        return previousCredentials;
    }

}
