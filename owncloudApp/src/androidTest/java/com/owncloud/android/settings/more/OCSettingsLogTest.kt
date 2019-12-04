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

package com.owncloud.android.settings.more

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LogHistoryActivity
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OCSettingsLogTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(LogHistoryActivity::class.java, true, true)

    @Before
    fun setUp() {
    }

    @Test
    fun checkTitle() {
        onView(withText(R.string.actionbar_logger)).check(matches(isDisplayed()))
    }

    @Test
    fun itemsToolbar() {
        onView(withId(R.id.menu_search)).check(matches(isDisplayed()))
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText("Logcat")).check(matches(isDisplayed()))
    }

    @Test
    fun logHistoryButtons() {
        onView(withId(R.id.deleteLogHistoryButton)).check(matches(isDisplayed()))
        onView(withId(R.id.sendLogHistoryButton)).check(matches(isDisplayed()))
    }

    @Test
    fun sendLogHistory() {
        Intents.init()
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_SEND_MULTIPLE)).respondWith(intentResult);
        onView(withId(R.id.sendLogHistoryButton)).perform(click())
        intended(
            Matchers.allOf(
                hasAction(Intent.ACTION_SEND_MULTIPLE),
                hasExtra(
                    Intent.EXTRA_SUBJECT,
                    String.format(
                        activityRule.activity.getString(R.string.log_send_mail_subject),
                        activityRule.activity.getString(R.string.app_name)
                    )
                ),
                hasType("text/plain"),
                hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )
        Intents.release()
    }
}
