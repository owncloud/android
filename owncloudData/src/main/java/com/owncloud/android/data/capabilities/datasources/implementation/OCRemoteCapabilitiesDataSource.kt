/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.capabilities.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.domain.capabilities.model.OCCapability

class OCRemoteCapabilitiesDataSource(
    private val clientManager: ClientManager,
    private val remoteCapabilityMapper: RemoteCapabilityMapper
) : RemoteCapabilitiesDataSource {

    override fun getCapabilities(
        accountName: String
    ): OCCapability {
        executeRemoteOperation {
            clientManager.getCapabilityService(accountName).getCapabilities()
        }.let { remoteCapability ->
            return remoteCapabilityMapper.toModel(remoteCapability)!!.apply {
                this.accountName = accountName
            }
        }
    }
}
