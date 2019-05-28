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

package com.owncloud.android.capabilities.viewmodel

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.MainApp
import com.owncloud.android.capabilities.datasource.OCLocalCapabilitiesDataSource
import com.owncloud.android.capabilities.datasource.OCRemoteCapabilitiesDataSource
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.repository.CapabilityRepository
import com.owncloud.android.capabilities.repository.OCCapabilityRepository
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.testing.OpenForTesting
import com.owncloud.android.vo.Resource

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */

@OpenForTesting
class OCCapabilityViewModel(
    val account: Account,
    val capabilityRepository: CapabilityRepository = OCCapabilityRepository.create(
        localCapabilitiesDataSource = OCLocalCapabilitiesDataSource(),
        remoteCapabilitiesDataSource = OCRemoteCapabilitiesDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, MainApp.appContext),
                MainApp.appContext
            )
        )
    )
) : ViewModel() {

    fun getCapabilityForAccount(shouldFetchFromNetwork: Boolean = true): LiveData<Resource<OCCapability>> =
        capabilityRepository.loadCapabilityForAccount(account.name, shouldFetchFromNetwork)
}
