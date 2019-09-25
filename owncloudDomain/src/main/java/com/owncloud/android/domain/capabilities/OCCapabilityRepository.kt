/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.DataResult
import com.owncloud.android.data.capabilities.CapabilityRepository
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.lib.resources.status.RemoteCapability

class OCCapabilityRepository(
    private val localCapabilitiesDataSource: LocalCapabilitiesDataSource,
    private val remoteCapabilitiesDataSource: RemoteCapabilitiesDataSource
) : CapabilityRepository {

    override fun getCapabilitiesAsLiveData(accountName: String): LiveData<OCCapabilityEntity> {
        return localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(accountName)
    }

    override fun refreshCapabilitiesForAccount(
        accountName: String,
        shouldFetchFromNetwork: Boolean
    ) {
//        remoteCapabilitiesDataSource.getCapabilities().also { remoteOperationResult ->
//            //Error
//            if (!remoteOperationResult.isSuccess) {
//                return DataResult.error(
//                    code = remoteOperationResult.code,
//                    msg = remoteOperationResult.httpPhrase,
//                    exception = remoteOperationResult.exception
//                )
//            }
//
//            // Success
//            val capabilitiesForAccountFromServer = remoteOperationResult.data.apply {
//                this.accountName = accountName
//            }
//
//            localCapabilitiesDataSource.insert(
//                listOf(
//                    OCCapabilityEntity.fromRemoteCapability(
//                        capabilitiesForAccountFromServer
//                    )
//                )
//            )
//
//            return DataResult.success()
//        }
    }
}
