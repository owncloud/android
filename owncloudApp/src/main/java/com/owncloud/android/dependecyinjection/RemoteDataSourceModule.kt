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
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.implementation.OCRemoteAuthenticationDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.files.datasources.implementation.OCRemoteFileDataSource
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.data.server.datasources.implementation.OCRemoteServerInfoDataSource
import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.data.user.datasources.implementation.OCRemoteUserDataSource
import com.owncloud.android.data.user.datasources.mapper.RemoteUserAvatarMapper
import com.owncloud.android.data.user.datasources.mapper.RemoteUserInfoMapper
import com.owncloud.android.data.user.datasources.mapper.RemoteUserQuotaMapper
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.resources.files.services.FileService
import com.owncloud.android.lib.resources.files.services.implementation.OCFileService
import com.owncloud.android.lib.resources.shares.services.ShareService
import com.owncloud.android.lib.resources.shares.services.ShareeService
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareService
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareeService
import com.owncloud.android.lib.resources.status.services.CapabilityService
import com.owncloud.android.lib.resources.status.services.ServerInfoService
import com.owncloud.android.lib.resources.status.services.implementation.OCCapabilityService
import com.owncloud.android.lib.resources.status.services.implementation.OCServerInfoService
import com.owncloud.android.lib.resources.users.services.UserService
import com.owncloud.android.lib.resources.users.services.implementation.OCUserService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val remoteDataSourceModule = module {
    single { AccountUtils.getCurrentOwnCloudAccount(androidContext()) }
    single { OwnCloudAccount(get(), androidContext()) }
    single { SingleSessionManager.getDefaultSingleton().getClientFor(get(), androidContext()) }

    single<CapabilityService> { OCCapabilityService(get()) }
    single<ShareService> { OCShareService(get()) }
    single<ShareeService> { OCShareeService(get()) }
    single<UserService> { OCUserService(get(), androidContext().resources.getDimension(R.dimen.file_avatar_size).toInt()) }
    single<FileService> { OCFileService(get()) }
    single<ServerInfoService> { OCServerInfoService() }

    factory<RemoteCapabilitiesDataSource> { OCRemoteCapabilitiesDataSource(get(), get()) }
    factory<RemoteShareDataSource> { OCRemoteShareDataSource(get(), get()) }
    factory<RemoteShareeDataSource> { OCRemoteShareeDataSource(get()) }
    factory<RemoteUserDataSource> { OCRemoteUserDataSource(get(), get(), get(), get()) }
    factory<RemoteFileDataSource> { OCRemoteFileDataSource(get()) }
    factory<RemoteServerInfoDataSource> { OCRemoteServerInfoDataSource(get()) }
    factory<RemoteAuthenticationDataSource> { OCRemoteAuthenticationDataSource(androidContext(), get()) }

    factory { RemoteCapabilityMapper() }
    factory { RemoteShareMapper() }
    factory { RemoteUserInfoMapper() }
    factory { RemoteUserQuotaMapper() }
    factory { RemoteUserAvatarMapper() }
}
