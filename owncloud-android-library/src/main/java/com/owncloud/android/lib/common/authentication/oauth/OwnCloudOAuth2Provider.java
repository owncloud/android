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

import com.owncloud.android.lib.common.utils.Log_OC;

public class OwnCloudOAuth2Provider implements OAuth2Provider {

    public static final String NAME = OAuth2Provider.class.getName();

    public static final String ACCESS_TOKEN_ENDPOINT_PATH = "index.php/apps/oauth2/api/v1/token";
    private static final String AUTHORIZATION_CODE_ENDPOINT_PATH = "index.php/apps/oauth2/authorize";

    private String mAuthorizationServerUrl = "";
    private String mAccessTokenEndpointPath = ACCESS_TOKEN_ENDPOINT_PATH;
    private String mAuthorizationCodeEndpointPath = AUTHORIZATION_CODE_ENDPOINT_PATH;

    private OAuth2ClientConfiguration mClientConfiguration;

    @Override
    public OAuth2RequestBuilder getOperationBuilder() {
        return new OwnCloudOAuth2RequestBuilder(this);
    }

    @Override
    public void setClientConfiguration(OAuth2ClientConfiguration oAuth2ClientConfiguration) {
        mClientConfiguration = oAuth2ClientConfiguration;
    }

    @Override
    public OAuth2ClientConfiguration getClientConfiguration() {
        return mClientConfiguration;
    }

    @Override
    public void setAuthorizationServerUri(String authorizationServerUri) {
        mAuthorizationServerUrl = authorizationServerUri;
    }

    @Override
    public String getAuthorizationServerUri() {
        return mAuthorizationServerUrl;
    }

    public String getAccessTokenEndpointPath() {
        return mAccessTokenEndpointPath;
    }

    public void setAccessTokenEndpointPath(String accessTokenEndpointPath) {
        if (accessTokenEndpointPath == null || accessTokenEndpointPath.length() <= 0) {
            Log_OC.w(NAME, "Setting invalid access token endpoint path, going on with default");
            mAccessTokenEndpointPath = ACCESS_TOKEN_ENDPOINT_PATH;
        } else {
            mAccessTokenEndpointPath = accessTokenEndpointPath;
        }
    }

    public String getAuthorizationCodeEndpointPath() {
        return mAuthorizationCodeEndpointPath;
    }

    public void setAuthorizationCodeEndpointPath(String authorizationCodeEndpointPath) {
        if (authorizationCodeEndpointPath == null || authorizationCodeEndpointPath.length() <= 0) {
            Log_OC.w(NAME, "Setting invalid authorization code endpoint path, going on with default");
            mAuthorizationCodeEndpointPath = AUTHORIZATION_CODE_ENDPOINT_PATH;
        } else {
            mAuthorizationCodeEndpointPath = authorizationCodeEndpointPath;
        }
    }
}
