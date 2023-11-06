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

import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.appregistry.datasources.RemoteAppRegistryDataSource
import com.owncloud.android.data.appregistry.datasources.implementation.OCRemoteAppRegistryDataSource
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.implementation.OCRemoteAuthenticationDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.files.datasources.implementation.OCRemoteFileDataSource
import com.owncloud.android.data.oauth.datasources.RemoteOAuthDataSource
import com.owncloud.android.data.oauth.datasources.implementation.OCRemoteOAuthDataSource
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.data.server.datasources.implementation.OCRemoteServerInfoDataSource
import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.mapper.RemoteShareeMapper
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.data.spaces.datasources.RemoteSpacesDataSource
import com.owncloud.android.data.spaces.datasources.implementation.OCRemoteSpacesDataSource
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.data.user.datasources.implementation.OCRemoteUserDataSource
import com.owncloud.android.data.webfinger.datasources.RemoteWebFingerDataSource
import com.owncloud.android.data.webfinger.datasources.implementation.OCRemoteWebFingerDataSource
import com.owncloud.android.lib.common.ConnectionValidator
import com.owncloud.android.lib.resources.oauth.services.OIDCService
import com.owncloud.android.lib.resources.oauth.services.implementation.OCOIDCService
import com.owncloud.android.lib.resources.status.services.ServerInfoService
import com.owncloud.android.lib.resources.status.services.implementation.OCServerInfoService
import com.owncloud.android.lib.resources.webfinger.services.WebFingerService
import com.owncloud.android.lib.resources.webfinger.services.implementation.OCWebFingerService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val remoteDataSourceModule = module {
    single { ConnectionValidator(androidContext(), androidContext().resources.getBoolean(R.bool.clear_cookies_on_validation)) }
    single { ClientManager(get(), get(), androidContext(), MainApp.accountType, get()) }

    singleOf(::OCServerInfoService) bind ServerInfoService::class
    singleOf(::OCOIDCService) bind OIDCService::class
    singleOf(::OCWebFingerService) bind WebFingerService::class

    singleOf(::OCRemoteAppRegistryDataSource) bind RemoteAppRegistryDataSource::class
    singleOf(::OCRemoteAuthenticationDataSource) bind RemoteAuthenticationDataSource::class
    singleOf(::OCRemoteCapabilitiesDataSource) bind RemoteCapabilitiesDataSource::class
    singleOf(::OCRemoteFileDataSource) bind RemoteFileDataSource::class
    singleOf(::OCRemoteOAuthDataSource) bind RemoteOAuthDataSource::class
    singleOf(::OCRemoteServerInfoDataSource) bind RemoteServerInfoDataSource::class
    singleOf(::OCRemoteShareDataSource) bind RemoteShareDataSource::class
    singleOf(::OCRemoteShareeDataSource) bind RemoteShareeDataSource::class
    singleOf(::OCRemoteSpacesDataSource) bind RemoteSpacesDataSource::class
    singleOf(::OCRemoteWebFingerDataSource) bind RemoteWebFingerDataSource::class
    single<RemoteUserDataSource> { OCRemoteUserDataSource(get(), androidContext().resources.getDimension(R.dimen.file_avatar_size).toInt()) }

    factoryOf(::RemoteCapabilityMapper)
    factoryOf(::RemoteShareMapper)
    factoryOf(::RemoteShareeMapper)
}
