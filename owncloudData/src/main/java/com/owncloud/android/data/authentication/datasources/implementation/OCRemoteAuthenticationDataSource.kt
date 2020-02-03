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
package com.owncloud.android.data.authentication.datasources.implementation

import android.content.Context
import android.net.Uri
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory
import com.owncloud.android.lib.resources.server.CheckPathExistenceOperation

class OCRemoteAuthenticationDataSource(private val context: Context) : RemoteAuthenticationDataSource {
    override fun login(serverPath: String, username: String, password: String) {

        val credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password)
        val url: Uri = Uri.parse(serverPath)

        val client: OwnCloudClient =
            OwnCloudClientFactory.createOwnCloudClient(url, context, false).apply { setCredentials(credentials) }

        val operation = CheckPathExistenceOperation("/", true)
        executeRemoteOperation { operation.execute(client) }

        if (operation.wasRedirected()) {
            val redirectionPath = operation.redirectionPath
            client.apply {
                baseUri = Uri.parse(redirectionPath?.lastPermanentLocation)
                setFollowRedirects(true)
            }
        }
    }
}
