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

data class OIDCServerConfiguration(
    val authorization_endpoint: String,
    val check_session_iframe: String,
    val end_session_endpoint: String,
    val issuer: String,
    val registration_endpoint: String,
    val response_types_supported: List<String>,
    val scopes_supported: List<String>,
    val token_endpoint: String,
    val token_endpoint_auth_methods_supported: List<String>,
    val userinfo_endpoint: String,
)
