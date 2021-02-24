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

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.PreferenceMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsFragment>

    private lateinit var prefPasscode: CheckBoxPreference

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val keyCheckResult = "KEY_CHECK_RESULT"
    private val keyPassCode = "KEY_PASSCODE"
    private val keyPattern = "KEY_PATTERN"
    private val keyCheckPatternResult = "KEY_CHECK_PATTERN_RESULT"

    private val passCodeValue = "1111"
    private val patternValue = "1234"

    private val keyPrefBiometric = "set_biometric"

    @Before
    fun setUp() {
        fragmentScenario = launchFragmentInContainer()
        fragmentScenario.onFragment { fragment ->
            prefPasscode = fragment.findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE)!!
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
        onView(withText(R.string.prefs_passcode)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_pattern)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_biometric)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_biometric_summary)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_biometric)).check(matches(Matchers.not(isEnabled())))
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_touches_with_other_visible_windows_summary)).check(matches(isDisplayed()))
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
        val result = Intent()
        result.putExtra(keyPassCode, passCodeValue)
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)).respondWith(intentResult)
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertTrue(prefPasscode.isChecked)
    }
}
