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

package com.owncloud.android.lib.common.authentication.oauth;

import android.net.Uri;

import com.owncloud.android.lib.common.authentication.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class OAuth2RefreshAccessTokenOperation extends RemoteOperation {

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
    protected RemoteOperationResult run(OwnCloudClient client) {

        RemoteOperationResult result = null;
        PostMethod postMethod = null;

        try {
            NameValuePair[] nameValuePairs = new NameValuePair[3];
            nameValuePairs[0] = new NameValuePair(
                OAuth2Constants.KEY_GRANT_TYPE,
                OAuth2GrantType.REFRESH_TOKEN.getValue()    // always for this operation
            );
            nameValuePairs[1] = new NameValuePair(OAuth2Constants.KEY_CLIENT_ID, mClientId);
            nameValuePairs[2] = new NameValuePair(OAuth2Constants.KEY_REFRESH_TOKEN, mRefreshToken);

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
            Log_OC.d(TAG, "OAUTH2: raw response from POST TOKEN: " + response);

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
            if (postMethod != null) {
                postMethod.releaseConnection();    // let the connection available for other methods
            }
        }

        return result;
    }

    private OwnCloudCredentials switchClientCredentials(OwnCloudCredentials newCredentials) {
        OwnCloudCredentials previousCredentials = getClient().getCredentials();
        getClient().setCredentials(newCredentials);
        return previousCredentials;
    }

}