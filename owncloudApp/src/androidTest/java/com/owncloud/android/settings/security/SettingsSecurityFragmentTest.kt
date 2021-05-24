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

package com.owncloud.android.settings.security

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.R
import com.owncloud.android.authentication.BiometricManager
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment
import com.owncloud.android.presentation.viewmodels.settings.SettingsSecurityViewModel
import com.owncloud.android.ui.activity.BiometricActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.utils.matchers.verifyPreference
import com.owncloud.android.utils.mockIntent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsSecurityFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsSecurityFragment>

    private lateinit var prefPasscode: CheckBoxPreference
    private lateinit var prefPattern: CheckBoxPreference
    private var prefBiometric: CheckBoxPreference? = null
    private lateinit var prefTouchesWithOtherVisibleWindows: CheckBoxPreference

    private lateinit var biometricManager: BiometricManager

    private lateinit var securityViewModel: SettingsSecurityViewModel
    private lateinit var context: Context

    private val passCodeValue = "1111"
    private val patternValue = "1234"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        securityViewModel = mockk(relaxUnitFun = true)
        mockkStatic(BiometricManager::class)
        biometricManager = mockk(relaxUnitFun = true)

        every { BiometricManager.getBiometricManager(any()) } returns biometricManager

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        securityViewModel
                    }
                }
            )
        }

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    private fun launchTest(withBiometrics: Boolean = true) {
        every { biometricManager.isHardwareDetected } returns withBiometrics

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            prefPasscode = fragment.findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE)!!
            prefPattern = fragment.findPreference(PatternLockActivity.PREFERENCE_SET_PATTERN)!!
            prefBiometric = fragment.findPreference(BiometricActivity.PREFERENCE_SET_BIOMETRIC)
            prefTouchesWithOtherVisibleWindows =
                fragment.findPreference(SettingsSecurityFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS)!!
        }
    }

    private fun checkCommonPreferences() {
        prefPasscode.verifyPreference(
            keyPref = PassCodeActivity.PREFERENCE_SET_PASSCODE,
            titlePref = context.getString(R.string.prefs_passcode),
            visible = true,
            enabled = true
        )
        assertFalse(prefPasscode.isChecked)

        prefPattern.verifyPreference(
            keyPref = PatternLockActivity.PREFERENCE_SET_PATTERN,
            titlePref = context.getString(R.string.prefs_pattern),
            visible = true,
            enabled = true
        )
        assertFalse(prefPattern.isChecked)

        prefTouchesWithOtherVisibleWindows.verifyPreference(
            keyPref = SettingsSecurityFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS,
            titlePref = context.getString(R.string.prefs_touches_with_other_visible_windows),
            summaryPref = context.getString(R.string.prefs_touches_with_other_visible_windows_summary),
            visible = true,
            enabled = true
        )
        assertFalse(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    @Test
    fun securityViewDeviceWithBiometrics() {
        launchTest()

        checkCommonPreferences()

        assertNotNull(prefBiometric)
        prefBiometric?.run {
            verifyPreference(
                keyPref = BiometricActivity.PREFERENCE_SET_BIOMETRIC,
                titlePref = context.getString(R.string.prefs_biometric),
                summaryPref = context.getString(R.string.prefs_biometric_summary),
                visible = true,
                enabled = false
            )
            assertFalse(isChecked)
        }
    }

    @Test
    fun securityViewDeviceWithNoBiometrics() {
        launchTest(withBiometrics = false)

        checkCommonPreferences()

        assertNull(prefBiometric)
    }

    @Test
    fun passcodeOpen() {
        every { securityViewModel.isPatternSet() } returns false

        launchTest()

        onView(withText(R.string.prefs_passcode)).perform(click())
        intended(hasComponent(PassCodeActivity::class.java.name))
    }

    @Test
    fun patternOpen() {
        every { securityViewModel.isPasscodeSet() } returns false

        launchTest()

        onView(withText(R.string.prefs_pattern)).perform(click())
        intended(hasComponent(PatternLockActivity::class.java.name))
    }

    @Test
    fun passcodeLockEnabledOk() {
        every { securityViewModel.isPatternSet() } returns false
        every { securityViewModel.handleEnablePasscode(any()) } returns UIResult.Success()

        launchTest()

        mockIntent(
            extras = Pair(PassCodeActivity.KEY_PASSCODE, passCodeValue),
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertTrue(prefPasscode.isChecked)
    }

    @Test
    fun passcodeLockEnabledError() {
        every { securityViewModel.isPatternSet() } returns false
        every { securityViewModel.handleEnablePasscode(any()) } returns UIResult.Error()

        launchTest()

        mockIntent(
            extras = Pair(PassCodeActivity.KEY_PASSCODE, passCodeValue),
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertFalse(prefPasscode.isChecked)
        onView(withText(R.string.pass_code_error_set)).check(matches(isDisplayed()))
    }

    @Test
    fun patternLockEnabledOk() {
        every { securityViewModel.isPasscodeSet() } returns false
        every { securityViewModel.handleEnablePattern(any()) } returns UIResult.Success()

        launchTest()

        mockIntent(
            extras = Pair(PatternLockActivity.KEY_PATTERN, patternValue),
            action = PatternLockActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertTrue(prefPattern.isChecked)
    }

    @Test
    fun patternLockEnabledError() {
        every { securityViewModel.isPasscodeSet() } returns false
        every { securityViewModel.handleEnablePattern(any()) } returns UIResult.Error()

        launchTest()

        mockIntent(
            extras = Pair(PatternLockActivity.KEY_PATTERN, patternValue),
            action = PatternLockActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertFalse(prefPattern.isChecked)
        onView(withText(R.string.pattern_error_set)).check(matches(isDisplayed()))
    }

    @Test
    fun enablePasscodeEnablesBiometricLock() {
        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
    }

    @Test
    fun enablePatternEnablesBiometricLock() {
        launchTest()

        firstEnablePattern()
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
    }

    @Test
    fun onlyOneMethodEnabledPattern() {
        every { securityViewModel.isPatternSet() } returns true

        launchTest()

        firstEnablePattern()
        onView(withText(R.string.prefs_passcode)).perform(click())
        onView(withText(R.string.pattern_already_set)).check(matches(isEnabled()))
    }

    @Test
    fun onlyOneMethodEnabledPasscode() {
        every { securityViewModel.isPasscodeSet() } returns true

        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_pattern)).perform(click())
        onView(withText(R.string.passcode_already_set)).check(matches(isEnabled()))
    }

    @Test
    fun disablePasscodeOk() {
        every { securityViewModel.handleDisablePasscode(any()) } returns UIResult.Success()

        launchTest()

        firstEnablePasscode()
        mockIntent(
            extras = Pair(PassCodeActivity.KEY_CHECK_RESULT, true),
            action = PassCodeActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertFalse(prefPasscode.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(not(isEnabled())))
        assertFalse(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
    }

    @Test
    fun disablePasscodeError() {
        every { securityViewModel.handleDisablePasscode(any()) } returns UIResult.Error()

        launchTest()

        firstEnablePasscode()
        mockIntent(
            extras = Pair(PassCodeActivity.KEY_CHECK_RESULT, true),
            action = PassCodeActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertTrue(prefPasscode.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric!!.isEnabled)
        onView(withText(R.string.pass_code_error_remove)).check(matches(isDisplayed()))
    }

    @Test
    fun disablePatternOk() {
        every { securityViewModel.handleDisablePattern(any()) } returns UIResult.Success()

        launchTest()

        firstEnablePattern()
        mockIntent(
            extras = Pair(PatternLockActivity.KEY_CHECK_RESULT, true),
            action = PatternLockActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertFalse(prefPattern.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(not(isEnabled())))
        assertFalse(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
    }

    @Test
    fun disablePatternError() {
        every { securityViewModel.handleDisablePattern(any()) } returns UIResult.Error()

        launchTest()

        firstEnablePattern()
        mockIntent(
            extras = Pair(PatternLockActivity.KEY_CHECK_RESULT, true),
            action = PatternLockActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertTrue(prefPattern.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric!!.isEnabled)
        onView(withText(R.string.pattern_error_remove)).check(matches(isDisplayed()))
    }

    @Test
    fun enableBiometricLockWithPasscodeEnabled() {
        every { biometricManager.hasEnrolledBiometric() } returns true

        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertTrue(prefBiometric!!.isChecked)
    }

    @Test
    fun enableBiometricLockWithPatternEnabled() {
        every { biometricManager.hasEnrolledBiometric() } returns true

        launchTest()

        firstEnablePattern()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertTrue(prefBiometric!!.isChecked)
    }

    @Test
    fun enableBiometricLockNoEnrolledBiometric() {
        every { biometricManager.hasEnrolledBiometric() } returns false

        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertFalse(prefBiometric!!.isChecked)
        onView(withText(R.string.biometric_not_enrolled)).check(matches(isEnabled()))
    }

    @Test
    fun disableBiometricLock() {
        every { biometricManager.hasEnrolledBiometric() } returns true

        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertFalse(prefBiometric!!.isChecked)
    }

    @Test
    fun touchesDialog() {
        launchTest()

        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.confirmation_touches_with_other_windows_title)).check(matches(isDisplayed()))
        onView(withText(R.string.confirmation_touches_with_other_windows_message)).check(matches(isDisplayed()))
    }

    @Test
    fun touchesEnable() {
        launchTest()

        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        assertTrue(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    @Test
    fun touchesRefuse() {
        launchTest()

        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        assertFalse(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    @Test
    fun touchesDisable() {
        launchTest()

        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        assertFalse(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    private fun firstEnablePasscode() {
        every { securityViewModel.isPatternSet() } returns false
        every { securityViewModel.handleEnablePasscode(any()) } returns UIResult.Success()

        mockIntent(
            extras = Pair(PassCodeActivity.KEY_PASSCODE, passCodeValue),
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
    }

    private fun firstEnablePattern() {
        every { securityViewModel.isPasscodeSet() } returns false
        every { securityViewModel.handleEnablePattern(any()) } returns UIResult.Success()

        mockIntent(
            extras = Pair(PatternLockActivity.KEY_PATTERN, patternValue),
            action = PatternLockActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
    }
}
