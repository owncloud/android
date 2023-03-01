/**
 * ownCloud Android client application
 *
 * Copyright (C) 2022 ownCloud GmbH.
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
package com.owncloud.android.data.webfinger.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.webfinger.datasources.WebfingerRemoteDatasource
import com.owncloud.android.domain.webfinger.model.WebfingerRel
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory
import com.owncloud.android.lib.resources.webfinger.services.WebfingerService

class OCWebfingerRemoteDatasource(
    private val webfingerService: WebfingerService,
    private val clientManager: ClientManager,
) : WebfingerRemoteDatasource {

    override fun getInstancesFromWebFinger(
        lookupServer: String,
        rel: WebfingerRel,
        username: String
    ): List<String> {
        val ownCloudClient = clientManager.getClientForAnonymousCredentials(lookupServer, false)

        return executeRemoteOperation {
            webfingerService.getInstancesFromWebfinger(
                lookupServer = lookupServer,
                rel = rel.uri,
                username = username,
                client = ownCloudClient
            )
        }
    }

    override fun getInstancesFromAuthenticatedWebfinger(
        lookupServer: String,
        rel: WebfingerRel,
        username: String,
        accessToken: String,
    ): List<String> {
        val ownCloudCredentials = OwnCloudCredentialsFactory.newBearerCredentials(username, accessToken)

        val ownCloudClient: OwnCloudClient =
            clientManager.getClientForAnonymousCredentials(
                path = lookupServer,
                requiresNewClient = false
            ).apply { credentials = ownCloudCredentials }

        return executeRemoteOperation {
            webfingerService.getInstancesFromWebfinger(
                lookupServer = lookupServer,
                rel = rel.uri,
                username = username,
                client = ownCloudClient
            )
        }
    }
}
