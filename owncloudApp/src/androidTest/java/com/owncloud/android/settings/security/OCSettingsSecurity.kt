/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
 * Copyright (C) 2019 ownCloud GmbH.
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
import android.preference.PreferenceScreen
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.ui.activity.Preferences
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import com.owncloud.android.R
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.junit.Test
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.ui.activity.PrivacyPolicyActivity
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import org.junit.Assert.assertTrue
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction

@RunWith(AndroidJUnit4::class)
class OCSettingsSecurity {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    private lateinit var mPrefPasscode: CheckBoxPreference
    private lateinit var mPrefPattern: CheckBoxPreference

    @Before
    fun setUp() {
        //Only interested in "More" section, so we can get rid of the other categories. SmoothScroll is not
        //working fine to reach the bottom of the screen, so this approach was taken to display the section
        val preferenceScreen = activityRule.activity.getPreferenceScreen() as PreferenceScreen
        val cameraUploadsCategory =
            activityRule.activity.findPreference("camera_uploads_category") as PreferenceCategory
        val moreCategory =
            activityRule.activity.findPreference("more") as PreferenceCategory
        preferenceScreen.removePreference(cameraUploadsCategory)
        preferenceScreen.removePreference(moreCategory)

        mPrefPasscode = activityRule.activity.findPreference("set_pincode") as CheckBoxPreference
        mPrefPattern = activityRule.activity.findPreference("set_pattern") as CheckBoxPreference
    }

    @After
    fun tearDown(){
        //clear SharedPreferences
    }

    @Test
    fun securityView(){
        onView(withText(R.string.prefs_passcode)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_pattern)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_touches_with_other_visible_windows)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_touches_with_other_visible_windows_summary)).check(matches(isDisplayed()))
    }

    @Test
    fun passcodeOpen(){
        Intents.init()
        onView(withText(R.string.prefs_passcode)).perform(click())
        intended(hasComponent(PassCodeActivity::class.java.name))
        Intents.release()
    }

    @Test
    fun patternOpen(){
        Intents.init()
        onView(withText(R.string.prefs_pattern)).perform(click())
        intended(hasComponent(PatternLockActivity::class.java.name))
        Intents.release()
    }

    @Test
    fun passcodeEnabled(){
        Intents.init()
        val result = Intent()
        result.putExtra("KEY_PASSCODE", "1111")
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)).respondWith(intentResult);
        onView(withText(R.string.prefs_passcode)).perform(click())
        assertTrue(mPrefPasscode.isChecked)
        Intents.release()
        //disablePasscode()
    }

    @Test
    fun patternLockEnabled(){
        Intents.init()
        val result = Intent()
        result.putExtra("KEY_PATTERN", "sñljldfjgodfjgodfgofdg")
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, result)
        intending(hasAction(PatternLockActivity.ACTION_REQUEST_WITH_RESULT)).respondWith(intentResult)
        onView(withText(R.string.prefs_pattern)).perform(click())
        assertTrue(mPrefPattern.isChecked)
        Intents.release()
        //disablePattern()
    }

    fun disablePasscode(){
        mPrefPasscode.isEnabled = false
    }

    fun disablePattern(){
        mPrefPattern.isEnabled = false
    }


}