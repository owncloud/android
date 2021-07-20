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

import com.owncloud.android.lib.resources.oauth.params.ClientRegistrationParams
import com.owncloud.android.lib.resources.oauth.params.TokenRequestParams
import com.owncloud.android.lib.resources.oauth.responses.ClientRegistrationResponse
import com.owncloud.android.lib.resources.oauth.responses.OIDCDiscoveryResponse
import com.owncloud.android.lib.resources.oauth.responses.TokenResponse
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION_REQUEST
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_ACCESS
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_REFRESH
import com.owncloud.android.testutil.oauth.OC_TOKEN_RESPONSE

val OC_REMOTE_OIDC_DISCOVERY_RESPONSE = OIDCDiscoveryResponse(
    authorization_endpoint = OC_OIDC_SERVER_CONFIGURATION.authorizationEndpoint,
    check_session_iframe = OC_OIDC_SERVER_CONFIGURATION.checkSessionIframe,
    end_session_endpoint = OC_OIDC_SERVER_CONFIGURATION.endSessionEndpoint,
    issuer = OC_OIDC_SERVER_CONFIGURATION.issuer,
    registration_endpoint = OC_OIDC_SERVER_CONFIGURATION.registrationEndpoint,
    response_types_supported = OC_OIDC_SERVER_CONFIGURATION.responseTypesSupported,
    scopes_supported = OC_OIDC_SERVER_CONFIGURATION.scopesSupported,
    token_endpoint = OC_OIDC_SERVER_CONFIGURATION.tokenEndpoint,
    token_endpoint_auth_methods_supported = OC_OIDC_SERVER_CONFIGURATION.tokenEndpointAuthMethodsSupported,
    userinfo_endpoint = OC_OIDC_SERVER_CONFIGURATION.userInfoEndpoint
)

val OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS = TokenRequestParams.Authorization(
    tokenEndpoint = OC_TOKEN_REQUEST_ACCESS.tokenEndpoint,
    clientAuth = OC_TOKEN_REQUEST_ACCESS.clientAuth,
    grantType = OC_TOKEN_REQUEST_ACCESS.grantType,
    authorizationCode = OC_TOKEN_REQUEST_ACCESS.authorizationCode,
    redirectUri = OC_TOKEN_REQUEST_ACCESS.redirectUri,
    codeVerifier = OC_TOKEN_REQUEST_ACCESS.codeVerifier
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

val OC_REMOTE_CLIENT_REGISTRATION_PARAMS = ClientRegistrationParams(
    registrationEndpoint = OC_CLIENT_REGISTRATION_REQUEST.registrationEndpoint,
    clientName = OC_CLIENT_REGISTRATION_REQUEST.clientName,
    redirectUris = OC_CLIENT_REGISTRATION_REQUEST.redirectUris,
    tokenEndpointAuthMethod = OC_CLIENT_REGISTRATION_REQUEST.tokenEndpointAuthMethod,
    applicationType = OC_CLIENT_REGISTRATION_REQUEST.applicationType
)

val OC_REMOTE_CLIENT_REGISTRATION_RESPONSE = ClientRegistrationResponse(
    clientId = OC_CLIENT_REGISTRATION.clientId,
    clientSecret = OC_CLIENT_REGISTRATION.clientSecret,
    clientIdIssuedAt = OC_CLIENT_REGISTRATION.clientIdIssuedAt,
    clientSecretExpiration = OC_CLIENT_REGISTRATION.clientSecretExpiration
)
