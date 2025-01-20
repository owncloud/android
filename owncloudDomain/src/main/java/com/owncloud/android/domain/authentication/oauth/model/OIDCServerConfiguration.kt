/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

data class OIDCServerConfiguration(
    val authorizationEndpoint: String,
    val checkSessionIframe: String?,
    val endSessionEndpoint: String?,
    val issuer: String,
    val registrationEndpoint: String?,
    val responseTypesSupported: List<String>?, // To do: provisional, remove nullability ASAP
    val scopesSupported: List<String>?,
    val tokenEndpoint: String,
    val tokenEndpointAuthMethodsSupported: List<String>?,
    val userInfoEndpoint: String?,
) {
    fun isTokenEndpointAuthMethodSupportedClientSecretPost(): Boolean =
        tokenEndpointAuthMethodsSupported?.any { it == "client_secret_post" } ?: false
}
