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
import com.owncloud.android.data.oauth.mapper.RemoteOIDCDiscoveryMapper
import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration
import com.owncloud.android.lib.resources.oauth.services.OIDCService

class RemoteOAuthDataSourceImpl(
    private val clientManager: ClientManager,
    private val oidcService: OIDCService,
    private val remoteOIDCDiscoveryMapper: RemoteOIDCDiscoveryMapper
) : RemoteOAuthDataSource {

    override fun performOIDCDiscovery(baseUrl: String): OIDCServerConfiguration {
        val ownCloudClient = clientManager.getClientForUnExistingAccount(baseUrl, false)

        val result = executeRemoteOperation {
            oidcService.getOIDCServerDiscovery(ownCloudClient)
        }

        return remoteOIDCDiscoveryMapper.toModel(result)!!
    }
}
