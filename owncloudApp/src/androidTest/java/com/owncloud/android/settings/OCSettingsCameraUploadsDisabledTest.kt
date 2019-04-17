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
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var mPrefCameraPictureUploads: CheckBoxPreference
    private lateinit var mPrefCameraVideoUploads: CheckBoxPreference
    private lateinit var mPrefCameraUploadBehaviour: ListPreference
    private lateinit var mPrefCameraUploadSourcePath: Preference
    private lateinit var mPrefCameraUploadsCategory: PreferenceCategory

    private val CAMERA_PICTURE_UPLOADS = "camera_picture_uploads"
    private val CAMERA_VIDEO_UPLOADS = "camera_video_uploads"
    private val CAMERA_VIDEO_UPLOADS_WIFI= "camera_video_uploads_on_wifi"
    private val CAMERA_PICTURE_UPLOADS_WIFI= "camera_picture_uploads_on_wifi"
    private val CAMERA_UPLOADS_BEHAVIOUR = "camera_uploads_behaviour"
    private val CAMERA_PICTURE_PATH = "camera_picture_uploads_path"
    private val CAMERA_UPLOADS_CATEGORIES = "camera_uploads_category"
    private val CAMERA_UPLOADS_PATH = "camera_uploads_source_path"

    @Before
    fun setUp() {
        //Set preferences as disabled
        mPrefCameraPictureUploads =
            activityRule.activity.findPreference(CAMERA_PICTURE_UPLOADS) as CheckBoxPreference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraPictureUploads.setChecked(false)
        })
        mPrefCameraVideoUploads =
            activityRule.activity.findPreference(CAMERA_VIDEO_UPLOADS) as CheckBoxPreference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraVideoUploads.setChecked(false)
        })

        var preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferencesEditor.putBoolean(CAMERA_PICTURE_UPLOADS, false);
        preferencesEditor.putBoolean(CAMERA_VIDEO_UPLOADS, false);
        preferencesEditor.putBoolean(CAMERA_PICTURE_UPLOADS_WIFI, false);
        preferencesEditor.putBoolean(CAMERA_VIDEO_UPLOADS_WIFI, false);
        preferencesEditor.remove(CAMERA_UPLOADS_BEHAVIOUR)
        preferencesEditor.remove(CAMERA_UPLOADS_PATH)
        preferencesEditor.commit();
    }

    @Test
    fun enablePictureUploadsShowsWarning() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        //Asserts
        onView(withText(R.string.common_important)).check(matches(isDisplayed()))
        onView(withText(R.string.proper_pics_folder_warning_camera_upload)).check(matches(isDisplayed()))
    }

    @Test
    fun enablePictureUploads() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
        //Asserts
        assertTrue(mPrefCameraPictureUploads.isChecked)
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(matches(isDisplayed()))
    }

    @Test
    fun enableVideoUploadsShowsWarning() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        //Asserts
        onView(withText(R.string.common_important)).check(matches(isDisplayed()));
        onView(withText(R.string.proper_videos_folder_warning_camera_upload)).check(matches(isDisplayed()));
    }

    @Test
    fun enableVideoUploads() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
        //Asserts
        assertTrue(mPrefCameraVideoUploads.isChecked)
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_video_upload_on_wifi)).check(matches(isDisplayed()))
    }


}