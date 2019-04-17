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

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LogHistoryActivity
import com.owncloud.android.ui.activity.Preferences
import com.owncloud.android.ui.activity.PrivacyPolicyActivity
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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

        //Starts the intent catching
        Intents.init()
    }

    @After
    fun tearDown(){
        Intents.release()
    }


    @Test
    fun helpView(){
        onView(withText(R.string.prefs_help)).check(matches(isDisplayed()))
    }

    @Test
    fun helpOpensWeb(){
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        val helpUrl = activityRule.activity.getText(R.string.url_help) as String
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult);
        onView(withText(R.string.prefs_help)).perform(click())
        intended(hasData(helpUrl))
    }

    @Test
    fun dav5xView(){
        onView(withText(R.string.prefs_sync_calendar_contacts)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_sync_calendar_contacts_summary)).check(matches(isDisplayed()))
    }

    @Test
    fun davx5OpensLink(){
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult);
        onView(withText(R.string.prefs_sync_calendar_contacts)).perform(click())
        intended(hasData(activityRule.activity.getText(R.string.url_sync_calendar_contacts) as String))
    }

    @Test
    fun recommendView(){
        onView(withText(R.string.prefs_recommend)).check(matches(isDisplayed()))
    }

    @Test
    fun recommendOpenSender(){
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
    }

    @Test
    fun feedbackView(){
        onView(withText(R.string.drawer_feedback)).check(matches(isDisplayed()))
    }

    @Test
    fun feedbackOpenSender(){
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_SENDTO)).respondWith(intentResult);
        onView(withText(R.string.drawer_feedback)).perform(click())
        intended(allOf(
            hasAction(Intent.ACTION_SENDTO),
            hasExtra(Intent.EXTRA_SUBJECT,
                "Android v" + BuildConfig.VERSION_NAME + " - " + activityRule.activity.getText(R.string.prefs_feedback)),
            hasData(Uri.parse(activityRule.activity.getText(R.string.mail_feedback) as String)),
            hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)))
    }

    @Test
    fun loggerView(){
        onView(withText(R.string.actionbar_logger)).check(matches(isDisplayed()))
    }

    @Test
    fun loggerOpenView(){
        onView(withText(R.string.actionbar_logger)).perform(click())
        intended(hasComponent(LogHistoryActivity::class.java.name))
    }

    @Test
    fun privacyPolicyView(){
        onView(withText(R.string.prefs_privacy_policy)).check(matches(isDisplayed()))
    }

    @Test
    fun privacyPolicyOpenWeb(){
        onView(withText(R.string.prefs_privacy_policy)).perform(click())
        intended(hasComponent(PrivacyPolicyActivity::class.java.name))
    }

    @Test
    fun aboutView(){
        val version = activityRule.activity.getPackageManager()
            .getPackageInfo(activityRule.activity.getPackageName(), 0).versionName
        val commit = BuildConfig.COMMIT_SHA1
        val type = BuildConfig.BUILD_TYPE
        onView(withText(
                String.format(
                    activityRule.activity.getString(R.string.about_android),
                    activityRule.activity.getString(R.string.app_name))))
            .check(matches(isDisplayed()))
        onView(withText(
            String.format(
                activityRule.activity.getString(R.string.about_version),
                version + " " + type + " " + commit)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun aboutViewOpensGHCommit(){
        val commit = BuildConfig.COMMIT_SHA1
        val ghBaseUrl = BuildConfig.GIT_REMOTE
        val intentResult = ActivityResult(Activity.RESULT_OK, Intent())
        val ghUrl = ghBaseUrl + "/commit/" + commit
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult);
        onView(withText(
            String.format(
                activityRule.activity.getString(R.string.about_android),
                activityRule.activity.getString(R.string.app_name))))
            .perform(click())
        intended(hasData(ghUrl))
    }


}