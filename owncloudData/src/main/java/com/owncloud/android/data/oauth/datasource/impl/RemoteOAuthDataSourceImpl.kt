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

package com.owncloud.android.data.oauth.datasource.impl

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.oauth.datasource.RemoteOAuthDataSource
import com.owncloud.android.data.oauth.mapper.RemoteClientRegistrationInfoMapper
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationRequest
import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration
import com.owncloud.android.domain.authentication.oauth.model.TokenRequest
import com.owncloud.android.domain.authentication.oauth.model.TokenResponse
import com.owncloud.android.lib.resources.oauth.params.ClientRegistrationParams
import com.owncloud.android.lib.resources.oauth.params.TokenRequestParams
import com.owncloud.android.lib.resources.oauth.responses.OIDCDiscoveryResponse
import com.owncloud.android.lib.resources.oauth.services.OIDCService
import com.owncloud.android.lib.resources.oauth.responses.TokenResponse as RemoteTokenResponse

class RemoteOAuthDataSourceImpl(
    private val clientManager: ClientManager,
    private val oidcService: OIDCService,
    private val remoteClientRegistrationInfoMapper: RemoteClientRegistrationInfoMapper
) : RemoteOAuthDataSource {

    override fun performOIDCDiscovery(baseUrl: String): OIDCServerConfiguration {
        val ownCloudClient = clientManager.getClientForUnExistingAccount(baseUrl, false)

        val serverConfiguration = executeRemoteOperation {
            oidcService.getOIDCServerDiscovery(ownCloudClient)
        }

        return serverConfiguration.toModel()
    }

    override fun performTokenRequest(tokenRequest: TokenRequest): TokenResponse {
        val ownCloudClient = clientManager.getClientForUnExistingAccount(tokenRequest.baseUrl, false)

        val tokenResponse = executeRemoteOperation {
            oidcService.performTokenRequest(
                ownCloudClient = ownCloudClient,
                tokenRequest = tokenRequest.toParams()
            )
        }

        return tokenResponse.toModel()
    }

    override fun registerClient(clientRegistrationRequest: ClientRegistrationRequest): ClientRegistrationInfo {
        val ownCloudClient =
            clientManager.getClientForUnExistingAccount(clientRegistrationRequest.registrationEndpoint, false)

        val remoteClientRegistrationInfo = executeRemoteOperation {
            oidcService.registerClientWithRegistrationEndpoint(
                ownCloudClient = ownCloudClient,
                clientRegistrationParams = ClientRegistrationParams(
                    registrationEndpoint = clientRegistrationRequest.registrationEndpoint,
                    clientName = clientRegistrationRequest.clientName,
                    redirectUris = clientRegistrationRequest.redirectUris,
                    tokenEndpointAuthMethod = clientRegistrationRequest.tokenEndpointAuthMethod,
                    applicationType = clientRegistrationRequest.applicationType
                )
            )
        }

        return remoteClientRegistrationInfoMapper.toModel(remoteClientRegistrationInfo)!!
    }

    /**************************************************************************************************************
     ************************************************* Mappers ****************************************************
     **************************************************************************************************************/
    private fun OIDCDiscoveryResponse.toModel(): OIDCServerConfiguration =
        OIDCServerConfiguration(
            authorization_endpoint = this.authorization_endpoint,
            check_session_iframe = this.check_session_iframe,
            end_session_endpoint = this.end_session_endpoint,
            issuer = this.issuer,
            registration_endpoint = this.registration_endpoint,
            response_types_supported = this.response_types_supported,
            scopes_supported = this.scopes_supported,
            token_endpoint = this.token_endpoint,
            token_endpoint_auth_methods_supported = this.token_endpoint_auth_methods_supported,
            userinfo_endpoint = this.userinfo_endpoint
        )

    private fun TokenRequest.toParams(): TokenRequestParams =
        when (this) {
            is TokenRequest.AccessToken ->
                TokenRequestParams.Authorization(
                    tokenEndpoint = this.tokenEndpoint,
                    authorizationCode = this.authorizationCode,
                    grantType = this.grantType,
                    redirectUri = this.redirectUri,
                    clientAuth = this.clientAuth
                )
            is TokenRequest.RefreshToken ->
                TokenRequestParams.RefreshToken(
                    tokenEndpoint = this.tokenEndpoint,
                    grantType = this.grantType,
                    clientAuth = this.clientAuth,
                    refreshToken = this.refreshToken
                )
        }

    private fun RemoteTokenResponse.toModel(): TokenResponse =
        TokenResponse(
            accessToken = this.accessToken,
            expiresIn = this.expiresIn,
            refreshToken = this.refreshToken,
            tokenType = this.tokenType,
            userId = this.userId,
            scope = this.scope,
            additionalParameters = this.additionalParameters
        )
}
