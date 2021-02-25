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

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceCategory
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
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.ui.activity.BiometricActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.utils.mockIntent
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsFragment>

    private lateinit var categorySecurity: PreferenceCategory
    private lateinit var prefPasscode: CheckBoxPreference
    private lateinit var prefPattern: CheckBoxPreference
    private lateinit var prefBiometric: CheckBoxPreference
    private lateinit var prefTouchesWithOtherVisibleWindows: CheckBoxPreference

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val passCodeValue = "1111"
    private val patternValue = "1234"

    @Before
    fun setUp() {
        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            categorySecurity = fragment.findPreference("security_category")!!
            prefPasscode = fragment.findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE)!!
            prefPattern = fragment.findPreference(PatternLockActivity.PREFERENCE_SET_PATTERN)!!
            prefBiometric = fragment.findPreference(BiometricActivity.PREFERENCE_SET_BIOMETRIC)!!
            prefTouchesWithOtherVisibleWindows =
                fragment.findPreference(SettingsFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS)!!
        }
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun securityView() {
        onView(withText(R.string.prefs_category_security)).check(matches(isDisplayed()))
        assertEquals("security_category", categorySecurity.key)
        assertEquals(context.getString(R.string.prefs_category_security), categorySecurity.title)
        assertEquals(null, categorySecurity.summary)
        assertTrue(categorySecurity.isVisible)

        onView(withText(R.string.prefs_passcode)).check(matches(isDisplayed()))
        assertEquals("set_pincode", prefPasscode.key)
        assertEquals(context.getString(R.string.prefs_passcode), prefPasscode.title)
        assertEquals(null, prefPasscode.summary)
        assertTrue(prefPasscode.isVisible)
        assertTrue(prefPasscode.isEnabled)
        assertFalse(prefPasscode.isChecked)

        onView(withText(R.string.prefs_pattern)).check(matches(isDisplayed()))
        assertEquals("set_pattern", prefPattern.key)
        assertEquals(context.getString(R.string.prefs_pattern), prefPattern.title)
        assertEquals(null, prefPattern.summary)
        assertTrue(prefPattern.isVisible)
        assertTrue(prefPattern.isEnabled)
        assertFalse(prefPattern.isChecked)

        onView(withText(R.string.prefs_biometric)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_biometric_summary)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_biometric)).check(matches(not(isEnabled())))
        assertEquals("set_biometric", prefBiometric.key)
        assertEquals(context.getString(R.string.prefs_biometric), prefBiometric.title)
        assertEquals(context.getString(R.string.prefs_biometric_summary), prefBiometric.summary)
        assertTrue(prefBiometric.isVisible)
        assertFalse(prefBiometric.isEnabled)
        assertFalse(prefBiometric.isChecked)

        onView(withText(R.string.prefs_touches_with_other_visible_windows)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_touches_with_other_visible_windows_summary)).check(matches(isDisplayed()))
        assertEquals("touches_with_other_visible_windows", prefTouchesWithOtherVisibleWindows.key)
        assertEquals(
            context.getString(R.string.prefs_touches_with_other_visible_windows),
            prefTouchesWithOtherVisibleWindows.title
        )
        assertEquals(
            context.getString(R.string.prefs_touches_with_other_visible_windows_summary),
            prefTouchesWithOtherVisibleWindows.summary
        )
        assertTrue(prefTouchesWithOtherVisibleWindows.isVisible)
        assertTrue(prefTouchesWithOtherVisibleWindows.isEnabled)
        assertFalse(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    @Test
    fun passcodeOpen() {
        onView(withText(R.string.prefs_passcode)).perform(click())
        intended(hasComponent(PassCodeActivity::class.java.name))
    }

    @Test
    fun patternOpen() {
        onView(withText(R.string.prefs_pattern)).perform(click())
        intended(hasComponent(PatternLockActivity::class.java.name))
    }

    @Test
    fun passcodeLockEnabled() {
        mockIntent(
            extras = Pair(PassCodeActivity.KEY_PASSCODE, passCodeValue),
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertTrue(prefPasscode.isChecked)
    }

    @Test
    fun patternLockEnabled() {
        mockIntent(
            extras = Pair(PatternLockActivity.KEY_PATTERN, patternValue),
            action = PatternLockActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertTrue(prefPattern.isChecked)
    }

    @Test
    fun enablePasscodeEnablesBiometricLock() {
        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric.isEnabled)
        assertFalse(prefBiometric.isChecked)
    }

    @Test
    fun enablePatternEnablesBiometricLock() {
        firstEnablePattern()
        onView(withText(R.string.prefs_biometric)).check(matches(isEnabled()))
        assertTrue(prefBiometric.isEnabled)
        assertFalse(prefBiometric.isChecked)
    }

    @Test
    fun onlyOneMethodEnabledPattern() {
        firstEnablePattern()
        onView(withText(R.string.prefs_passcode)).perform(click())
        onView(withText(R.string.pattern_already_set)).check(matches(isEnabled()))
    }

    @Test
    fun onlyOneMethodEnabledPasscode() {
        firstEnablePasscode()
        onView(withText(R.string.prefs_pattern)).perform(click())
        onView(withText(R.string.passcode_already_set)).check(matches(isEnabled()))
    }

    @Test
    fun disablePasscode() {
        firstEnablePasscode()
        mockIntent(
            extras = Pair(PassCodeActivity.KEY_CHECK_RESULT, true),
            action = PassCodeActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertFalse(prefPasscode.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(not(isEnabled())))
        assertFalse(prefBiometric.isEnabled)
        assertFalse(prefBiometric.isChecked)
    }

    @Test
    fun disablePattern() {
        firstEnablePattern()
        mockIntent(
            extras = Pair(PatternLockActivity.KEY_CHECK_RESULT, true),
            action = PatternLockActivity.ACTION_CHECK_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertFalse(prefPattern.isChecked)
        onView(withText(R.string.prefs_biometric)).check(matches(not(isEnabled())))
        assertFalse(prefBiometric.isEnabled)
        assertFalse(prefBiometric.isChecked)
    }

    @Test
    fun enableBiometricLockWithPasscodeEnabled() {
        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertTrue(prefBiometric.isChecked)
    }

    @Test
    fun enableBiometricLockWithPatternEnabled() {
        firstEnablePattern()
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertTrue(prefBiometric.isChecked)
    }

    @Test
    fun disableBiometricLock() {
        firstEnablePasscode()
        onView(withText(R.string.prefs_biometric)).perform(click())
        onView(withText(R.string.prefs_biometric)).perform(click())
        assertFalse(prefBiometric.isChecked)
    }

    @Test
    fun touchesDialog() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.confirmation_touches_with_other_windows_title)).check(matches(isDisplayed()))
        onView(withText(R.string.confirmation_touches_with_other_windows_message)).check(matches(isDisplayed()))
    }

    @Test
    fun touchesEnable() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        assertTrue(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    @Test
    fun touchesRefuse() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        assertFalse(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    @Test
    fun disableTouches() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        assertFalse(prefTouchesWithOtherVisibleWindows.isChecked)
    }

    private fun firstEnablePasscode() {
        mockIntent(
            extras = Pair(PassCodeActivity.KEY_PASSCODE, passCodeValue),
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_passcode)).perform(click())
    }

    private fun firstEnablePattern() {
        mockIntent(
            extras = Pair(PatternLockActivity.KEY_PATTERN, patternValue),
            action = PatternLockActivity.ACTION_REQUEST_WITH_RESULT
        )
        onView(withText(R.string.prefs_pattern)).perform(click())
    }
}
