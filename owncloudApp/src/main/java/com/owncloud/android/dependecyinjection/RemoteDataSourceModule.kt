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

import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.implementation.OCRemoteAuthenticationDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.files.datasources.implementation.OCRemoteFileDataSource
import com.owncloud.android.data.files.datasources.mapper.RemoteFileMapper
import com.owncloud.android.data.oauth.datasource.RemoteOAuthDataSource
import com.owncloud.android.data.oauth.datasource.impl.RemoteOAuthDataSourceImpl
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.data.server.datasources.implementation.OCRemoteServerInfoDataSource
import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.mapper.RemoteShareeMapper
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.data.user.datasources.implementation.OCRemoteUserDataSource
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.resources.files.services.FileService
import com.owncloud.android.lib.resources.files.services.implementation.OCFileService
import com.owncloud.android.lib.resources.oauth.services.OIDCService
import com.owncloud.android.lib.resources.oauth.services.implementation.OCOIDCService
import com.owncloud.android.lib.resources.shares.services.ShareService
import com.owncloud.android.lib.resources.shares.services.ShareeService
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareService
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareeService
import com.owncloud.android.lib.resources.status.services.CapabilityService
import com.owncloud.android.lib.resources.status.services.ServerInfoService
import com.owncloud.android.lib.resources.status.services.implementation.OCCapabilityService
import com.owncloud.android.lib.resources.status.services.implementation.OCServerInfoService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val remoteDataSourceModule = module {
    single { AccountUtils.getCurrentOwnCloudAccount(androidContext()) }
    single { OwnCloudAccount(get(), androidContext()) }
    single { SingleSessionManager.getDefaultSingleton().getClientFor(get(), androidContext()) }

    single { ClientManager(get(), get(), get()) }

    single<CapabilityService> { OCCapabilityService(get()) }
    single<FileService> { OCFileService(get()) }
    single<ServerInfoService> { OCServerInfoService() }
    single<OIDCService> { OCOIDCService() }
    single<ShareService> { OCShareService(get()) }
    single<ShareeService> { OCShareeService(get()) }

    factory<RemoteAuthenticationDataSource> { OCRemoteAuthenticationDataSource(get()) }
    factory<RemoteCapabilitiesDataSource> { OCRemoteCapabilitiesDataSource(get(), get()) }
    factory<RemoteFileDataSource> { OCRemoteFileDataSource(get(), get()) }
    factory<RemoteOAuthDataSource> { RemoteOAuthDataSourceImpl(get(), get()) }
    factory<RemoteServerInfoDataSource> { OCRemoteServerInfoDataSource(get(), get()) }
    factory<RemoteShareDataSource> { OCRemoteShareDataSource(get(), get()) }
    factory<RemoteShareeDataSource> { OCRemoteShareeDataSource(get(), get()) }
    factory<RemoteUserDataSource> { OCRemoteUserDataSource(get(), androidContext().resources.getDimension(
                R.dimen.file_avatar_size
            ).toInt()
        )
    }

    factory { RemoteCapabilityMapper() }
    factory { RemoteFileMapper() }
    factory { RemoteShareMapper() }
    factory { RemoteShareeMapper() }
}
