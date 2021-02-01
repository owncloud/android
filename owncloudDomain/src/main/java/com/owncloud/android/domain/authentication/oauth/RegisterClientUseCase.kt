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
package com.owncloud.android.domain.authentication.oauth

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo

class RegisterClientUseCase(
    private val oAuthRepository: OAuthRepository
) : BaseUseCaseWithResult<ClientRegistrationInfo, RegisterClientUseCase.Params>() {

    override fun run(params: Params): ClientRegistrationInfo =
        oAuthRepository.registerClient(
            registrationEndpoint = params.registrationEndpoint,
            clientName = params.clientName,
            redirectUris = params.redirectUris,
            tokenEndpointAuthMethod = params.tokenEndpointAuthMethod,
            applicationType = params.applicationType
        )

    data class Params(
        val registrationEndpoint: String,
        val clientName: String,
        val redirectUris: List<String>,
        val tokenEndpointAuthMethod: String = CLIENT_REGISTRATION_SECRET_BASIC,
        val applicationType: String = CLIENT_REGISTRATION_APPLICATION_TYPE
    )

    companion object {
        /**
         * The client uses HTTP Basic as defined in OAuth 2.0, Section 2.3.1.
         * https://tools.ietf.org/html/rfc7591#section-2.3.1
         * Use this auth method for the moment. We should check if it is allowed in the OIDC Discovery.
         */
        private const val CLIENT_REGISTRATION_SECRET_BASIC = "client_secret_basic"
        private const val CLIENT_REGISTRATION_APPLICATION_TYPE = "native"
    }
}
