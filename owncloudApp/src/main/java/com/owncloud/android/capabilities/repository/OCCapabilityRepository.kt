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

package com.owncloud.android.capabilities.repository

import androidx.lifecycle.LiveData
import com.owncloud.android.AppExecutors
import com.owncloud.android.NetworkBoundResource
import com.owncloud.android.capabilities.datasource.LocalCapabilitiesDataSource
import com.owncloud.android.capabilities.datasource.RemoteCapabilitiesDataSource
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.lib.resources.status.RemoteCapability
import com.owncloud.android.testing.OpenForTesting
import com.owncloud.android.vo.Resource

@OpenForTesting
class OCCapabilityRepository(
    private val appExecutors: AppExecutors,
    private val localCapabilitiesDataSource: LocalCapabilitiesDataSource,
    private val remoteCapabilitiesDataSource: RemoteCapabilitiesDataSource
) : CapabilityRepository {

    companion object Factory {
        fun create(
            appExecutors: AppExecutors = AppExecutors(),
            localCapabilitiesDataSource: LocalCapabilitiesDataSource,
            remoteCapabilitiesDataSource: RemoteCapabilitiesDataSource
        ): OCCapabilityRepository = OCCapabilityRepository(
            appExecutors,
            localCapabilitiesDataSource,
            remoteCapabilitiesDataSource
        )
    }

    override fun getCapabilityForAccountAsLiveData(
        accountName: String,
        shouldFetchFromNetwork: Boolean
    ): LiveData<Resource<OCCapability>> =
        object : NetworkBoundResource<OCCapability, RemoteCapability>(appExecutors) {
            override fun saveCallResult(item: RemoteCapability) {
                item.accountName = accountName
                localCapabilitiesDataSource.insert(listOf(OCCapability.fromRemoteCapability(item)))
            }

            override fun shouldFetchFromNetwork(data: OCCapability?) = shouldFetchFromNetwork

            override fun loadFromDb(): LiveData<OCCapability> =
                localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(accountName)

            override fun createCall() = remoteCapabilitiesDataSource.getCapabilities()

        }.asLiveData()

    override fun getStoredCapabilityForAccount(
        accountName: String
    ): OCCapability =
        localCapabilitiesDataSource.getCapabilityForAccount(accountName)
}
