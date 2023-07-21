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
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.presentation.security.biometric.BiometricViewModel
import com.owncloud.android.presentation.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.security.passcode.PassCodeActivity
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.testutil.security.OC_PASSCODE_4_DIGITS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class BiometricViewModelTest : ViewModelTest() {
    private lateinit var biometricViewModel: BiometricViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var contextProvider: ContextProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        contextProvider = mockk(relaxUnitFun = true)
        biometricViewModel = BiometricViewModel(preferencesProvider, contextProvider)
    }

    @Test
    fun `set last unlock timestamp - ok`() {
        biometricViewModel.setLastUnlockTimestamp()

        verify(exactly = 1) {
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, any())
        }
    }

    @Test
    fun `should ask for new passcode - ok - true`() {
        every { preferencesProvider.getString(any(), any()) } returns OC_PASSCODE_4_DIGITS
        every { contextProvider.getInt(any()) } returns 6

        val shouldAsk = biometricViewModel.shouldAskForNewPassCode()
        assertTrue(shouldAsk)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

    @Test
    fun `should ask for new passcode - ok - false`() {
        every { preferencesProvider.getString(any(), any()) } returns OC_PASSCODE_4_DIGITS
        every { contextProvider.getInt(any()) } returns 4

        val shouldAsk = biometricViewModel.shouldAskForNewPassCode()
        assertFalse(shouldAsk)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

    @Test
    fun `should ask for new passcode - ko - passcode is null`() {
        every { preferencesProvider.getString(any(), any()) } returns null
        every { contextProvider.getInt(any()) } returns 4

        val shouldAsk = biometricViewModel.shouldAskForNewPassCode()
        assertFalse(shouldAsk)

        verify(exactly = 1) {
            preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, any())
            contextProvider.getInt(R.integer.passcode_digits)
        }
    }

    @Test
    fun `remove passcode - ok`() {
        biometricViewModel.removePassCode()

        verify(exactly = 1) {
            preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }
}
