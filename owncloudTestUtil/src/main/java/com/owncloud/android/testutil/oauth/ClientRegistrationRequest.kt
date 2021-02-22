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

import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationRequest
import com.owncloud.android.testutil.OC_REDIRECT_URI

val OC_CLIENT_REGISTRATION_REQUEST = ClientRegistrationRequest(
    registrationEndpoint = OC_OIDC_SERVER_CONFIGURATION.registration_endpoint,
    clientName = "Android Client v2.17",
    redirectUris = listOf(OC_REDIRECT_URI),
    tokenEndpointAuthMethod = "client_secret_basic",
    applicationType = "native"
)
