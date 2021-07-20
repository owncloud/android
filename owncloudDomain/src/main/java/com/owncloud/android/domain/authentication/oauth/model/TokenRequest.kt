/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.domain.authentication.oauth.model

sealed class TokenRequest(
    val baseUrl: String,
    val tokenEndpoint: String,
    val clientAuth: String,
    val grantType: String
) {
    class AccessToken(
        baseUrl: String,
        tokenEndpoint: String,
        clientAuth: String,
        val authorizationCode: String,
        val redirectUri: String,
        val codeVerifier: String
    ) : TokenRequest(baseUrl, tokenEndpoint, clientAuth, GrantType.ACCESS_TOKEN.string)

    class RefreshToken(
        baseUrl: String,
        tokenEndpoint: String,
        clientAuth: String,
        val refreshToken: String? = null
    ) : TokenRequest(baseUrl, tokenEndpoint, clientAuth, GrantType.REFRESH_TOKEN.string)

    enum class GrantType(val string: String) {
        /** Request access token. [More info](https://tools.ietf.org/html/rfc6749#section-4.1.3) */
        ACCESS_TOKEN("authorization_code"),

        /** Refresh access token. [More info](https://tools.ietf.org/html/rfc6749#section-6) */
        REFRESH_TOKEN("refresh_token")
    }
}
