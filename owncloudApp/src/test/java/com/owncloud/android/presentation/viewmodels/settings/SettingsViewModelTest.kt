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

import android.content.Intent
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
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
class SettingsViewModelTest : ViewModelTest() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk()

        settingsViewModel = SettingsViewModel(
            preferencesProvider
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `is pattern set - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any())} returns true

        val patternSet = settingsViewModel.isPatternSet()

        assertTrue(patternSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `is pattern set - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any())} returns false

        val patternSet = settingsViewModel.isPatternSet()

        assertFalse(patternSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `handle enable passcode - ok`() {
        val data: Intent = mockk()
        val passcode = "1111"

        every { data.getStringExtra(any()) } returns passcode
        every { preferencesProvider.putString(any(), any()) } returns Unit
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val passcodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertTrue(passcodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
            passcode.length
            for (i in 1..4) {
                preferencesProvider.putString(PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                    passcode.substring(i - 1, i))
            }
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        }
    }

    @Test
    fun `handle enable passcode - ko - data intent is null`() {
        val passcodeEnableOk = settingsViewModel.handleEnablePasscode(null)

        assertFalse(passcodeEnableOk)
    }

    @Test
    fun `handle enable passcode - ko - passcode is null`() {
        val data: Intent = mockk()

        every { data.getStringExtra(any())} returns null

        val passcodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertFalse(passcodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
        }
    }

    @Test
    fun `handle enable passcode - ko - passcode has length 3`() {
        val data: Intent = mockk()
        val passcode = "111"

        every { data.getStringExtra(any()) } returns passcode
        every { preferencesProvider.putString(any(), any()) } returns Unit
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val passcodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertFalse(passcodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
            passcode.length
        }
    }

    @Test
    fun `handle enable passcode - ko - passcode has length 5`() {
        val data: Intent = mockk()
        val passcode = "11111"

        every { data.getStringExtra(any()) } returns passcode
        every { preferencesProvider.putString(any(), any()) } returns Unit
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val passcodeEnableOk = settingsViewModel.handleEnablePasscode(data)

        assertFalse(passcodeEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PassCodeActivity.KEY_PASSCODE)
            passcode.length
        }
    }

    @Test
    fun `handle disable passcode - ok`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns true
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val passcodeDisableOk = settingsViewModel.handleDisablePasscode(data)

        assertTrue(passcodeDisableOk)

        verify(exactly = 1) {
            data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `handle disable passcode - ko - data intent is null`() {
        val passcodeDisableOk = settingsViewModel.handleDisablePasscode(null)

        assertFalse(passcodeDisableOk)
    }

    @Test
    fun `handle disable passcode - ko - key check result is false`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns false

        val passcodeDisableOk = settingsViewModel.handleDisablePasscode(data)

        assertFalse(passcodeDisableOk)

        verify(exactly = 1) {
            data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)
        }
    }

    @Test
    fun `is passcode set - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any())} returns true

        val passcodeSet = settingsViewModel.isPasscodeSet()

        assertTrue(passcodeSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `is passcode set - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any())} returns false

        val passcodeSet = settingsViewModel.isPasscodeSet()

        assertFalse(passcodeSet)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `handle enable pattern - ok`() {
        val data: Intent = mockk()
        val pattern = "pattern"

        every { data.getStringExtra(any()) } returns pattern
        every { preferencesProvider.putString(any(), any()) } returns Unit
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val patternEnableOk = settingsViewModel.handleEnablePattern(data)

        assertTrue(patternEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PatternLockActivity.KEY_PATTERN)
            preferencesProvider.putString(PatternLockActivity.KEY_PATTERN, pattern)
            preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true)
        }
    }

    @Test
    fun `handle enable pattern - ko - data intent is null`() {
        val patternEnableOk = settingsViewModel.handleEnablePattern(null)

        assertFalse(patternEnableOk)
    }

    @Test
    fun `handle enable pattern - ko - pattern is null`() {
        val data: Intent = mockk()

        every { data.getStringExtra(any())} returns null

        val patternEnableOk = settingsViewModel.handleEnablePattern(data)

        assertFalse(patternEnableOk)

        verify(exactly = 1) {
            data.getStringExtra(PatternLockActivity.KEY_PATTERN)
        }
    }

    @Test
    fun `handle disable pattern - ok`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns true
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val patternDisableOk = settingsViewModel.handleDisablePattern(data)

        assertTrue(patternDisableOk)

        verify(exactly = 1) {
            data.getBooleanExtra(PatternLockActivity.KEY_CHECK_RESULT, false)
            preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `handle disable pattern - ko - data intent is null`() {
        val patternDisableOk = settingsViewModel.handleDisablePattern(null)

        assertFalse(patternDisableOk)
    }

    @Test
    fun `handle disable pattern - ko - key check result is false`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns false

        val patternDisableOk = settingsViewModel.handleDisablePattern(data)

        assertFalse(patternDisableOk)

        verify(exactly = 1) {
            data.getBooleanExtra(PatternLockActivity.KEY_CHECK_RESULT, false)
        }
    }

    @Test
    fun `set pref touches with other visible windows - ok - true`() {
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        settingsViewModel.setPrefTouchesWithOtherVisibleWindows(true)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, true)
        }
    }

    @Test
    fun `set pref touches with other visible windows - ok - false`() {
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        settingsViewModel.setPrefTouchesWithOtherVisibleWindows(false)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, false)
        }
    }

}
