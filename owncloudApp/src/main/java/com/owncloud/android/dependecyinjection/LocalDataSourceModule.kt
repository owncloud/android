/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.MainApp.Companion.dataFolder
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.appregistry.datasources.LocalAppRegistryDataSource
import com.owncloud.android.data.appregistry.datasources.implementation.OCLocalAppRegistryDataSource
import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.implementation.OCLocalAuthenticationDataSource
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCLocalCapabilitiesDataSource
import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.implementation.OCLocalFileDataSource
import com.owncloud.android.data.folderbackup.datasources.LocalFolderBackupDataSource
import com.owncloud.android.data.folderbackup.datasources.implementation.OCLocalFolderBackupDataSource
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCLocalShareDataSource
import com.owncloud.android.data.spaces.datasources.LocalSpacesDataSource
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource
import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.data.providers.QaStorageProvider
import com.owncloud.android.data.providers.ScopedStorageProvider
import com.owncloud.android.data.transfers.datasources.LocalTransferDataSource
import com.owncloud.android.data.transfers.datasources.implementation.OCLocalTransferDataSource
import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.datasources.implementation.OCLocalUserDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val localDataSourceModule = module {
    single { AccountManager.get(androidContext()) }

    single { OwncloudDatabase.getDatabase(androidContext()).appRegistryDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).capabilityDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).fileDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).folderBackUpDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).shareDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).spacesDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).transferDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).userDao() }

    singleOf(::OCSharedPreferencesProvider) bind SharedPreferencesProvider::class
    single<LocalStorageProvider> {
        if (BuildConfig.FLAVOR == MainApp.QA_FLAVOR) {
            QaStorageProvider(dataFolder)
        } else {
            ScopedStorageProvider(dataFolder, androidContext())
        }
    }

    factory<LocalAuthenticationDataSource> { OCLocalAuthenticationDataSource(androidContext(), get(), get(), accountType) }
    factoryOf(::OCLocalFolderBackupDataSource) bind LocalFolderBackupDataSource::class
    factoryOf(::OCLocalAppRegistryDataSource) bind LocalAppRegistryDataSource::class
    factoryOf(::OCLocalCapabilitiesDataSource) bind LocalCapabilitiesDataSource::class
    factoryOf(::OCLocalFileDataSource) bind LocalFileDataSource::class
    factoryOf(::OCLocalShareDataSource) bind LocalShareDataSource::class
    factoryOf(::OCLocalSpacesDataSource) bind LocalSpacesDataSource::class
    factoryOf(::OCLocalTransferDataSource) bind LocalTransferDataSource::class
    factoryOf(::OCLocalUserDataSource) bind LocalUserDataSource::class
}
