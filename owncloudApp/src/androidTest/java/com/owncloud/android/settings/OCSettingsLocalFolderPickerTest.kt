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

import android.os.Environment
import android.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
import com.owncloud.android.ui.activity.Preferences
import org.junit.Rule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import org.junit.Assert.assertTrue
import com.owncloud.android.R

@RunWith(AndroidJUnit4::class)
class OCSettingsLocalFolderPickerTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(LocalFolderPickerActivity::class.java, true, true)

    @Test
    fun LocalFolderPickerView(){
        onView(withId(R.id.folder_picker_btn_cancel)).check(matches(isDisplayed()))
        onView(withId(R.id.folder_picker_btn_choose)).check(matches(isDisplayed()))
        onView(withId(R.id.folder_picker_btn_home)).check(matches(isDisplayed()))
    }

    @Test
    fun cancelButtonDismiss(){
        onView(withId(R.id.folder_picker_btn_cancel)).perform(click())
        assertTrue("Activity not finished", activityRule.activity.isFinishing)
    }

    @Test
    fun chooseButtonDismiss(){
        onView(withId(R.id.folder_picker_btn_choose)).perform(click())
        assertTrue("Activity not finished", activityRule.activity.isFinishing)
    }

    @Test
    fun homeButtontoHome(){
        onView(withId(R.id.folder_picker_btn_home)).perform(click())
        val path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).parent.split("/").last()
        onView(withText(path)).check(matches(isDisplayed()))
    }

}