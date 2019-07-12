/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.domain.capabilities

import androidx.lifecycle.LiveData
import com.owncloud.android.data.Executors
import com.owncloud.android.data.NetworkBoundResource
import com.owncloud.android.data.DataResult
import com.owncloud.android.data.capabilities.CapabilityRepository
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.lib.resources.status.RemoteCapability

class OCCapabilityRepository(
    private val executors: Executors,
    private val localCapabilitiesDataSource: LocalCapabilitiesDataSource,
    private val remoteCapabilitiesDataSource: RemoteCapabilitiesDataSource
) : CapabilityRepository {

    companion object Factory {
        fun create(
            executors: Executors = Executors(),
            localCapabilitiesDataSource: LocalCapabilitiesDataSource,
            remoteCapabilitiesDataSource: RemoteCapabilitiesDataSource
        ): OCCapabilityRepository =
            OCCapabilityRepository(
                executors,
                localCapabilitiesDataSource,
                remoteCapabilitiesDataSource
            )
    }

    override fun getCapabilityForAccountAsLiveData(
        accountName: String,
        shouldFetchFromNetwork: Boolean
    ): LiveData<DataResult<OCCapabilityEntity>> =
        object : NetworkBoundResource<OCCapabilityEntity, RemoteCapability>(executors) {
            override fun saveCallResult(item: RemoteCapability) {
                item.accountName = accountName
                localCapabilitiesDataSource.insert(listOf(OCCapabilityEntity.fromRemoteCapability(item)))
            }

            override fun shouldFetchFromNetwork(data: OCCapabilityEntity?) = shouldFetchFromNetwork

            override fun loadFromDb(): LiveData<OCCapabilityEntity> =
                localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(accountName)

            override fun createCall() = remoteCapabilitiesDataSource.getCapabilities()

        }.asLiveData()

    override fun getStoredCapabilityForAccount(
        accountName: String
    ): OCCapability =
        localCapabilitiesDataSource.getCapabilityForAccount(accountName)
}
