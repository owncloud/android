/**
 * ownCloud Android client application
 *
 * @author Jesús Recio @jesmrec
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

package com.owncloud.android.settings.more

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LogHistoryActivity
import org.junit.Rule
import org.junit.Test

class OCSettingsLogTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(LogHistoryActivity::class.java, true, true)

    @Test
    fun itemsToolbar() {
        onView(withId(R.id.search_button)).check(matches(isDisplayed()))
        //Values not i18n
        onView(withText("LOGCAT")).check(matches(isDisplayed()))
        onView(withText("LOGFILE")).check(matches(isDisplayed()))
    }

    @Test
    fun itemsLogLevel() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        //Values not i18n
        onView(withText("Verbose")).check(matches(isDisplayed()))
        onView(withText("Debug")).check(matches(isDisplayed()))
        onView(withText("Info")).check(matches(isDisplayed()))
        onView(withText("Warning")).check(matches(isDisplayed()))
        onView(withText("Error")).check(matches(isDisplayed()))
    }
}
