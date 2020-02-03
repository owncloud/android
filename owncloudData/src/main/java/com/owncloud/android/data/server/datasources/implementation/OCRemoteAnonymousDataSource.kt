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

import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.server.datasources.RemoteAnonymousDatasource
import com.owncloud.android.data.server.network.OCAnonymousServerService
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.OwnCloudVersion

class OCRemoteAnonymousDataSource : RemoteAnonymousDatasource {
    private lateinit var serverService: OCAnonymousServerService

    override fun checkPathExistence(path: String, checkUserCredentials: Boolean): Boolean {
        serverService = OCAnonymousServerService(path)
        executeRemoteOperation {
            serverService.checkPathExistence(path = path, isUserLogged = checkUserCredentials)
        }.let { return it == Any() }
    }

    /* Basically, tries to access to the root folder without authorization and analyzes the response.*/
    override fun getAuthenticationMethod(path: String): AuthenticationMethod {
        // Step 1: check whether the root folder exists, following redirections
        serverService = OCAnonymousServerService(path)
        var checkPathExistenceResult = serverService.checkPathExistence("", isUserLogged = false)
        var redirectionLocation = checkPathExistenceResult.redirectedLocation
        while (!redirectionLocation.isNullOrEmpty()) {
            serverService = OCAnonymousServerService(redirectionLocation)
            //serverService.client.baseUri = Uri.parse(redirectionLocation)
            checkPathExistenceResult = serverService.checkPathExistence("", isUserLogged = false)
            redirectionLocation = checkPathExistenceResult.redirectedLocation
        }

        // Step 2: look for authentication methods
        var authenticationMethod: AuthenticationMethod = AuthenticationMethod.NONE
        if (checkPathExistenceResult.httpCode == HttpConstants.HTTP_UNAUTHORIZED) {
            val authenticateHeaders = checkPathExistenceResult.authenticateHeaders
            if (authenticateHeaders.contains("basic")) {
                authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH
            } else if (authenticateHeaders.contains("bearer")) {
                authenticationMethod = AuthenticationMethod.BEARER_TOKEN
            }
        }
        return authenticationMethod
    }

    override fun getRemoteStatus(path: String): Pair<OwnCloudVersion, Boolean> {
        serverService = OCAnonymousServerService(path)
        val remoteStatusResult = serverService.getRemoteStatus(path)

        val ownCloudVersion = executeRemoteOperation {
            remoteStatusResult
        }
        return Pair(ownCloudVersion, remoteStatusResult.code == RemoteOperationResult.ResultCode.OK_SSL)
    }
}
