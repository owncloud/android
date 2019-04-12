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
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OCSettingsCameraUploadsEnabledTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    @Before
    fun setUp() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraPictureUploads.setChecked(true)
        })
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraVideoUploads.setChecked(true)
        })
    }

    @Test
    fun testDisablePictureUploadsAccept() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        assertFalse(mPrefCameraPictureUploads.isChecked)
    }

    @Test
    fun testDisablePictureUploadsRefuse() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        assertTrue(mPrefCameraPictureUploads.isChecked)
    }

    @Test
    fun testDisableVideoUploadsAccept() {
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        assertFalse(mPrefCameraVideoUploads.isChecked)
    }

    @Test
    fun testDisableVideoUploadsRefuse() {
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference

        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        assertTrue(mPrefCameraVideoUploads.isChecked)
    }

}