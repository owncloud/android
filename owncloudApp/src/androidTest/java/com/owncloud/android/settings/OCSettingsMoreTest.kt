/**
 * ownCloud Android client application
 *
 * @author Jesús Recio @jesmrec
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.settings

import android.preference.CheckBoxPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.Preferences
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.allOf
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import android.content.Intent
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import android.app.Instrumentation.ActivityResult
import android.app.Activity
import android.net.Uri
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag
import com.owncloud.android.BuildConfig
import com.owncloud.android.ui.activity.LogHistoryActivity
import com.owncloud.android.ui.activity.PrivacyPolicyActivity

@RunWith(AndroidJUnit4::class)
class OCSettingsMoreTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    @Before
    fun setUp() {
        //Only interested in "More" section, so we can get rid of the other categories. SmoothScroll is not
        //working fine to reach the bottom of the screen, so this approach was take to display the section
        val preferenceScreen = activityRule.activity.getPreferenceScreen() as PreferenceScreen
        val cameraUploadsCategory = activityRule.activity.findPreference("camera_uploads_category") as PreferenceCategory
        val securityCategory = activityRule.activity.findPreference("security_category") as PreferenceCategory
        preferenceScreen.removePreference(cameraUploadsCategory)
        preferenceScreen.removePreference(securityCategory)
    }


    @Test
    fun helpView(){
        onView(withText(R.string.prefs_help)).check(matches(isDisplayed()))
    }

    @Test
    fun helpOptionOpensWeb(){
        Intents.init()
        onView(withText(R.string.prefs_help)).perform(click())
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult);
        intended(hasData("https://owncloud.com/mobile/help"))
        Intents.release()
    }

    @Test
    fun dav5xView(){
        onView(withText(R.string.prefs_sync_calendar_contacts)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_sync_calendar_contacts_summary)).check(matches(isDisplayed()))
    }

    @Test
    fun davx5OpensLink(){
        Intents.init()
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult);
        onView(withText(R.string.prefs_sync_calendar_contacts)).perform(click())
        intended(hasData("https://play.google.com/store/apps/details?id=at.bitfire.davdroid"))
        Intents.release()
    }

    @Test
    fun recommendView(){
        onView(withText(R.string.prefs_recommend)).check(matches(isDisplayed()))
    }

    @Test
    fun recommendOpenSender(){
        Intents.init()
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_SENDTO)).respondWith(intentResult);
        onView(withText(R.string.prefs_recommend)).perform(click())
        intended(allOf(
            hasAction(Intent.ACTION_SENDTO),
            hasExtra(Intent.EXTRA_SUBJECT,
                String.format(
                    activityRule.activity.getString(R.string.recommend_subject),
                    activityRule.activity.getString(R.string.app_name))),
            hasExtra(Intent.EXTRA_TEXT,
                String.format(
                    activityRule.activity.getString(R.string.recommend_text),
                    activityRule.activity.getString(R.string.app_name),
                    activityRule.activity.getString(R.string.url_app_download)
                )),
            hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)))
        Intents.release()
    }

    @Test
    fun feedbackView(){
        onView(withText(R.string.drawer_feedback)).check(matches(isDisplayed()))
    }

    @Test
    fun feedbackOpenSender(){
        Intents.init()
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_SENDTO)).respondWith(intentResult);
        onView(withText(R.string.drawer_feedback)).perform(click())
        intended(allOf(
            hasAction(Intent.ACTION_SENDTO),
            hasExtra(Intent.EXTRA_SUBJECT,
                "Android v" + BuildConfig.VERSION_NAME + " - " + activityRule.activity.getText(R.string.prefs_feedback)),
            hasData(Uri.parse(activityRule.activity.getText(R.string.mail_feedback) as String)),
            hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)))
        Intents.release()
    }

    @Test
    fun loggerView(){
        onView(withText(R.string.actionbar_logger)).check(matches(isDisplayed()))
    }

    @Test
    fun loggerOpen(){
        Intents.init()
        onView(withText(R.string.actionbar_logger)).perform(click())
        intended(hasComponent(LogHistoryActivity::class.java.name))
        Intents.release()
    }

    @Test
    fun privacyPolicyView(){
        onView(withText(R.string.prefs_privacy_policy)).check(matches(isDisplayed()))
    }

    @Test
    fun privacyPolicyOpenWeb(){
        Intents.init()
        onView(withText(R.string.prefs_privacy_policy)).perform(click())
        intended(hasComponent(PrivacyPolicyActivity::class.java.name))
        Intents.release()
    }

}