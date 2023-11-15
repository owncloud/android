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

package com.owncloud.android.data.server.repository

import com.owncloud.android.data.oauth.datasources.RemoteOAuthDataSource
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.data.webfinger.datasources.RemoteWebFingerDataSource
import com.owncloud.android.domain.server.ServerInfoRepository
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.webfinger.model.WebFingerRel
import timber.log.Timber

class OCServerInfoRepository(
    private val remoteServerInfoDataSource: RemoteServerInfoDataSource,
    private val webFingerDatasource: RemoteWebFingerDataSource,
    private val oidcRemoteOAuthDataSource: RemoteOAuthDataSource,
) : ServerInfoRepository {

    override fun getServerInfo(path: String, creatingAccount: Boolean): ServerInfo {
        val oidcIssuerFromWebFinger: String? = if (creatingAccount) retrieveOIDCIssuerFromWebFinger(serverUrl = path) else null

        if (oidcIssuerFromWebFinger != null) {
            val openIDConnectServerConfiguration = oidcRemoteOAuthDataSource.performOIDCDiscovery(oidcIssuerFromWebFinger)
            return ServerInfo.OIDCServer(
                ownCloudVersion = "10.12",
                baseUrl = path,
                oidcServerConfiguration = openIDConnectServerConfiguration
            )
        }

        val serverInfo = remoteServerInfoDataSource.getServerInfo(path)

        if (serverInfo is ServerInfo.BasicServer) {
            return serverInfo
        } else {
            // Could be OAuth or OpenID Connect
            val openIDConnectServerConfiguration = try {
                oidcRemoteOAuthDataSource.performOIDCDiscovery(serverInfo.baseUrl)
            } catch (exception: Exception) {
                Timber.d("OIDC discovery not found")
                null
            }

            return if (openIDConnectServerConfiguration != null) {
                ServerInfo.OIDCServer(
                    ownCloudVersion = serverInfo.ownCloudVersion,
                    baseUrl = serverInfo.baseUrl,
                    oidcServerConfiguration = openIDConnectServerConfiguration
                )
            } else {
                ServerInfo.OAuth2Server(
                    ownCloudVersion = serverInfo.ownCloudVersion,
                    baseUrl = serverInfo.baseUrl,
                )
            }
        }
    }

    private fun retrieveOIDCIssuerFromWebFinger(
        serverUrl: String,
    ): String? {
        val oidcIssuer = try {
            webFingerDatasource.getInstancesFromWebFinger(
                lookupServer = serverUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = serverUrl,
            ).firstOrNull()
        } catch (exception: Exception) {
            Timber.d("Cant retrieve the oidc issuer from webfinger.")
            null
        }

        return oidcIssuer
    }
}
