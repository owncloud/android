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

package com.owncloud.android.dependecyinjection

import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.implementation.OCRemoteAuthenticationDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.network.OCCapabilityService
import com.owncloud.android.data.server.datasources.RemoteAnonymousDatasource
import com.owncloud.android.data.server.datasources.RemoteServerDataSource
import com.owncloud.android.data.server.datasources.implementation.OCRemoteAnonymousDataSource
import com.owncloud.android.data.server.datasources.implementation.OCRemoteServerDataSource
import com.owncloud.android.data.server.network.OCAnonymousServerService
import com.owncloud.android.data.server.network.OCServerService
import com.owncloud.android.data.sharing.shares.network.OCShareService
import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.network.OCShareeService
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.data.user.datasources.implementation.OCRemoteUserDataSource
import com.owncloud.android.data.user.network.OCUserService
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.resources.server.AnonymousService
import com.owncloud.android.lib.resources.server.ServerService
import com.owncloud.android.lib.resources.shares.ShareService
import com.owncloud.android.lib.resources.shares.ShareeService
import com.owncloud.android.lib.resources.status.CapabilityService
import com.owncloud.android.lib.resources.users.UserService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val remoteDataSourceModule = module {
    single { AccountUtils.getCurrentOwnCloudAccount(androidContext()) }
    single { OwnCloudAccount(get(), androidContext()) }
    single { SingleSessionManager.getDefaultSingleton().getClientFor(get(), androidContext()) }

    single<CapabilityService> { OCCapabilityService(get()) }
    single<ShareService> { OCShareService(get()) }
    single<ShareeService> { OCShareeService(get()) }
    single<UserService> { OCUserService(get()) }
    single<ServerService> { OCServerService(get()) }
    single<AnonymousService>{ OCAnonymousServerService(get())}

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
    factory<RemoteUserDataSource> {
        OCRemoteUserDataSource(
            get(),
            get()
        )
    }
    factory<RemoteServerDataSource> { OCRemoteServerDataSource(get()) }
    factory<RemoteAnonymousDatasource> { OCRemoteAnonymousDataSource() }
    factory<RemoteAuthenticationDataSource> { OCRemoteAuthenticationDataSource(androidContext()) }
}
