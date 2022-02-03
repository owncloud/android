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

import android.app.Activity.RESULT_OK
import android.content.Context
import androidx.biometric.BiometricViewModel
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
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
import com.owncloud.android.presentation.ui.security.BiometricActivity
import com.owncloud.android.presentation.ui.security.BiometricManager
import com.owncloud.android.presentation.ui.security.PREFERENCE_LOCK_TIMEOUT
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.presentation.ui.security.PatternActivity
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment.Companion.PREFERENCE_ACCESS_FROM_DOCUMENT_PROVIDER
import com.owncloud.android.presentation.viewmodels.settings.SettingsSecurityViewModel
import com.owncloud.android.testutil.security.OC_PATTERN
import com.owncloud.android.utils.matchers.verifyPreference
import com.owncloud.android.utils.mockIntent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
    private lateinit var prefLockApplication: ListPreference
    private lateinit var prefAccessDocumentProvider: CheckBoxPreference
    private lateinit var prefTouchesWithOtherVisibleWindows: CheckBoxPreference

    private lateinit var securityViewModel: SettingsSecurityViewModel
    private lateinit var biometricViewModel: BiometricViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        securityViewModel = mockk(relaxUnitFun = true)
        biometricViewModel = mockk(relaxUnitFun = true)
        mockkObject(BiometricManager)

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
        every { securityViewModel.isSecurityEnforcedEnabled() } returns false
        every { securityViewModel.getBiometricsState() } returns false

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    private fun launchTest(withBiometrics: Boolean = true) {
        every { BiometricManager.isHardwareDetected() } returns withBiometrics

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            prefPasscode = fragment.findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE)!!
            prefPattern = fragment.findPreference(PatternActivity.PREFERENCE_SET_PATTERN)!!
            prefBiometric = fragment.findPreference(BiometricActivity.PREFERENCE_SET_BIOMETRIC)
            prefLockApplication = fragment.findPreference(PREFERENCE_LOCK_TIMEOUT)!!
            prefAccessDocumentProvider = fragment.findPreference(PREFERENCE_ACCESS_FROM_DOCUMENT_PROVIDER)!!
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
            keyPref = PatternActivity.PREFERENCE_SET_PATTERN,
            titlePref = context.getString(R.string.prefs_pattern),
            visible = true,
            enabled = true
        )
        assertFalse(prefPattern.isChecked)

        prefLockApplication.verifyPreference(
            keyPref = PREFERENCE_LOCK_TIMEOUT,
            titlePref = context.getString(R.string.prefs_lock_application),
            visible = true,
            enabled = false
        )

        prefAccessDocumentProvider.verifyPreference(
            keyPref = PREFERENCE_ACCESS_FROM_DOCUMENT_PROVIDER,
            titlePref = context.getString(R.string.prefs_access_from_document_provider),
            summaryPref = context.getString(R.string.prefs_access_from_document_provider_summary),
            visible = true,
            enabled = false
        )
        assertFalse(prefAccessDocumentProvider.isChecked)

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

        mockIntent(RESULT_OK, PassCodeActivity.ACTION_REQUEST_WITH_RESULT)
        onView(withText(R.string.prefs_passcode)).perform(click())
        intended(hasComponent(PassCodeActivity::class.java.name))
    }

    @Test
    fun patternOpen() {
        every { securityViewModel.isPasscodeSet() } returns false

        launchTest()

        onView(withText(R.string.prefs_pattern)).perform(click())
        intended(hasComponent(PatternActivity::class.java.name))
    }

    @Test
    fun passcodeLockEnabledOk() {
        every { securityViewModel.isPatternSet() } returns false

        launchTest()

        mockIntent(
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertTrue(prefPasscode.isChecked)
    }

    @Test
    fun patternLockEnabledOk() {
        every { securityViewModel.isPasscodeSet() } returns false

        launchTest()

        mockIntent(
            extras = Pair(PatternActivity.PREFERENCE_PATTERN, OC_PATTERN),
            action = PatternActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertTrue(prefPattern.isChecked)
    }

    @Test
    fun enablePasscodeEnablesBiometricLockAndLockApplicationAndAccessFromDocumentProvider() {
        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
        assertTrue(prefLockApplication.isEnabled)
        assertTrue(prefAccessDocumentProvider.isEnabled)
        assertFalse(prefAccessDocumentProvider.isChecked)
    }

    @Test
    fun enablePatternEnablesBiometricLockAndLockApplicationAndAccessFromDocumentProvider() {
        launchTest()

        firstEnablePattern()
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
        assertTrue(prefLockApplication.isEnabled)
        assertTrue(prefAccessDocumentProvider.isEnabled)
        assertFalse(prefAccessDocumentProvider.isChecked)
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
        launchTest()

        firstEnablePasscode()
        mockIntent(
            action = PassCodeActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertFalse(prefPasscode.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(not(isEnabled())))
        assertFalse(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
        assertFalse(prefLockApplication.isEnabled)
        assertFalse(prefAccessDocumentProvider.isEnabled)
        assertFalse(prefAccessDocumentProvider.isChecked)
    }

    @Test
    fun disablePatternOk() {
        launchTest()

        firstEnablePattern()
        mockIntent(
            action = PatternActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertFalse(prefPattern.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(not(isEnabled())))
        assertFalse(prefBiometric!!.isEnabled)
        assertFalse(prefBiometric!!.isChecked)
        assertFalse(prefLockApplication.isEnabled)
        assertFalse(prefAccessDocumentProvider.isEnabled)
        assertFalse(prefAccessDocumentProvider.isChecked)
    }

    @Test
    fun enableBiometricLockWithPasscodeEnabled() {
        every { BiometricManager.hasEnrolledBiometric() } returns true

        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertTrue(prefBiometric!!.isChecked)
    }

    @Test
    fun enableBiometricLockWithPatternEnabled() {
        every { BiometricManager.hasEnrolledBiometric() } returns true

        launchTest()

        firstEnablePattern()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertTrue(prefBiometric!!.isChecked)
    }

    @Test
    fun enableBiometricLockNoEnrolledBiometric() {
        every { BiometricManager.hasEnrolledBiometric() } returns false

        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertFalse(prefBiometric!!.isChecked)
        onView(withText(R.string.biometric_not_enrolled)).check(matches(isEnabled()))
    }

    @Test
    fun disableBiometricLock() {
        every { BiometricManager.hasEnrolledBiometric() } returns true

        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertFalse(prefBiometric!!.isChecked)
    }

    @Test
    fun accessFromDocumentProviderDialog() {
        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_access_from_document_provider)).perform(click())
        onView(withText(R.string.confirmation_access_from_document_provider_title)).check(matches(isDisplayed()))
        onView(withText(R.string.confirmation_access_from_document_provider_message)).check(matches(isDisplayed()))
    }

    @Test
    fun accessFromDocumentProviderEnable() {
        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_access_from_document_provider)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        assertTrue(prefAccessDocumentProvider.isChecked)
    }

    @Test
    fun accessFromDocumentProviderRefuse() {
        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_access_from_document_provider)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        assertFalse(prefAccessDocumentProvider.isChecked)
    }

    @Test
    fun accessFromDocumentProviderDisable() {
        launchTest()

        firstEnablePasscode()
        onView(withText(R.string.prefs_access_from_document_provider)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        onView(withText(R.string.prefs_access_from_document_provider)).perform(click())
        assertFalse(prefAccessDocumentProvider.isChecked)
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

    @Test
    fun passcodeLockNotVisible() {
        every { securityViewModel.isSecurityEnforcedEnabled() } returns true
        launchTest()
        assertFalse(prefPasscode.isVisible)
    }

    @Test
    fun patternLockNotVisible() {
        every { securityViewModel.isSecurityEnforcedEnabled() } returns true
        launchTest()
        assertFalse(prefPattern.isVisible)
    }

    @Test
    fun passcodeLockVisible() {
        launchTest()
        assertTrue(prefPasscode.isVisible)
    }

    @Test
    fun patternLockVisible() {
        launchTest()
        assertTrue(prefPattern.isVisible)
    }

    @Test
    fun checkIfUserEnabledBiometricRecommendation() {
        every { securityViewModel.getBiometricsState() } returns true

        launchTest()

        firstEnablePasscode()

        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric!!.isChecked)
    }

    @Test
    fun checkIfUserNotEnabledBiometricRecommendation() {
        launchTest()

        firstEnablePasscode()

        assertFalse(prefBiometric!!.isChecked)
    }

    private fun firstEnablePasscode() {
        every { securityViewModel.isPatternSet() } returns false

        mockIntent(
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
    }

    private fun firstEnablePattern() {
        every { securityViewModel.isPasscodeSet() } returns false

        mockIntent(
            action = PatternActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
    }
}
