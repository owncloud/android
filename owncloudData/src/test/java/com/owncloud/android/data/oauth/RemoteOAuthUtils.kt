/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.data.oauth

import com.owncloud.android.lib.resources.oauth.params.TokenRequestParams
import com.owncloud.android.lib.resources.oauth.responses.OIDCDiscoveryResponse
import com.owncloud.android.lib.resources.oauth.responses.TokenResponse
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_ACCESS
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_REFRESH
import com.owncloud.android.testutil.oauth.OC_TOKEN_RESPONSE

val OC_REMOTE_OIDC_DISCOVERY_RESPONSE = OIDCDiscoveryResponse(
    authorization_endpoint = OC_OIDC_SERVER_CONFIGURATION.authorization_endpoint,
    check_session_iframe = OC_OIDC_SERVER_CONFIGURATION.check_session_iframe,
    end_session_endpoint = OC_OIDC_SERVER_CONFIGURATION.end_session_endpoint,
    issuer = OC_OIDC_SERVER_CONFIGURATION.issuer,
    registration_endpoint = OC_OIDC_SERVER_CONFIGURATION.registration_endpoint,
    response_types_supported = OC_OIDC_SERVER_CONFIGURATION.response_types_supported,
    scopes_supported = OC_OIDC_SERVER_CONFIGURATION.scopes_supported,
    token_endpoint = OC_OIDC_SERVER_CONFIGURATION.token_endpoint,
    token_endpoint_auth_methods_supported = OC_OIDC_SERVER_CONFIGURATION.token_endpoint_auth_methods_supported,
    userinfo_endpoint = OC_OIDC_SERVER_CONFIGURATION.userinfo_endpoint
)

val OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS = TokenRequestParams.Authorization(
    tokenEndpoint = OC_TOKEN_REQUEST_ACCESS.tokenEndpoint,
    clientAuth = OC_TOKEN_REQUEST_ACCESS.clientAuth,
    grantType = OC_TOKEN_REQUEST_ACCESS.grantType,
    authorizationCode = OC_TOKEN_REQUEST_ACCESS.authorizationCode,
    redirectUri = OC_TOKEN_REQUEST_ACCESS.redirectUri
)

val OC_REMOTE_TOKEN_REQUEST_PARAMS_REFRESH = TokenRequestParams.RefreshToken(
    tokenEndpoint = OC_TOKEN_REQUEST_REFRESH.tokenEndpoint,
    clientAuth = OC_TOKEN_REQUEST_REFRESH.clientAuth,
    grantType = OC_TOKEN_REQUEST_REFRESH.grantType,
    refreshToken = OC_TOKEN_REQUEST_REFRESH.refreshToken
)

val OC_REMOTE_TOKEN_RESPONSE = TokenResponse(
    accessToken = OC_TOKEN_RESPONSE.accessToken,
    expiresIn = OC_TOKEN_RESPONSE.expiresIn,
    refreshToken = OC_TOKEN_RESPONSE.refreshToken,
    tokenType = OC_TOKEN_RESPONSE.tokenType,
    userId = OC_TOKEN_RESPONSE.userId,
    scope = OC_TOKEN_RESPONSE.scope,
    additionalParameters = OC_TOKEN_RESPONSE.additionalParameters
)
