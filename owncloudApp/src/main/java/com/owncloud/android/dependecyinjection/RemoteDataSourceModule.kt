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

package com.owncloud.android.dependecyinjection

import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.network.OCCapabilityService
import com.owncloud.android.data.sharing.shares.network.OCShareService
import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.network.OCShareeService
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.shares.ShareService
import com.owncloud.android.lib.resources.shares.ShareeService
import com.owncloud.android.lib.resources.status.CapabilityService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val remoteDataSourceModule = module {
    single { AccountUtils.getCurrentOwnCloudAccount(androidContext()) }
    single { OwnCloudAccount(get(), androidContext()) }
    single { OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(get(), androidContext()) }

    single<CapabilityService> { OCCapabilityService(get()) }
    single<ShareService> { OCShareService(get()) }
    single<ShareeService> { OCShareeService(get()) }

    factory<RemoteCapabilitiesDataSource> {
        OCRemoteCapabilitiesDataSource(
            get(),
            get()
        )
    }
    factory<RemoteShareDataSource> {
        OCRemoteShareDataSource(
            get(),
            get()
        )
    }
    factory<RemoteShareeDataSource> {
        OCRemoteShareeDataSource(
            get()
        )
    }
}
