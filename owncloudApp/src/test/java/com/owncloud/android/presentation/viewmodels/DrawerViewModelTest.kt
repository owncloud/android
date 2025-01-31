/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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
package com.owncloud.android.presentation.viewmodels

import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.domain.user.usecases.GetStoredQuotaAsStreamUseCase
import com.owncloud.android.domain.user.usecases.GetUserQuotasUseCase
import com.owncloud.android.presentation.common.DrawerViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.usecases.accounts.RemoveAccountUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class DrawerViewModelTest : ViewModelTest() {
    private lateinit var drawerViewModel: DrawerViewModel
    private lateinit var getStoredQuotaAsStreamUseCase: GetStoredQuotaAsStreamUseCase
    private lateinit var removeAccountUseCase: RemoveAccountUseCase
    private lateinit var getUserQuotasUseCase: GetUserQuotasUseCase
    private lateinit var localStorageProvider: LocalStorageProvider

    private lateinit var contextProvider: ContextProvider

    @Before
    fun setUp() {
        contextProvider = mockk()

        every { contextProvider.isConnected() } returns true

        Dispatchers.setMain(testCoroutineDispatcher)
        startKoin {
            allowOverride(override = true)
            modules(
                module {
                    factory {
                        contextProvider
                    }
                })
        }

        getStoredQuotaAsStreamUseCase = mockk()
        removeAccountUseCase = mockk()
        getUserQuotasUseCase = mockk()
        localStorageProvider = mockk()

        testCoroutineDispatcher.pauseDispatcher()

        drawerViewModel = DrawerViewModel(
            getStoredQuotaAsStreamUseCase = getStoredQuotaAsStreamUseCase,
            removeAccountUseCase = removeAccountUseCase,
            getUserQuotasUseCase = getUserQuotasUseCase,
            localStorageProvider = localStorageProvider,
            coroutinesDispatcherProvider = coroutineDispatcherProvider,
            contextProvider = contextProvider,
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        stopKoin()
    }

}
