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

package com.owncloud.android.data.server.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.SpecificServiceUnavailableException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.network.WebdavUtils.normalizeProtocolPrefix
import com.owncloud.android.lib.resources.status.RemoteServerInfo
import com.owncloud.android.lib.resources.status.services.ServerInfoService

class OCRemoteServerInfoDataSource(
    private val serverInfoService: ServerInfoService,
    private val clientManager: ClientManager
) : RemoteServerInfoDataSource {

    // Basically, tries to access to the root folder without authorization and analyzes the response.
    fun getAuthenticationMethod(path: String): AuthenticationMethod {
        // Use the same client across the whole login process to keep cookies updated.
        val owncloudClient = clientManager.getClientForAnonymousCredentials(path, false)

        // Step 1: Check whether the root folder exists.
        val checkPathExistenceResult =
            serverInfoService.checkPathExistence(path = path, isUserLoggedIn = false, client = owncloudClient)

        // Step 2: Check if server is available (If server is in maintenance for example, throw exception with specific message)
        if (checkPathExistenceResult.httpCode == HttpConstants.HTTP_SERVICE_UNAVAILABLE) {
            throw SpecificServiceUnavailableException(checkPathExistenceResult.httpPhrase)
        }

        // Step 3: look for authentication methods
        var authenticationMethod = AuthenticationMethod.NONE
        if (checkPathExistenceResult.httpCode == HttpConstants.HTTP_UNAUTHORIZED) {
            val authenticateHeaders = checkPathExistenceResult.authenticateHeaders
            var isBasic = false
            authenticateHeaders.forEach { authenticateHeader ->
                if (authenticateHeader.contains(AuthenticationMethod.BEARER_TOKEN.toString())) {
                    return AuthenticationMethod.BEARER_TOKEN  // Bearer top priority
                } else if (authenticateHeader.contains(AuthenticationMethod.BASIC_HTTP_AUTH.toString())) {
                    isBasic = true
                }
            }

            if (isBasic) {
                authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH
            }
        }

        return authenticationMethod
    }

    fun getRemoteStatus(path: String): RemoteServerInfo {
        val ownCloudClient = clientManager.getClientForAnonymousCredentials(path, true)

        val remoteStatusResult = serverInfoService.getRemoteStatus(path, ownCloudClient)

        val remoteServerInfo = executeRemoteOperation {
            remoteStatusResult
        }

        if (!remoteServerInfo.ownCloudVersion.isServerVersionSupported && !remoteServerInfo.ownCloudVersion.isVersionHidden) {
            throw OwncloudVersionNotSupportedException()
        }

        return remoteServerInfo
    }

    override fun getServerInfo(path: String): ServerInfo {
        // First step: check the status of the server (including its version)
        val remoteServerInfo = getRemoteStatus(path)
        val normalizedProtocolPrefix =
            normalizeProtocolPrefix(remoteServerInfo.baseUrl, remoteServerInfo.isSecureConnection)

        // Second step: get authentication method required by the server
        val authenticationMethod = getAuthenticationMethod(normalizedProtocolPrefix)

        return if (authenticationMethod == AuthenticationMethod.BEARER_TOKEN) {
            ServerInfo.OAuth2Server(
                ownCloudVersion = remoteServerInfo.ownCloudVersion.version,
                baseUrl = normalizedProtocolPrefix
            )
        } else {
            ServerInfo.BasicServer(
                ownCloudVersion = remoteServerInfo.ownCloudVersion.version,
                baseUrl = normalizedProtocolPrefix,
            )
        }
    }
}
