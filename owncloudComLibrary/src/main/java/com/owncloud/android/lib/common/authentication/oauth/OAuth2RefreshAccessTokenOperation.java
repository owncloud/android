/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   @author Christian Schabesberger
 *
 *   Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.lib.common.authentication.oauth;

import android.net.Uri;

import com.owncloud.android.lib.common.authentication.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.json.JSONObject;

import java.net.URL;
import java.util.Map;

import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class OAuth2RefreshAccessTokenOperation extends RemoteOperation<Map<String, String>> {

    private static final String TAG = OAuth2RefreshAccessTokenOperation.class.getSimpleName();

    private String mClientId;
    private String mClientSecret;
    private String mRefreshToken;

    private final String mAccessTokenEndpointPath;

    private final OAuth2ResponseParser mResponseParser;

    public OAuth2RefreshAccessTokenOperation(
            String clientId,
            String secretId,
            String refreshToken,
            String accessTokenEndpointPath
    ) {

        mClientId = clientId;
        mClientSecret = secretId;
        mRefreshToken = refreshToken;

        mAccessTokenEndpointPath =
                accessTokenEndpointPath != null ?
                        accessTokenEndpointPath :
                        OwnCloudOAuth2Provider.ACCESS_TOKEN_ENDPOINT_PATH
        ;

        mResponseParser = new OAuth2ResponseParser();
    }

    @Override
    protected RemoteOperationResult<Map<String, String>> run(OwnCloudClient client) {

        try {
            final RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(OAuth2Constants.KEY_GRANT_TYPE,
                            OAuth2GrantType.REFRESH_TOKEN.getValue())
                    .addFormDataPart(OAuth2Constants.KEY_CLIENT_ID, mClientId)
                    .addFormDataPart(OAuth2Constants.KEY_REFRESH_TOKEN, mRefreshToken)
                    .build();

            Uri.Builder uriBuilder = client.getBaseUri().buildUpon();
            uriBuilder.appendEncodedPath(mAccessTokenEndpointPath);

            final PostMethod postMethod = new PostMethod(new URL(
                    client.getBaseUri().buildUpon()
                            .appendEncodedPath(mAccessTokenEndpointPath)
                            .build()
                            .toString()));
            postMethod.setRequestBody(requestBody);

            final OwnCloudCredentials oauthCredentials = new OwnCloudBasicCredentials(mClientId, mClientSecret);

            final OwnCloudCredentials oldCredentials = switchClientCredentials(oauthCredentials);
            client.executeHttpMethod(postMethod);
            switchClientCredentials(oldCredentials);

            final String responseData = postMethod.getResponseBodyAsString();
            Log_OC.d(TAG, "OAUTH2: raw response from POST TOKEN: " + responseData);

            if (responseData != null && responseData.length() > 0) {
                final JSONObject tokenJson = new JSONObject(responseData);

                final Map<String, String> accessTokenResult =
                        mResponseParser.parseAccessTokenResult(tokenJson);

                final RemoteOperationResult<Map<String, String>> result = new RemoteOperationResult<>(ResultCode.OK);
                result.setData(accessTokenResult);
                return (accessTokenResult.get(OAuth2Constants.KEY_ERROR) != null ||
                        accessTokenResult.get(OAuth2Constants.KEY_ACCESS_TOKEN) == null)
                        ? new RemoteOperationResult<>(ResultCode.OAUTH2_ERROR)
                        : result;
            } else {
                return new RemoteOperationResult<>(postMethod);
            }

        } catch (Exception e) {
            return new RemoteOperationResult<>(e);
        }
    }

    private OwnCloudCredentials switchClientCredentials(OwnCloudCredentials newCredentials) {
        OwnCloudCredentials previousCredentials = getClient().getCredentials();
        getClient().setCredentials(newCredentials);
        return previousCredentials;
    }
}