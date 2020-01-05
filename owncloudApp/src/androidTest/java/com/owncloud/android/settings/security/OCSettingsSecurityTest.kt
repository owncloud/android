/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.settings.security

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.preference.CheckBoxPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import android.preference.PreferenceScreen
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.ui.activity.Preferences
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OCSettingsSecurityTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    private lateinit var mPrefPasscode: CheckBoxPreference
    private lateinit var mPrefPattern: CheckBoxPreference
    private lateinit var mPrefTouches: CheckBoxPreference

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val keyCheckResult = "KEY_CHECK_RESULT"
    private val keyPassCode = "KEY_PASSCODE"
    private val keyPattern = "KEY_PATTERN"
    private val keyCheckPatternResult = "KEY_CHECK_PATTERN_RESULT"

    private val passCodeValue = "1111"
    private val patternValue = "1234"

    @Before
    fun setUp() {
        //Only interested in "More" section, so we can get rid of the other categories. SmoothScroll is not
        //working fine to reach the bottom of the screen, so this approach was taken to display the section
        val preferenceScreen = activityRule.activity.preferenceScreen as PreferenceScreen
        val cameraUploadsCategory =
            activityRule.activity.findPreference("camera_uploads_category") as PreferenceCategory
        val moreCategory =
            activityRule.activity.findPreference("more") as PreferenceCategory
        preferenceScreen.removePreference(cameraUploadsCategory)
        preferenceScreen.removePreference(moreCategory)

        mPrefPasscode = activityRule.activity.findPreference("set_pincode") as CheckBoxPreference
        mPrefPattern = activityRule.activity.findPreference("set_pattern") as CheckBoxPreference
        mPrefTouches = activityRule.activity.findPreference("touches_with_other_visible_windows")
                as CheckBoxPreference

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        //clear SharedPreferences
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun securityView() {
        onView(withText(R.string.prefs_passcode)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_pattern)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_fingerprint)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_fingerprint_summary)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_fingerprint)).check(matches(not(isEnabled())))
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
        assertTrue(mPrefPasscode.isChecked)
        onView(withText(R.string.pass_code_stored)).check(matches(isDisplayed()))
    }

    @Test
    fun patternLockEnabled() {
        val result = Intent()
        result.putExtra(keyPattern, patternValue)
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PatternLockActivity.ACTION_REQUEST_WITH_RESULT)).respondWith(intentResult)
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertTrue(mPrefPattern.isChecked)
        onView(withText(R.string.pattern_stored)).check(matches(isDisplayed()))
    }

    @Test
    fun enablePasscodeEnablesFingerprint() {
        firstEnablePasscode()
        onView(withText(R.string.prefs_fingerprint)).check(matches(isEnabled()))
    }

    @Test
    fun enablePatternEnablesFingerprint() {
        firstEnablePattern()
        onView(withText(R.string.prefs_fingerprint)).check(matches(isEnabled()))
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
        val result = Intent()
        result.putExtra(keyCheckResult, true)
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PatternLockActivity.ACTION_CHECK_WITH_RESULT)).respondWith(intentResult)
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertFalse(mPrefPasscode.isChecked)
        onView(withText(R.string.pass_code_removed)).check(matches(isEnabled()))
        onView(withText(R.string.prefs_fingerprint)).check(matches(not(isEnabled())))
    }

    @Test
    fun disablePattern() {
        firstEnablePattern()
        val result = Intent()
        result.putExtra(keyCheckPatternResult, true)
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PatternLockActivity.ACTION_CHECK_WITH_RESULT)).respondWith(intentResult)
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertFalse(mPrefPattern.isChecked)
        onView(withText(R.string.pattern_removed)).check(matches(isEnabled()))
        onView(withText(R.string.prefs_fingerprint)).check(matches(not(isEnabled())))
    }

    @Test
    fun touchesDialog() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(activityRule.activity.getString(R.string.confirmation_touches_with_other_windows_title)))
            .check(matches(isDisplayed()))
        onView(withText(activityRule.activity.getString(R.string.confirmation_touches_with_other_windows_message)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun touchesEnable() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        assertTrue(mPrefTouches.isChecked)
    }

    @Test
    fun touchesRefuse() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        assertFalse(mPrefTouches.isChecked)
    }

    @Test
    fun disableTouches() {
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).perform(click())
        assertFalse(mPrefTouches.isChecked)
    }

    private fun firstEnablePasscode() {
        val result = Intent()
        result.putExtra(keyPassCode, passCodeValue)
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)).respondWith(intentResult)
        onView(withText(R.string.prefs_passcode)).perform(click())
    }

    private fun firstEnablePattern() {
        val result = Intent()
        result.putExtra(keyPattern, patternValue)
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PatternLockActivity.ACTION_REQUEST_WITH_RESULT)).respondWith(intentResult)
        onView(withText(R.string.prefs_pattern)).perform(click())
    }
}
