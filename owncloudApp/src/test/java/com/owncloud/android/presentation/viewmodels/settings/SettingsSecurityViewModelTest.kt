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
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.presentation.ui.security.PatternActivity
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
class SettingsSecurityViewModelTest : ViewModelTest() {
    private lateinit var securityViewModel: SettingsSecurityViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        securityViewModel = SettingsSecurityViewModel(preferencesProvider)
    }

    @After
    override fun tearDown() {
        super.tearDown()
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
    fun `handle enable pattern - ok`() {
        val data: Intent = mockk()
        val pattern = "pattern"

        every { data.getStringExtra(any()) } returns pattern

        val patternEnableResult = securityViewModel.handleEnablePattern(data)

        assertTrue(patternEnableResult.isSuccess)

        verify(exactly = 1) {
            data.getStringExtra(PatternActivity.KEY_PATTERN)
            preferencesProvider.putString(PatternActivity.KEY_PATTERN, pattern)
            preferencesProvider.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, true)
        }
    }

    @Test
    fun `handle enable pattern - ko - data intent is null`() {
        val patternEnableResult = securityViewModel.handleEnablePattern(null)

        assertTrue(patternEnableResult.isError)
    }

    @Test
    fun `handle enable pattern - ko - pattern is null`() {
        val data: Intent = mockk()

        every { data.getStringExtra(any()) } returns null

        val patternEnableResult = securityViewModel.handleEnablePattern(data)

        assertTrue(patternEnableResult.isError)

        verify(exactly = 1) {
            data.getStringExtra(PatternActivity.KEY_PATTERN)
        }
    }

    @Test
    fun `handle disable pattern - ok`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns true

        val patternDisableResult = securityViewModel.handleDisablePattern(data)

        assertTrue(patternDisableResult.isSuccess)

        verify(exactly = 1) {
            data.getBooleanExtra(PatternActivity.KEY_CHECK_RESULT, false)
            preferencesProvider.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `handle disable pattern - ko - data intent is null`() {
        val patternDisableResult = securityViewModel.handleDisablePattern(null)

        assertTrue(patternDisableResult.isError)
    }

    @Test
    fun `handle disable pattern - ko - key check result is false`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns false

        val patternDisableResult = securityViewModel.handleDisablePattern(data)

        assertTrue(patternDisableResult.isError)

        verify(exactly = 1) {
            data.getBooleanExtra(PatternActivity.KEY_CHECK_RESULT, false)
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

}
