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

public interface OAuth2Provider {

    /**
     * {@link OAuth2RequestBuilder} implementation for this provider.
     *
     * @return      {@link OAuth2RequestBuilder} implementation.
     */
    OAuth2RequestBuilder getOperationBuilder();


    /**
     * Set configuration of the client that will use this {@link OAuth2Provider}
     * @param oAuth2ClientConfiguration     Configuration of the client that will use this {@link OAuth2Provider}
     */
    void setClientConfiguration(OAuth2ClientConfiguration oAuth2ClientConfiguration);

    /**
     * Configuration of the client that is using this {@link OAuth2Provider}
     * return                   Configuration of the client that is usinng this {@link OAuth2Provider}
     */
    OAuth2ClientConfiguration getClientConfiguration();


    /**
     * Set base URI to authorization server.
     *
     * @param authorizationServerUri        Set base URL to authorization server.
     */
    void setAuthorizationServerUri(String authorizationServerUri);

    /**
     * base URI to authorization server.
     *
     * @return      Base URL to authorization server.
     */
    String getAuthorizationServerUri();

}
