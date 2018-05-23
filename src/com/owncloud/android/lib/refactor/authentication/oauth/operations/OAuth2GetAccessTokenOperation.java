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

package com.owncloud.android.lib.refactor.authentication.oauth.operations;

import android.net.Uri;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.RemoteOperationResult;
import com.owncloud.android.lib.refactor.authentication.oauth.OAuth2Constants;
import com.owncloud.android.lib.refactor.authentication.oauth.OAuth2ResponseParser;
import com.owncloud.android.lib.refactor.authentication.oauth.OwnCloudOAuth2Provider;
import com.owncloud.android.lib.refactor.RemoteOperation;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OAuth2GetAccessTokenOperation extends RemoteOperation {
    
    private final String mGrantType;
    private final String mCode;
    private final String mClientId;
    private final String mClientSecret;
    private final String mRedirectUri;
    private final String mAccessTokenEndpointPath;
    private final OAuth2ResponseParser mResponseParser;

    public OAuth2GetAccessTokenOperation(
            OCContext context,
            String grantType,
            String code,
            String clientId,
            String secretId,
            String redirectUri,
            String accessTokenEndpointPath) {
        super(context);
        mClientId = clientId;
        mClientSecret = secretId;
        mRedirectUri = redirectUri;
        mGrantType = grantType;
        mCode = code;

        mAccessTokenEndpointPath =
            accessTokenEndpointPath != null
                    ? accessTokenEndpointPath
                    : OwnCloudOAuth2Provider.ACCESS_TOKEN_ENDPOINT_PATH;

        mResponseParser = new OAuth2ResponseParser();
    }

    @Override
    public RemoteOperationResult exec() {
        try {
            final RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(OAuth2Constants.KEY_GRANT_TYPE, mGrantType)
                    .addFormDataPart(OAuth2Constants.KEY_CODE, mCode)
                    .addFormDataPart(OAuth2Constants.KEY_REDIRECT_URI, mRedirectUri)
                    .addFormDataPart(OAuth2Constants.KEY_CLIENT_ID, mClientId)
                    .build();

            final Request request = getRequestBuilder()
                    .url(getHttpUrl(mAccessTokenEndpointPath))
                    .method("POST", requestBody)
                    .build();

            final Response response = getClient()
                    .newCall(request)
                    .execute();

            final String responseData = response.body().string();
            
            if (responseData != null && responseData.length() > 0) {
                JSONObject tokenJson = new JSONObject(responseData);
                Map<String, String> accessTokenResult =
                    mResponseParser.parseAccessTokenResult(tokenJson);
                if (accessTokenResult.get(OAuth2Constants.KEY_ERROR) != null ||
                        accessTokenResult.get(OAuth2Constants.KEY_ACCESS_TOKEN) == null) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.OAUTH2_ERROR);

                } else {
                    final RemoteOperationResult result = new RemoteOperationResult(true, request, response);
                    ArrayList<Object> data = new ArrayList<>();
                    data.add(accessTokenResult);
                    result.setData(data);
                    return result;
                }

            } else {
                return new RemoteOperationResult(false, request, response);
            }

        } catch (Exception e) {
            return new RemoteOperationResult(e);
        }
    }
}