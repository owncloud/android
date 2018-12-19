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

public class OAuth2ClientConfiguration {

    private String mClientId;

    private String mClientSecret;

    private String mRedirectUri;

    public OAuth2ClientConfiguration(String clientId, String clientSecret, String redirectUri) {
        mClientId = (clientId == null) ? "" : clientId;
        mClientSecret = (clientSecret == null) ? "" : clientSecret;
        mRedirectUri = (redirectUri == null) ? "" : redirectUri;
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String clientId) {
        mClientId = (clientId == null) ? "" : clientId;
    }

    public String getClientSecret() {
        return mClientSecret;
    }

    public void setClientSecret(String clientSecret) {
        mClientSecret = (clientSecret == null) ? "" : clientSecret;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.mRedirectUri = (redirectUri == null) ? "" : redirectUri;
    }
}
