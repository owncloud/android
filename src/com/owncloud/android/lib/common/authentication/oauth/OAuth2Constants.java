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

/** 
 * Constant values for OAuth 2 protocol.
 * 
 * Includes required and optional parameter NAMES used in the 'authorization code' grant type.
 */

public class OAuth2Constants {
    
    /// Parameters to send to the Authorization Endpoint
    public static final String KEY_RESPONSE_TYPE = "response_type";
    public static final String KEY_REDIRECT_URI = "redirect_uri";
    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_SCOPE = "scope";
    public static final String KEY_STATE = "state"; 
    
    /// Additional parameters to send to the Token Endpoint
    public static final String KEY_GRANT_TYPE = "grant_type";
    public static final String KEY_CODE = "code";

    // Used to get the Access Token using Refresh Token
    public static final String OAUTH2_REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
    
    /// Parameters received in an OK response from the Token Endpoint 
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_TOKEN_TYPE = "token_type";
    public static final String KEY_EXPIRES_IN = "expires_in";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    
    /// Parameters in an ERROR response
    public static final String KEY_ERROR = "error";
    public static final String KEY_ERROR_DESCRIPTION = "error_description";
    public static final String KEY_ERROR_URI = "error_uri";
    public static final String VALUE_ERROR_ACCESS_DENIED = "access_denied";

    /// Extra not standard
    public static final String KEY_USER_ID = "user_id";

    /// Depends on oauth2 grant type
    public static final String OAUTH2_RESPONSE_TYPE_CODE = "code";
}
