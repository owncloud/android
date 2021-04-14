/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.settings.logs

import android.preference.CheckBoxPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LogHistoryActivity
import com.owncloud.android.ui.activity.Preferences
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore
class OCSettingsLogsTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule(Preferences::class.java, true, true)
    private lateinit var prefHttpLogs: CheckBoxPreference

    @Before
    fun setUp() {
        Intents.init()
        //Only interested in "More" section, so we can get rid of the other categories. SmoothScroll is not
        //working fine to reach the bottom of the screen, so this approach was taken to display the section
        val preferenceScreen = activityRule.activity.preferenceScreen as PreferenceScreen
        prefHttpLogs = activityRule.activity.findPreference("set_httpLogs") as CheckBoxPreference

        val cameraUploadsCategory =
            activityRule.activity.findPreference("camera_uploads_category") as PreferenceCategory
        val securityCategory =
            activityRule.activity.findPreference("security_category") as PreferenceCategory
        val moreCategory =
            activityRule.activity.findPreference("more") as PreferenceCategory
        preferenceScreen.removePreference(cameraUploadsCategory)
        preferenceScreen.removePreference(securityCategory)
        preferenceScreen.removePreference(moreCategory)
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Ignore
    @Test
    fun loggerView() {
        onView(withText(R.string.actionbar_logger)).check(matches(isDisplayed()))
    }

    @Test
    fun enableHttpLogs() {
        onView(withText(R.string.prefs_http_logs)).perform(click())
        //Asserts
        Assert.assertTrue(prefHttpLogs.isChecked)
    }

    @Ignore
    @Test
    fun loggerOpen() {
        onView(withText(R.string.log_open_logs_view)).perform(click())
        intended(hasComponent(LogHistoryActivity::class.java.name))
    }
}
