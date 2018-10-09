/* ownCloud Android Library is available under MIT license
 *
 *   @author David A. Velasco
 *   @author Christian Schabesberger
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

package com.owncloud.android.lib.common.authentication.oauth;

import android.net.Uri;

import com.owncloud.android.lib.common.authentication.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import org.json.JSONObject;

import java.net.URL;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;


public class OAuth2GetAccessTokenOperation extends RemoteOperation<Map<String, String>> {
    
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
        RemoteOperationResult<Map<String, String>> result = null;
        
        try {

            final RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(OAuth2Constants.KEY_GRANT_TYPE, mGrantType)
                    .addFormDataPart(OAuth2Constants.KEY_CODE, mCode)
                    .addFormDataPart(OAuth2Constants.KEY_REDIRECT_URI, mRedirectUri)
                    .addFormDataPart(OAuth2Constants.KEY_CLIENT_ID, mClientId)
                    .build();

            Uri.Builder uriBuilder = client.getBaseUri().buildUpon();
            uriBuilder.appendEncodedPath(mAccessTokenEndpointPath);

            final PostMethod postMethod = new PostMethod(new URL(
                    client.getBaseUri().buildUpon()
                            .appendEncodedPath(mAccessTokenEndpointPath)
                            .build()
                            .toString()));

            postMethod.setRequestBody(requestBody);

            OwnCloudCredentials oauthCredentials =
                    new OwnCloudBasicCredentials(mClientId, mClientSecret);
            OwnCloudCredentials oldCredentials = switchClientCredentials(oauthCredentials);
            client.executeHttpMethod(postMethod);
            switchClientCredentials(oldCredentials);

            String response = postMethod.getResponseBodyAsString();
            if (response != null && response.length() > 0) {
                JSONObject tokenJson = new JSONObject(response);
                Map<String, String> accessTokenResult =
                    mResponseParser.parseAccessTokenResult(tokenJson);
                if (accessTokenResult.get(OAuth2Constants.KEY_ERROR) != null ||
                        accessTokenResult.get(OAuth2Constants.KEY_ACCESS_TOKEN) == null) {
                    result = new RemoteOperationResult<>(ResultCode.OAUTH2_ERROR);

                } else {
                    result = new RemoteOperationResult<>(ResultCode.OK);
                    result.setData(accessTokenResult);
                }

            } else {
                result = new RemoteOperationResult<>(ResultCode.OK);
                client.exhaustResponse(postMethod.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            
        }
        return result;
    }

    private OwnCloudCredentials switchClientCredentials(OwnCloudCredentials newCredentials) {
        OwnCloudCredentials previousCredentials = getClient().getCredentials();
        getClient().setCredentials(newCredentials);
        return previousCredentials;
    }
}