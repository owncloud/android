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
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.presentation.ui.settings.fragments.SettingsLogsFragment
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.LogsProvider
import com.owncloud.android.testutil.OC_BASE_URL
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest : ViewModelTest() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var contextProvider: ContextProvider
    private lateinit var logsProvider: LogsProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk()
        contextProvider = mockk()
        logsProvider = mockk()

        settingsViewModel = SettingsViewModel(
            preferencesProvider,
            contextProvider,
            logsProvider
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

        val passcodeEnableResult = settingsViewModel.handleEnablePasscode(data)

        assertTrue(passcodeEnableResult.isSuccess)

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
        val passcodeEnableResult = settingsViewModel.handleEnablePasscode(null)

        assertTrue(passcodeEnableResult.isError)
    }

    @Test
    fun `handle enable passcode - ko - passcode is null`() {
        val data: Intent = mockk()

        every { data.getStringExtra(any())} returns null

        val passcodeEnableResult = settingsViewModel.handleEnablePasscode(data)

        assertTrue(passcodeEnableResult.isError)

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

        val passcodeEnableResult = settingsViewModel.handleEnablePasscode(data)

        assertTrue(passcodeEnableResult.isError)

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

        val passcodeEnableResult = settingsViewModel.handleEnablePasscode(data)

        assertTrue(passcodeEnableResult.isError)

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

        val passcodeDisableResult = settingsViewModel.handleDisablePasscode(data)

        assertTrue(passcodeDisableResult.isSuccess)

        verify(exactly = 1) {
            data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        }
    }

    @Test
    fun `handle disable passcode - ko - data intent is null`() {
        val passcodeDisableResult = settingsViewModel.handleDisablePasscode(null)

        assertTrue(passcodeDisableResult.isError)
    }

    @Test
    fun `handle disable passcode - ko - key check result is false`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns false

        val passcodeDisableResult = settingsViewModel.handleDisablePasscode(data)

        assertTrue(passcodeDisableResult.isError)

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

        val patternEnableResult = settingsViewModel.handleEnablePattern(data)

        assertTrue(patternEnableResult.isSuccess)

        verify(exactly = 1) {
            data.getStringExtra(PatternLockActivity.KEY_PATTERN)
            preferencesProvider.putString(PatternLockActivity.KEY_PATTERN, pattern)
            preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true)
        }
    }

    @Test
    fun `handle enable pattern - ko - data intent is null`() {
        val patternEnableResult = settingsViewModel.handleEnablePattern(null)

        assertTrue(patternEnableResult.isError)
    }

    @Test
    fun `handle enable pattern - ko - pattern is null`() {
        val data: Intent = mockk()

        every { data.getStringExtra(any())} returns null

        val patternEnableResult = settingsViewModel.handleEnablePattern(data)

        assertTrue(patternEnableResult.isError)

        verify(exactly = 1) {
            data.getStringExtra(PatternLockActivity.KEY_PATTERN)
        }
    }

    @Test
    fun `handle disable pattern - ok`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns true
        every { preferencesProvider.putBoolean(any(), any()) } returns Unit

        val patternDisableResult = settingsViewModel.handleDisablePattern(data)

        assertTrue(patternDisableResult.isSuccess)

        verify(exactly = 1) {
            data.getBooleanExtra(PatternLockActivity.KEY_CHECK_RESULT, false)
            preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `handle disable pattern - ko - data intent is null`() {
        val patternDisableResult = settingsViewModel.handleDisablePattern(null)

        assertTrue(patternDisableResult.isError)
    }

    @Test
    fun `handle disable pattern - ko - key check result is false`() {
        val data: Intent = mockk()

        every { data.getBooleanExtra(any(), any()) } returns false

        val patternDisableResult = settingsViewModel.handleDisablePattern(data)

        assertTrue(patternDisableResult.isError)

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

    @Test
    fun `is help enabled - ok - true`() {
        every { contextProvider.getBoolean(any())} returns true

        val helpEnabled = settingsViewModel.isHelpEnabled()

        assertTrue(helpEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.help_enabled)
        }
    }

    @Test
    fun `is help enabled - ok - false`() {
        every { contextProvider.getBoolean(any())} returns false

        val helpEnabled = settingsViewModel.isHelpEnabled()

        assertFalse(helpEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.help_enabled)
        }
    }

    @Test
    fun `get help url - ok`() {
        every { contextProvider.getString(any())} returns OC_BASE_URL

        val helpUrl = settingsViewModel.getHelpUrl()

        assertEquals(OC_BASE_URL, helpUrl)

        verify(exactly = 1) {
            contextProvider.getString(R.string.url_help)
        }
    }

   @Test
    fun `is sync enabled - ok - true`() {
        every { contextProvider.getBoolean(any())} returns true

        val syncEnabled = settingsViewModel.isSyncEnabled()

        assertTrue(syncEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.sync_calendar_contacts_enabled)
        }
    }

    @Test
    fun `get sync url - ok`() {
        every { contextProvider.getString(any())} returns OC_BASE_URL

        val syncUrl = settingsViewModel.getSyncUrl()

        assertEquals(OC_BASE_URL, syncUrl)

        verify(exactly = 1) {
            contextProvider.getString(R.string.url_sync_calendar_contacts)
        }
    }

    @Test
    fun `is sync enabled - ok - false`() {
        every { contextProvider.getBoolean(any())} returns false

        val syncEnabled = settingsViewModel.isSyncEnabled()

        assertFalse(syncEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.sync_calendar_contacts_enabled)
        }
    }

    @Test
    fun `is recommend enabled - ok - true`() {
        every { contextProvider.getBoolean(any())} returns true

        val recommendEnabled = settingsViewModel.isRecommendEnabled()

        assertTrue(recommendEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.recommend_enabled)
        }
    }

    @Test
    fun `is recommend enabled - ok - false`() {
        every { contextProvider.getBoolean(any())} returns false

        val recommendEnabled = settingsViewModel.isRecommendEnabled()

        assertFalse(recommendEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.recommend_enabled)
        }
    }

    @Test
    fun `is feedback enabled - ok - true`() {
        every { contextProvider.getBoolean(any())} returns true

        val feedbackEnabled = settingsViewModel.isFeedbackEnabled()

        assertTrue(feedbackEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.feedback_enabled)
        }
    }

    @Test
    fun `is feedback enabled - ok - false`() {
        every { contextProvider.getBoolean(any())} returns false

        val feedbackEnabled = settingsViewModel.isFeedbackEnabled()

        assertFalse(feedbackEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.feedback_enabled)
        }
    }

    @Test
    fun `is privacy policy enabled - ok - true`() {
        every { contextProvider.getBoolean(any())} returns true

        val privacyPolicyEnabled = settingsViewModel.isPrivacyPolicyEnabled()

        assertTrue(privacyPolicyEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.privacy_policy_enabled)
        }
    }

    @Test
    fun `is privacy policy enabled - ok - false`() {
        every { contextProvider.getBoolean(any())} returns false

        val privacyPolicyEnabled = settingsViewModel.isPrivacyPolicyEnabled()

        assertFalse(privacyPolicyEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.privacy_policy_enabled)
        }
    }

    @Test
    fun `is imprint enabled - ok - true`() {
        every { contextProvider.getBoolean(any())} returns true

        val imprintEnabled = settingsViewModel.isImprintEnabled()

        assertTrue(imprintEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.imprint_enabled)
        }
    }

    @Test
    fun `is imprint enabled - ok - false`() {
        every { contextProvider.getBoolean(any())} returns false

        val imprintEnabled = settingsViewModel.isImprintEnabled()

        assertFalse(imprintEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.imprint_enabled)
        }
    }

    @Test
    fun `get imprint url - ok`() {
        every { contextProvider.getString(any())} returns OC_BASE_URL

        val imprintUrl = settingsViewModel.getImprintUrl()

        assertEquals(OC_BASE_URL, imprintUrl)

        verify(exactly = 1) {
            contextProvider.getString(R.string.url_imprint)
        }
    }

    @Test
    fun `should log http requests - ok`() {
        every { logsProvider.shouldLogHttpRequests(any())} returns Unit

        settingsViewModel.shouldLogHttpRequests(true)

        verify(exactly = 1) {
            logsProvider.shouldLogHttpRequests(true)
        }
    }

    @Test
    fun `set enable logging - ok`() {
        every { preferencesProvider.putBoolean(any(),any())} returns Unit

        settingsViewModel.setEnableLogging(true)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, true)
        }
    }

    @Test
    fun `is enable logging on - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any())} returns true

        val enableLoggingOn = settingsViewModel.isLoggingEnabled()

        assertTrue(enableLoggingOn)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, false)
        }
    }

    @Test
    fun `is enable logging on - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any())} returns false

        val enableLoggingOn = settingsViewModel.isLoggingEnabled()

        assertFalse(enableLoggingOn)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, false)
        }
    }

}
