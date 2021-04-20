/**
 * ownCloud Android client application
 *
 * @author Jesús Recio @jesmrec
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

package com.owncloud.android.settings.camerauploads

import android.os.Environment
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OCSettingsLocalFolderPickerTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(LocalFolderPickerActivity::class.java)

    @Test
    fun localFolderPickerView() {
        onView(withId(R.id.folder_picker_btn_cancel)).check(matches(isDisplayed()))
        onView(withId(R.id.folder_picker_btn_choose)).check(matches(isDisplayed()))
        onView(withId(R.id.folder_picker_btn_home)).check(matches(isDisplayed()))
    }

    //Activity is finished after assertion is checked. Will think whether the following
    //two tests make sense
    @Test
    fun cancelButtonDismiss() {
        onView(withId(R.id.folder_picker_btn_cancel)).perform(click())
        assertEquals(Lifecycle.State.DESTROYED, activityRule.scenario.state)
    }

    @Test
    fun chooseButtonDismiss() {
        onView(withId(R.id.folder_picker_btn_choose)).perform(click())
        assertEquals(Lifecycle.State.DESTROYED, activityRule.scenario.state)
    }

    @Test
    fun homeButtonBrowsesToHome() {
        onView(withId(R.id.folder_picker_btn_home)).perform(click())
        val path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).parent.split("/").last()
        onView(withText(path)).check(matches(isDisplayed()))
    }
}
