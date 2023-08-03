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

import com.owncloud.android.R
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.presentation.security.LockEnforcedType
import com.owncloud.android.presentation.security.isDeviceSecure
import com.owncloud.android.presentation.security.passcode.PassCodeActivity
import com.owncloud.android.presentation.security.pattern.PatternActivity
import com.owncloud.android.presentation.settings.security.SettingsSecurityViewModel
import com.owncloud.android.presentation.settings.security.SettingsSecurityFragment
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.MdmProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsSecurityViewModelTest : ViewModelTest() {
    private lateinit var securityViewModel: SettingsSecurityViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var mdmProvider: MdmProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        mdmProvider = mockk(relaxUnitFun = true)
        securityViewModel = SettingsSecurityViewModel(preferencesProvider, mdmProvider)
        mockkStatic(::isDeviceSecure)
    }

    @Test
    fun `is pattern set - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns true

        val patternSet = securityViewModel.isPatternSet()

        assertTrue(patternSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `is pattern set - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns false

        val patternSet = securityViewModel.isPatternSet()

        assertFalse(patternSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `is passcode set - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns true

        val passcodeSet = securityViewModel.isPasscodeSet()

        assertTrue(passcodeSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `is passcode set - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns false

        val passcodeSet = securityViewModel.isPasscodeSet()

        assertFalse(passcodeSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `set pref lock access from document provider - ok - true`() {
        securityViewModel.setPrefLockAccessDocumentProvider(true)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsSecurityFragment.PREFERENCE_LOCK_ACCESS_FROM_DOCUMENT_PROVIDER, true)
        }
    }

    @Test
    fun `set pref lock access from document provider - ok - false`() {
        securityViewModel.setPrefLockAccessDocumentProvider(false)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsSecurityFragment.PREFERENCE_LOCK_ACCESS_FROM_DOCUMENT_PROVIDER, false)
        }
    }

    @Test
    fun `set pref touches with other visible windows - ok - true`() {
        securityViewModel.setPrefTouchesWithOtherVisibleWindows(true)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsSecurityFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, true)
        }
    }

    @Test
    fun `set pref touches with other visible windows - ok - false`() {
        securityViewModel.setPrefTouchesWithOtherVisibleWindows(false)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsSecurityFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, false)
        }
    }

    @Test
    fun `is security enforced enabled false - ok - device secure device protection lock enforced`() {
        every { isDeviceSecure() } returns true
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns true
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.EITHER_ENFORCED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertTrue(result)

    }

    @Test
    fun `is security enforced enabled false - ok - device not secure device protection no lock enforced`() {
        every { isDeviceSecure() } returns false
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns true
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.DISABLED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertTrue(result)
    }

    @Test
    fun `is security enforced enabled true - ok - device not secure no device protection lock enforced`() {
        every { isDeviceSecure() } returns false
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns false
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.EITHER_ENFORCED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertTrue(result)
    }

    @Test
    fun `is security enforced enabled true - ok - device secure no device protection lock enforced`() {
        every { isDeviceSecure() } returns true
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns false
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.EITHER_ENFORCED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertTrue(result)
    }

    @Test
    fun `is security enforced enabled false - ok - device not secure no device protection no lock enforced`() {
        every { isDeviceSecure() } returns false
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns false
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.DISABLED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertFalse(result)
    }

    @Test
    fun `is security enforced enabled false - ok - device secure no device protection no lock enforced`() {
        every { isDeviceSecure() } returns true
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns false
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.DISABLED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertFalse(result)
    }

    @Test
    fun `is security enforced enabled false - ok - device secure device protection no lock enforced`() {
        every { isDeviceSecure() } returns true
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns true
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.DISABLED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertFalse(result)
    }

    @Test
    fun `is security enforced enabled true - ok - device not secure device protection lock enforced`() {
        every { isDeviceSecure() } returns false
        every { mdmProvider.getBrandingBoolean(any(), R.bool.device_protection) } returns true
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_enforced) } returns LockEnforcedType.EITHER_ENFORCED.ordinal

        val result = securityViewModel.isSecurityEnforcedEnabled()
        assertTrue(result)
    }

    @Test
    fun `is lock delay enforced enabled - ok - true`() {
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_delay_enforced) } returns 1

        val isEnabled = securityViewModel.isLockDelayEnforcedEnabled()
        assertTrue(isEnabled)
    }

    @Test
    fun `is lock delay enforced enabled - ok - false`() {
        every { mdmProvider.getBrandingInteger(any(), R.integer.lock_delay_enforced) } returns 0

        val isEnabled = securityViewModel.isLockDelayEnforcedEnabled()
        assertFalse(isEnabled)
    }
}

