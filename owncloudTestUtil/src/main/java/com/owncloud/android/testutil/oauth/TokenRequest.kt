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

import com.owncloud.android.domain.authentication.oauth.model.TokenRequest
import com.owncloud.android.testutil.OC_BASE_URL
import com.owncloud.android.testutil.OC_CLIENT_AUTH
import com.owncloud.android.testutil.OC_REDIRECT_URI
import com.owncloud.android.testutil.OC_REFRESH_TOKEN
import com.owncloud.android.testutil.OC_TOKEN_ENDPOINT

val OC_TOKEN_REQUEST_REFRESH = TokenRequest.RefreshToken(
    baseUrl = OC_BASE_URL,
    tokenEndpoint = OC_TOKEN_ENDPOINT,
    clientAuth = OC_CLIENT_AUTH,
    refreshToken = OC_REFRESH_TOKEN
)

val OC_TOKEN_REQUEST_ACCESS = TokenRequest.AccessToken(
    baseUrl = OC_BASE_URL,
    tokenEndpoint = OC_TOKEN_ENDPOINT,
    clientAuth = OC_CLIENT_AUTH,
    authorizationCode = "4uth0r1z4t10nC0d3",
    redirectUri = OC_REDIRECT_URI,
    codeVerifier = "A high-entropy cryptographic random STRING using the unreserved characters"
)
