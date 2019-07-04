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

package com.owncloud.android.presentation.capabilities

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.capabilities.datasources.OCLocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.capabilities.CapabilityRepository
import com.owncloud.android.domain.capabilities.OCCapabilityRepository
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.data.Resource

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class OCCapabilityViewModel(
    context: Context,
    val account: Account,
    val capabilityRepository: CapabilityRepository = OCCapabilityRepository.create(
        localCapabilitiesDataSource = OCLocalCapabilitiesDataSource(
            context
        ),
        remoteCapabilitiesDataSource = OCRemoteCapabilitiesDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, context),
                context
            )
        )
    )
) : ViewModel() {
    fun getCapabilityForAccountAsLiveData(shouldFetchFromNetwork: Boolean = true): LiveData<Resource<OCCapabilityEntity>> =
        capabilityRepository.getCapabilityForAccountAsLiveData(account.name, shouldFetchFromNetwork)

    fun getStoredCapabilityForAccount(): OCCapabilityEntity =
        capabilityRepository.getStoredCapabilityForAccount(account.name)
}
