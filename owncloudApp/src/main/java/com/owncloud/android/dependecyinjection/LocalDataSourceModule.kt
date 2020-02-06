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

import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.implementation.OCLocalCapabilitiesDataSource
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCLocalShareDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val localDataSourceModule = module {
    single { OwncloudDatabase.getDatabase(androidContext()).capabilityDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).shareDao() }

    factory<LocalCapabilitiesDataSource> {
        OCLocalCapabilitiesDataSource(
            get(),
            get()
        )
    }
    factory<LocalShareDataSource> {
        OCLocalShareDataSource(
            get(),
            get()
        )
    }
}
