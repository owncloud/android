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

import com.owncloud.android.lib.common.operations.RemoteOperation;

public class OwnCloudOAuth2RequestBuilder implements OAuth2RequestBuilder {

    private OwnCloudOAuth2Provider mOAuth2Provider;

    private OAuthRequest mRequest;
    private OAuth2GrantType mGrantType = OAuth2GrantType.AUTHORIZATION_CODE;
    private String mCode;
    private String mRefreshToken;

    public OwnCloudOAuth2RequestBuilder(OwnCloudOAuth2Provider ownCloudOAuth2Provider) {
        mOAuth2Provider = ownCloudOAuth2Provider;
    }

    @Override
    public void setRequest(OAuthRequest request) {
        mRequest = request;
    }

    @Override
    public void setGrantType(OAuth2GrantType grantType) {
        mGrantType = grantType;
    }

    @Override
    public void setAuthorizationCode(String code) {
        mCode = code;
    }

    @Override
    public void setRefreshToken(String refreshToken) {
        mRefreshToken = refreshToken;
    }

    @Override
    public RemoteOperation buildOperation() {
        if (mGrantType != OAuth2GrantType.AUTHORIZATION_CODE &&
                mGrantType != OAuth2GrantType.REFRESH_TOKEN) {
            throw new UnsupportedOperationException(
                "Unsupported grant type. Only " +
                    OAuth2GrantType.AUTHORIZATION_CODE.getValue() + " and " +
                        OAuth2GrantType.REFRESH_TOKEN + " are supported"
            );
        }
        OAuth2ClientConfiguration clientConfiguration = mOAuth2Provider.getClientConfiguration();

        switch(mRequest) {
            case CREATE_ACCESS_TOKEN:
                return new OAuth2GetAccessTokenOperation(
                    mGrantType.getValue(),
                    mCode,
                    clientConfiguration.getClientId(),
                    clientConfiguration.getClientSecret(),
                    clientConfiguration.getRedirectUri(),
                    mOAuth2Provider.getAccessTokenEndpointPath()
                );

            case REFRESH_ACCESS_TOKEN:
                return new OAuth2RefreshAccessTokenOperation(
                        clientConfiguration.getClientId(),
                        clientConfiguration.getClientSecret(),
                        mRefreshToken,
                        mOAuth2Provider.getAccessTokenEndpointPath()
                );
            default:
                throw new UnsupportedOperationException(
                    "Unsupported request"
                );
        }
    }

    @Override
    public String buildUri() {
        if (OAuth2GrantType.AUTHORIZATION_CODE != mGrantType) {
            throw new UnsupportedOperationException(
                "Unsupported grant type. Only " +
                    OAuth2GrantType.AUTHORIZATION_CODE.getValue() + " is supported by this provider"
            );
        }
        OAuth2ClientConfiguration clientConfiguration = mOAuth2Provider.getClientConfiguration();
        Uri uri;
        Uri.Builder uriBuilder;
        switch(mRequest) {
            case GET_AUTHORIZATION_CODE:
                uri = Uri.parse(mOAuth2Provider.getAuthorizationServerUri());
                uriBuilder = uri.buildUpon();
                uriBuilder.appendEncodedPath(mOAuth2Provider.getAuthorizationCodeEndpointPath());
                uriBuilder.appendQueryParameter(
                    OAuth2Constants.KEY_RESPONSE_TYPE, OAuth2Constants.OAUTH2_RESPONSE_TYPE_CODE
                );
                uriBuilder.appendQueryParameter(
                    OAuth2Constants.KEY_REDIRECT_URI, clientConfiguration.getRedirectUri()
                );
                uriBuilder.appendQueryParameter(
                    OAuth2Constants.KEY_CLIENT_ID, clientConfiguration.getClientId()
                );

                uri = uriBuilder.build();
                return uri.toString();

            case CREATE_ACCESS_TOKEN:
                uri = Uri.parse(mOAuth2Provider.getAuthorizationServerUri());
                uriBuilder = uri.buildUpon();
                uriBuilder.appendEncodedPath(mOAuth2Provider.getAccessTokenEndpointPath());
                uriBuilder.appendQueryParameter(
                    OAuth2Constants.KEY_RESPONSE_TYPE, OAuth2Constants.OAUTH2_RESPONSE_TYPE_CODE
                );
                uriBuilder.appendQueryParameter(
                    OAuth2Constants.KEY_REDIRECT_URI, clientConfiguration.getRedirectUri()
                );
                uriBuilder.appendQueryParameter(
                    OAuth2Constants.KEY_CLIENT_ID, clientConfiguration.getClientId()
                );

                uri = uriBuilder.build();
                return uri.toString();

            default:
                throw new UnsupportedOperationException(
                    "Unsupported request"
                );
        }
    }

}
