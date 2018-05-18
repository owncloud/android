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

package com.owncloud.android.lib.refactor.authentication.oauth.operations;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.RemoteOperationResult;
import com.owncloud.android.lib.refactor.Log_OC;
import com.owncloud.android.lib.refactor.RemoteOperation;
import com.owncloud.android.lib.refactor.authentication.oauth.OAuth2Constants;
import com.owncloud.android.lib.refactor.authentication.oauth.OAuth2GrantType;
import com.owncloud.android.lib.refactor.authentication.oauth.OAuth2ResponseParser;
import com.owncloud.android.lib.refactor.authentication.oauth.OwnCloudOAuth2Provider;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuth2RefreshAccessTokenOperation extends RemoteOperation {

    private static final String TAG = OAuth2RefreshAccessTokenOperation.class.getSimpleName();

    private String mClientId;
    private String mClientSecret;
    private String mRefreshToken;

    private final String mAccessTokenEndpointPath;

    private final OAuth2ResponseParser mResponseParser;

    public OAuth2RefreshAccessTokenOperation(
            OCContext ocContext,
            String clientId,
            String secretId,
            String refreshToken,
            String accessTokenEndpointPath
    ) {
        super(ocContext);

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
    public RemoteOperationResult exec() {
        try {
            final RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(OAuth2Constants.KEY_GRANT_TYPE,
                            OAuth2GrantType.REFRESH_TOKEN.getValue())
                    .addFormDataPart(OAuth2Constants.KEY_CLIENT_ID, mClientId)
                    .addFormDataPart(OAuth2Constants.KEY_REFRESH_TOKEN, mRefreshToken)
                    .build();

            final Request request = getRequestBuilder()
                    .url(getBaseUriBuilder()
                            .appendEncodedPath(mAccessTokenEndpointPath)
                            .build()
                            .toString())
                    .method("POST", requestBody)
                    .build();

            final Response response = getClient().newCall(request).execute();

            final String responseData = response.body().string();
            Log_OC.d(TAG, "OAUTH2: raw response from POST TOKEN: " + responseData);

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