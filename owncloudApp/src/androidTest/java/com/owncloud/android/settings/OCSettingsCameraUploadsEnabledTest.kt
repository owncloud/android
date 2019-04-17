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
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
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
        var mPrefCameraUploadsBehaviour =
            activityRule.activity.findPreference("camera_uploads_behaviour") as ListPreference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraUploadsBehaviour.setValue(mPrefCameraUploadsBehaviour.entryValues.get(0) as String)
        })
    }

    @Test
    fun disablePictureUploadsAccept() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        assertFalse(mPrefCameraPictureUploads.isChecked)
    }

    @Test
    fun disablePictureUploadsRefuse() {
        var mPrefCameraPictureUploads =
            activityRule.activity.findPreference("camera_picture_uploads") as CheckBoxPreference
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        assertTrue(mPrefCameraPictureUploads.isChecked)
    }

    @Test
    fun disableVideoUploadsAccept() {
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        assertFalse(mPrefCameraVideoUploads.isChecked)
    }

    @Test
    fun disableVideoUploadsRefuse() {
        var mPrefCameraVideoUploads =
            activityRule.activity.findPreference("camera_video_uploads") as CheckBoxPreference
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        assertTrue(mPrefCameraVideoUploads.isChecked)
    }

    @Test
    fun cameraFolderView() {
        onView(withText(String.format(
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required))))
        .check(matches(isDisplayed()))
    }

    @Test
    fun cameraFolderOpenPicker() {
        val cameraFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).absolutePath + "/Camera";
        Intents.init()
        onView(withText(String.format(
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required))))
        .perform(click())
        intended(hasComponent(LocalFolderPickerActivity::class.java.name))
        hasExtra(
            LocalFolderPickerActivity.EXTRA_PATH, cameraFolder)
        Intents.release()
    }

    @Test
    fun originalFileWillBeView() {
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(matches(isDisplayed()))
    }

    @Test
    fun originalFileWillBeOptions() {
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))
    }

    @Test
    fun switchOriginalFileWillBe() {
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        onView(withText(R.string.pref_behaviour_entries_move)).perform(click())
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))
    }

}