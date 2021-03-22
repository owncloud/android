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
package com.owncloud.android.testutil.oauth

import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration

val OC_OIDC_SERVER_CONFIGURATION = OIDCServerConfiguration(
    authorizationEndpoint = "https://owncloud.server/authorize",
    checkSessionIframe = "https://owncloud.server/check-session.html",
    endSessionEndpoint = "https://owncloud.server/endsession",
    issuer = "https://owncloud.server/",
    registrationEndpoint = "https://owncloud.server/register",
    responseTypesSupported = listOf(
        "id_token token",
        "id_token",
        "code id_token",
        "code id_token token"
    ),
    scopesSupported = listOf(
        "openid",
        "offline_access",
        "konnect/raw_sub",
        "profile",
        "email",
        "konnect/uuid"
    ),
    tokenEndpoint = "https://owncloud.server/token",
    tokenEndpointAuthMethodsSupported = listOf(),
    userInfoEndpoint = "https://owncloud.server/userinfo"
)
