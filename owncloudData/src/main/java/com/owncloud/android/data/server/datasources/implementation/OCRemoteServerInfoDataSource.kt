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
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.ServerInfoService
import com.owncloud.android.lib.resources.status.OwnCloudVersion

class OCRemoteServerInfoDataSource(
    private val serverInfoService: ServerInfoService
) : RemoteServerInfoDataSource {

    /* Basically, tries to access to the root folder without authorization and analyzes the response.*/
    override fun getAuthenticationMethod(path: String): AuthenticationMethod {
        // Step 1: check whether the root folder exists, following redirections
        var checkPathExistenceResult = serverInfoService.checkPathExistence(path, isUserLogged = false)
        var redirectionLocation = checkPathExistenceResult.redirectedLocation
        while (!redirectionLocation.isNullOrEmpty()) {
            checkPathExistenceResult = serverInfoService.checkPathExistence(redirectionLocation, isUserLogged = false)
            redirectionLocation = checkPathExistenceResult.redirectedLocation
        }

        // Step 2: look for authentication methods
        var authenticationMethod: AuthenticationMethod = AuthenticationMethod.NONE
        if (checkPathExistenceResult.httpCode == HttpConstants.HTTP_UNAUTHORIZED) {
            val authenticateHeaders = checkPathExistenceResult.authenticateHeaders
            if (authenticateHeaders.startsWith(AuthenticationMethod.BASIC_HTTP_AUTH.toString())) {
                authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH
            } else if (authenticateHeaders.startsWith(AuthenticationMethod.BEARER_TOKEN.toString())) {
                authenticationMethod = AuthenticationMethod.BEARER_TOKEN
            }
        }
        return authenticationMethod
    }

    override fun getRemoteStatus(path: String): Pair<OwnCloudVersion, Boolean> {
        val remoteStatusResult = serverInfoService.getRemoteStatus(path)

        val ownCloudVersion = executeRemoteOperation {
            remoteStatusResult
        }

        if(!ownCloudVersion.isServerVersionSupported){
            throw OwncloudVersionNotSupportedException()
        }

        return Pair(ownCloudVersion, remoteStatusResult.code == RemoteOperationResult.ResultCode.OK_SSL)
    }
}
