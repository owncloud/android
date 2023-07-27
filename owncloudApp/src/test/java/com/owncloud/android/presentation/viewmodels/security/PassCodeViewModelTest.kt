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
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.presentation.security.PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP
import com.owncloud.android.presentation.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.security.passcode.PassCodeViewModel
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.presentation.security.passcode.PassCodeActivity
import com.owncloud.android.presentation.security.passcode.PassCodeActivity.Companion.PREFERENCE_PASSCODE
import com.owncloud.android.presentation.security.passcode.PassCodeActivity.Companion.PREFERENCE_PASSCODE_D
import com.owncloud.android.presentation.security.passcode.PassCodeActivity.Companion.PREFERENCE_SET_PASSCODE
import com.owncloud.android.presentation.security.passcode.PasscodeAction
import com.owncloud.android.presentation.security.passcode.PasscodeType
import com.owncloud.android.presentation.security.passcode.Status
import com.owncloud.android.presentation.settings.security.SettingsSecurityFragment.Companion.PREFERENCE_LOCK_ATTEMPTS
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
        preferencesProvider = mockk(relaxed = true)
        contextProvider = mockk(relaxed = true)
    }

    private fun launchTest(
        passcodeDigits: Int = OC_PASSCODE_4_DIGITS.length,
        passcode: String? = OC_PASSCODE_4_DIGITS,
        passcodeD: String? = null,
        lockAttempts: Int = 0,
        lastUnlockAttempt: Long = 0
    ) {
        every { contextProvider.getInt(R.integer.passcode_digits) } returns passcodeDigits   //getNumberOfPassCodeDigits()
        every { preferencesProvider.getString(PREFERENCE_PASSCODE, any()) } returns passcode  //getPassCode()
        for (i in 0..4)
            every { preferencesProvider.getString(PREFERENCE_PASSCODE_D + i, null) } returns passcodeD  //loadPinFromOldFormatIfPossible()
        every { preferencesProvider.getInt(PREFERENCE_LOCK_ATTEMPTS, any()) } returns lockAttempts    //getNumberOfAttempts()
        every { preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP, any()) } returns lastUnlockAttempt   //getTimeToUnlockLeft()
    }

    @Test
    fun `on number clicked - ok`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.onNumberClicked(1)

        assertEquals("1", passCodeViewModel.passcode.value)
    }

    @Test
    fun `on number clicked - 4 numbers`() {
        launchTest(passcodeDigits = 0, passcode = null)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(OC_PASSCODE_4_DIGITS, passCodeViewModel.passcode.value)
    }

    @Test
    fun `on number clicked - 3 or more attemps`() {
        launchTest(
            passcodeDigits = 0,
            passcode = null,
            lockAttempts = 3
        )

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.onNumberClicked(1)

        assertEquals(null, passCodeViewModel.passcode.value)
    }

    @Test
    fun `on number clicked - lock time`() {
        launchTest(
            passcodeDigits = 0,
            passcode = null,
            lockAttempts = 3,
            lastUnlockAttempt = SystemClock.elapsedRealtime()
        )

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.onNumberClicked(1)

        assertEquals(null, passCodeViewModel.passcode.value)
    }

    @Test
    fun `process full passcode - check - ok`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(Status(PasscodeAction.CHECK, PasscodeType.OK), passCodeViewModel.status.value)

        verify(exactly = 1) {
            preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, 0)
        }
    }

    @Test
    fun `process full passcode - check - passcode not valid`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.onNumberClicked(2)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(Status(PasscodeAction.CHECK, PasscodeType.ERROR), passCodeViewModel.status.value)

        verify(exactly = 1) {
            preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, any())
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP, SystemClock.elapsedRealtime())
        }
    }

    @Test
    fun `process full passcode - remove - ok`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.REMOVE)

        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(Status(PasscodeAction.REMOVE, PasscodeType.OK), passCodeViewModel.status.value)

        verify(exactly = 1) {
            preferencesProvider.removePreference(PREFERENCE_PASSCODE)
            preferencesProvider.putBoolean(PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `process full passcode - remove - passcode not valid`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.REMOVE)

        passCodeViewModel.onNumberClicked(2)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(Status(PasscodeAction.REMOVE, PasscodeType.ERROR), passCodeViewModel.status.value)
    }

    @Test
    fun `process full passcode - create - no confirm`() {
        launchTest(passcodeDigits = 0, passcode = null)
        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CREATE)

        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(Status(PasscodeAction.CREATE, PasscodeType.NO_CONFIRM), passCodeViewModel.status.value)
    }

    @Test
    fun `process full passcode - create - confirm`() {
        launchTest(passcodeDigits = 0, passcode = null)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CREATE)

        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(Status(PasscodeAction.CREATE, PasscodeType.CONFIRM), passCodeViewModel.status.value)

        verify(exactly = 1) {
            preferencesProvider.putString(PREFERENCE_PASSCODE, any())
            preferencesProvider.putBoolean(PREFERENCE_SET_PASSCODE, true)
        }
    }

    @Test
    fun `process full passcode - create - error`() {
        launchTest(passcodeDigits = 0, passcode = null)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CREATE)

        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        passCodeViewModel.onNumberClicked(2)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)
        passCodeViewModel.onNumberClicked(1)

        assertEquals(Status(PasscodeAction.CREATE, PasscodeType.ERROR), passCodeViewModel.status.value)
    }

    @Test
    fun `get passcode - ok`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val getPassCode = passCodeViewModel.getPassCode()

        assertEquals(OC_PASSCODE_4_DIGITS, getPassCode)

        verify(exactly = 2) {
            preferencesProvider.getString(PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ok`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val passCode = OC_PASSCODE_4_DIGITS

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertTrue(passCodeCheckResult)

        verify(exactly = 2) {
            preferencesProvider.getString(PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - saved passcode is null`() {
        launchTest(passcodeDigits = 0, passcode = null)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val passCode = OC_PASSCODE_4_DIGITS

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertFalse(passCodeCheckResult)

        verify(exactly = 2) {
            preferencesProvider.getString(PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - saved passcode is empty`() {
        launchTest(passcodeDigits = "".length, passcode = "")
        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val passCode = OC_PASSCODE_4_DIGITS

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertFalse(passCodeCheckResult)

        verify(exactly = 2) {
            preferencesProvider.getString(PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - different digit`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val passCode = "1211"

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode)

        assertFalse(passCodeCheckResult)

        verify(exactly = 2) {
            preferencesProvider.getString(PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `check passcode is valid - ko - null digit`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val nullDigit: String? = null
        val passCode: StringBuilder = StringBuilder()
        passCode.append("1")
        passCode.append("1")
        passCode.append(nullDigit)
        passCode.append("1")

        val passCodeCheckResult = passCodeViewModel.checkPassCodeIsValid(passCode.toString())

        assertFalse(passCodeCheckResult)

        verify(exactly = 2) {
            preferencesProvider.getString(PREFERENCE_PASSCODE, any())
        }
    }

    @Test
    fun `get number of passcode digits - ok - digits is equal or greater than 4`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val getNumberDigits = passCodeViewModel.getNumberOfPassCodeDigits()

        assertEquals(OC_PASSCODE_4_DIGITS.length, getNumberDigits)

        verify(exactly = 1) {
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

    @Test
    fun `get number of passcode digits - ok - digits is less than 4`() {
        val numberDigits = 3

        launchTest(passcodeDigits = numberDigits)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val getNumberDigits = passCodeViewModel.getNumberOfPassCodeDigits()

        assertNotEquals(numberDigits, getNumberDigits)
        assertEquals(4, getNumberDigits)

        verify(exactly = 1) {
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

    @Test
    fun `set migration required - ok`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val required = true

        passCodeViewModel.setMigrationRequired(required)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_MIGRATION_REQUIRED, required)
        }
    }

    @Test
    fun `set last unlock timestamp - ok`() {
        launchTest()

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.setLastUnlockTimestamp()

        verify(exactly = 1) {
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, SystemClock.elapsedRealtime())
        }
    }

    @Test
    fun `get number of attempts - ok`() {
        launchTest(lockAttempts = 3)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val numberOfAttempts = passCodeViewModel.getNumberOfAttempts()

        assertEquals(3, numberOfAttempts)

        verify(exactly = 1) {
            preferencesProvider.getInt(PREFERENCE_LOCK_ATTEMPTS, any())
        }
    }

    @Test
    fun `increase number of attempts - ok`() {
        launchTest(lockAttempts = 3)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.increaseNumberOfAttempts()

        verify(exactly = 1) {
            preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, any())
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP, SystemClock.elapsedRealtime())
        }
    }

    @Test
    fun `reset number of attempts - ok`() {
        launchTest(lockAttempts = 3)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        passCodeViewModel.resetNumberOfAttempts()

        verify(exactly = 1) {
            preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, 0)
        }
    }

    @Test
    fun `get time to unlock left - ok`() {
        launchTest(lockAttempts = 3)

        passCodeViewModel = PassCodeViewModel(preferencesProvider, contextProvider, PasscodeAction.CHECK)

        val timeToUnlockLeft = passCodeViewModel.getTimeToUnlockLeft()

        assertEquals(3000, timeToUnlockLeft)
    }
}
