/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.viewmodels.settings

import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.settings.fragments.SettingsLogsFragment
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.LogsProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsLogsViewModelTest : ViewModelTest() {
    private lateinit var logsViewModel: SettingsLogsViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var logsProvider: LogsProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        logsProvider = mockk(relaxUnitFun = true)

        logsViewModel = SettingsLogsViewModel(
            preferencesProvider,
            logsProvider
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `should log http requests - ok`() {
        logsViewModel.shouldLogHttpRequests(true)

        verify(exactly = 1) {
            logsProvider.shouldLogHttpRequests(true)
        }
    }

    @Test
    fun `set enable logging - ok - true`() {
        logsViewModel.setEnableLogging(true)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, true)
            logsProvider.startLogging()
        }
    }

    @Test
    fun `set enable logging - ok - false`() {
        logsViewModel.setEnableLogging(false)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, false)
            logsProvider.stopLogging()
        }
    }

    @Test
    fun `is enable logging on - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns true

        val enableLoggingOn = logsViewModel.isLoggingEnabled()

        assertTrue(enableLoggingOn)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, false)
        }
    }

    @Test
    fun `is enable logging on - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns false

        val enableLoggingOn = logsViewModel.isLoggingEnabled()

        assertFalse(enableLoggingOn)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, false)
        }
    }

}
