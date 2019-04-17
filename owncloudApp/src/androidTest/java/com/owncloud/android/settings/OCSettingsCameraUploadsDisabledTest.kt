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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.Preferences
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OCSettingsCameraUploadsDisabledTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    @Before
    fun setUp() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraPictureUploads.setChecked(false)
        })
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraVideoUploads.setChecked(false)
        })
    }

    @Test
    fun testEnablePictureUploadsShowsWarning() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(R.string.common_important)).check(matches(isDisplayed()))
        onView(withText(R.string.proper_pics_folder_warning_camera_upload)).check(matches(isDisplayed()))
    }

    @Test
    fun testEnablePictureUploads() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
        assertTrue(mPrefCameraPictureUploads.isChecked)
    }



    @Test
    fun testEnableVideoUploadsShowsWarning() {
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_important)).check(matches(isDisplayed()));
        onView(withText(R.string.proper_videos_folder_warning_camera_upload)).check(matches(isDisplayed()));
    }

    @Test
    fun testEnableVideoUploads() {
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
        assertTrue(mPrefCameraVideoUploads.isChecked)
    }


    @Test
    fun testOptionsPictureUploadsEnabled() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())

        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(matches(isDisplayed()))
    }

    @Test
    fun testOptionsVideoUploadsEnabled() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())

        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_video_upload_on_wifi)).check(matches(isDisplayed()))
    }

}