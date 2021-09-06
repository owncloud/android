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

package com.owncloud.android.presentation.viewmodels.security

import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.testutil.security.EXAMPLE_PASSCODE_4_DIGITS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PassCodeViewModelTest : ViewModelTest() {
    private lateinit var passCodeViewModel: PassCodeViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var contextProvider: ContextProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        contextProvider = mockk(relaxUnitFun = true)
        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider)
    }

    @Test
    fun `get passcode - ok`() {
        every { preferencesProvider.getString(any(), any()) } returns EXAMPLE_PASSCODE_4_DIGITS

        val getPassCode = passCodeViewModel.getPassCode()

        assertEquals(EXAMPLE_PASSCODE_4_DIGITS, getPassCode)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `set passcode - ok`() {
        passCodeViewModel.setPassCode(EXAMPLE_PASSCODE_4_DIGITS)

        verify(exactly = 1) {
            preferencesProvider.putString(PassCodeActivity.PREFERENCE_PASSCODE, EXAMPLE_PASSCODE_4_DIGITS)
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        }
    }

    @Test
    fun `remove passcode - ok`() {
        passCodeViewModel.removePassCode()

        verify(exactly = 1) {
            preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `check passcode is valid - ok`() {
        every { preferencesProvider.getString(any(), any()) } returns EXAMPLE_PASSCODE_4_DIGITS

        val passCodeDigits: Array<String?> = arrayOf("1", "1", "1", "1")

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCodeDigits)

        assertTrue(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - saved passcode is null`() {
        every { preferencesProvider.getString(any(), any()) } returns null

        val passCodeDigits: Array<String?> = arrayOf("1", "1", "1", "1")

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCodeDigits)

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - saved passcode is empty`() {
        every { preferencesProvider.getString(any(), any()) } returns ""

        val passCodeDigits: Array<String?> = arrayOf("1", "1", "1", "1")

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCodeDigits)

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - different digit`() {
        every { preferencesProvider.getString(any(), any()) } returns EXAMPLE_PASSCODE_4_DIGITS

        val passCodeDigits: Array<String?> = arrayOf("1", "2", "1", "1")

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCodeDigits)

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - null digit`() {
        every { preferencesProvider.getString(any(), any()) } returns EXAMPLE_PASSCODE_4_DIGITS

        val passCodeDigits: Array<String?> = arrayOf("1", null, "1", "1")

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCodeDigits)

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `get number of passcode digits - ok`() {
        val numberDigits = 4

        every { contextProvider.getInt(any()) } returns numberDigits

        val getNumberDigits = passCodeViewModel.getNumberOfPassCodeDigits()

        assertEquals(numberDigits, getNumberDigits)

        verify(exactly = 1) {
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

}
