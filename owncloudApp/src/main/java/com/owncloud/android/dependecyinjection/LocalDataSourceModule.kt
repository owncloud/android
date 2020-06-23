/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
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

import android.accounts.AccountManager
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.implementation.OCLocalAuthenticationDataSource
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCLocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.OCCapabilityMapper
import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.implementation.OCLocalFileDataSource
import com.owncloud.android.data.files.datasources.mapper.OCFileMapper
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCLocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.OCShareMapper
import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.datasources.implementation.OCLocalUserDataSource
import com.owncloud.android.data.user.datasources.mapper.UserQuotaMapper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val localDataSourceModule = module {
    single { AccountManager.get(androidContext()) }

    single { OwncloudDatabase.getDatabase(androidContext()).capabilityDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).fileDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).shareDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).userDao() }

    factory { OCCapabilityMapper() }
    factory { OCFileMapper() }
    factory { OCShareMapper() }
    factory { UserQuotaMapper() }

    single<SharedPreferencesProvider> { SharedPreferencesProviderImpl(get()) }

    factory<LocalAuthenticationDataSource> { OCLocalAuthenticationDataSource(androidContext(), get(), get(), accountType) }
    factory<LocalCapabilitiesDataSource> { OCLocalCapabilitiesDataSource(get(), get()) }
    factory<LocalFileDataSource> { OCLocalFileDataSource(get(), get()) }
    factory<LocalShareDataSource> { OCLocalShareDataSource(get(), get()) }
    factory<LocalUserDataSource> { OCLocalUserDataSource(get(), get()) }
}
