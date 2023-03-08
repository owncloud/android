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
package com.owncloud.android.data.oauth.repository

import com.owncloud.android.data.oauth.datasources.RemoteOAuthDataSource
import com.owncloud.android.data.webfinger.datasources.RemoteWebFingerDatasource
import com.owncloud.android.domain.authentication.oauth.OAuthRepository
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationRequest
import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration
import com.owncloud.android.domain.authentication.oauth.model.TokenRequest
import com.owncloud.android.domain.authentication.oauth.model.TokenResponse
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.webfinger.model.WebFingerRel
import timber.log.Timber

class OCOAuthRepository(
    private val oidcRemoteOAuthDataSource: RemoteOAuthDataSource,
    private val remoteWebFingerDatasource: RemoteWebFingerDatasource,
) : OAuthRepository {

    override fun performOIDCDiscovery(baseUrl: String): OIDCServerConfiguration {
        // First, we will try to retrieve the OpenID Connect issuer from webfinger.
        // https://openid.net/specs/openid-connect-discovery-1_0.html#IssuerDiscovery
        val oidcIssuerFromWebFinger: String? = try {
            remoteWebFingerDatasource.getInstancesFromWebFinger(
                lookupServer = baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                username = baseUrl
            ).firstOrNull()
        } catch (fileNotFoundException: FileNotFoundException) {
            Timber.e(fileNotFoundException, "Could not retrieve oidc issuer from WebFinger")
            null
        }

        // If oidcIssuer was not retrieved from webfinger, perform oidc discovery against the base url
        val oidcIssuer = if (oidcIssuerFromWebFinger.isNullOrBlank()) baseUrl else oidcIssuerFromWebFinger
        Timber.d("OIDC discovery will be done against $oidcIssuer")
        return oidcRemoteOAuthDataSource.performOIDCDiscovery(oidcIssuer)
    }

    override fun performTokenRequest(tokenRequest: TokenRequest): TokenResponse =
        oidcRemoteOAuthDataSource.performTokenRequest(tokenRequest)

    override fun registerClient(clientRegistrationRequest: ClientRegistrationRequest): ClientRegistrationInfo =
        oidcRemoteOAuthDataSource.registerClient(clientRegistrationRequest)
}
