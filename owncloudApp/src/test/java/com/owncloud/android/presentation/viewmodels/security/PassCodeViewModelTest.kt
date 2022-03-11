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

import android.os.SystemClock
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.security.PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP
import com.owncloud.android.presentation.ui.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.presentation.ui.security.passcode.PassCodeActivity
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment.Companion.PREFERENCE_LOCK_ATTEMPTS
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.testutil.security.OC_PASSCODE_4_DIGITS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
        every { preferencesProvider.getString(any(), any()) } returns OC_PASSCODE_4_DIGITS

        val getPassCode = passCodeViewModel.getPassCode()

        assertEquals(OC_PASSCODE_4_DIGITS, getPassCode)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `set passcode - ok`() {
        passCodeViewModel.setPassCode(OC_PASSCODE_4_DIGITS)

        verify(exactly = 1) {
            preferencesProvider.putString(PassCodeActivity.PREFERENCE_PASSCODE, OC_PASSCODE_4_DIGITS)
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
        every { preferencesProvider.getString(any(), any()) } returns OC_PASSCODE_4_DIGITS

        val passCode = "1111"

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertTrue(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - saved passcode is null`() {
        every { preferencesProvider.getString(any(), any()) } returns null

        val passCode = "1111"

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - saved passcode is empty`() {
        every { preferencesProvider.getString(any(), any()) } returns ""

        val passCode = "1111"

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - different digit`() {
        every { preferencesProvider.getString(any(), any()) } returns OC_PASSCODE_4_DIGITS

        val passCode = "1211"

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - null digit`() {
        every { preferencesProvider.getString(any(), any()) } returns OC_PASSCODE_4_DIGITS

        val nullDigit: String? = null
        val passCode: StringBuilder = StringBuilder()
        passCode.append("1")
        passCode.append("1")
        passCode.append(nullDigit)
        passCode.append("1")


        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode.toString())

        assertFalse(passCodeCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `get number of passcode digits - ok - digits is equal or greater than 4`() {
        val numberDigits = 4

        every { contextProvider.getInt(any()) } returns numberDigits

        val getNumberDigits = passCodeViewModel.getNumberOfPassCodeDigits()

        assertEquals(numberDigits, getNumberDigits)

        verify(exactly = 1) {
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

    @Test
    fun `get number of passcode digits - ok - digits is less than 4`() {
        val numberDigits = 3

        every { contextProvider.getInt(any()) } returns numberDigits

        val getNumberDigits = passCodeViewModel.getNumberOfPassCodeDigits()

        assertNotEquals(numberDigits, getNumberDigits)
        assertEquals(4, getNumberDigits)

        verify(exactly = 1) {
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

    @Test
    fun `set migration required - ok`() {
        val required = true

        passCodeViewModel.setMigrationRequired(required)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_MIGRATION_REQUIRED, required)
        }
    }

    @Test
    fun `set last unlock timestamp - ok`() {
        passCodeViewModel.setLastUnlockTimestamp()

        verify(exactly = 1) {
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, SystemClock.elapsedRealtime())
        }
    }

    @Test
    fun `get number of attempts - ok`() {
        every { preferencesProvider.getInt(any(), any()) } returns 3

        val numberOfAttempts = passCodeViewModel.getNumberOfAttempts()

        assertEquals(3, numberOfAttempts)

        verify(exactly = 1) {
            preferencesProvider.getInt(PREFERENCE_LOCK_ATTEMPTS, 0)
        }
    }

    @Test
    fun `increase number of attempts - ok`() {
        every { preferencesProvider.getInt(any(), any()) } returns 3

        passCodeViewModel.increaseNumberOfAttempts()

        verify(exactly = 1) {
            preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, any())
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP, SystemClock.elapsedRealtime())
        }
    }

    @Test
    fun `reset number of attempts - ok`() {
        passCodeViewModel.resetNumberOfAttempts()

        verify(exactly = 1) {
            preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, 0)
        }
    }

    @Test
    fun `get time to unlock left - ok`() {
        every { preferencesProvider.getInt(any(), any()) } returns 3
        every { preferencesProvider.getLong(any(), any()) } returns 0

        val timeToUnlockLeft = passCodeViewModel.getTimeToUnlockLeft()

        assertEquals(3000, timeToUnlockLeft)
    }
}
