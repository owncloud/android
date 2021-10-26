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

import com.owncloud.android.presentation.manager.AvatarManager
import com.owncloud.android.providers.AccountProvider
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.LogsProvider
import com.owncloud.android.providers.OCContextProvider
import com.owncloud.android.providers.WorkManagerProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val commonModule = module {

    single { AvatarManager() }
    single { CoroutinesDispatcherProvider() }
    factory<ContextProvider> { OCContextProvider(androidContext()) }
    single { LogsProvider(get()) }
    single { WorkManagerProvider(androidContext()) }
    single { AccountProvider(androidContext()) }
}
