/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
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

package com.owncloud.android.testutil

import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION

const val OC_SECURE_BASE_URL = "https://demo.owncloud.com"
const val OC_INSECURE_BASE_URL = "http://demo.owncloud.com"

val OC_SECURE_SERVER_INFO_BASIC_AUTH = ServerInfo.BasicServer(
    baseUrl = OC_SECURE_BASE_URL,
    ownCloudVersion = "10.3.2.1",
)

val OC_INSECURE_SERVER_INFO_BASIC_AUTH = ServerInfo.BasicServer(
    baseUrl = OC_INSECURE_BASE_URL,
    ownCloudVersion = "10.3.2.1",
)

val OC_SECURE_SERVER_INFO_BEARER_AUTH = ServerInfo.OAuth2Server(
    baseUrl = OC_SECURE_BASE_URL,
    ownCloudVersion = "10.3.2.1",
)

val OC_INSECURE_SERVER_INFO_BEARER_AUTH = ServerInfo.OAuth2Server(
    baseUrl = OC_INSECURE_BASE_URL,
    ownCloudVersion = "10.3.2.1",
)

val OC_SECURE_SERVER_INFO_OIDC_AUTH = ServerInfo.OIDCServer(
    baseUrl = OC_SECURE_BASE_URL,
    ownCloudVersion = "10.3.2.1",
    oidcServerConfiguration = OC_OIDC_SERVER_CONFIGURATION
)

const val OC_WEBFINGER_INSTANCE_URL = "WEBFINGER_INSTANCE"

val OC_SECURE_SERVER_INFO_BEARER_AUTH_WEBFINGER_INSTANCE = ServerInfo.OAuth2Server(
    baseUrl = OC_WEBFINGER_INSTANCE_URL,
    ownCloudVersion = "10.3.2.1",
)

val OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE = ServerInfo.OIDCServer(
    baseUrl = OC_SECURE_BASE_URL,
    ownCloudVersion = "10.12",
    oidcServerConfiguration = OC_OIDC_SERVER_CONFIGURATION
)
