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
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();

    private lateinit var mPrefCameraPictureUploads: CheckBoxPreference
    private lateinit var mPrefCameraVideoUploads: CheckBoxPreference
    private lateinit var mPrefCameraPictureUploadsWiFi: CheckBoxPreference
    private lateinit var mPrefCameraPictureUploadsPath: Preference
    private lateinit var mPrefCameraVideoUploadsWiFi: CheckBoxPreference
    private lateinit var mPrefCameraVideoUploadsPath: Preference
    private lateinit var mPrefCameraUploadsBehaviour: ListPreference
    private lateinit var mPrefCameraUploadsCategory: PreferenceCategory
    private lateinit var mPrefCameraUploadsSourcePath: Preference

    private val CAMERA_PICTURE_UPLOADS = "camera_picture_uploads"
    private val CAMERA_VIDEO_UPLOADS = "camera_video_uploads"
    private val CAMERA_PICTURE_UPLOADS_WIFI = "camera_picture_uploads_on_wifi"
    private val CAMERA_VIDEO_UPLOADS_WIFI = "camera_video_uploads_on_wifi"
    private val CAMERA_UPLOADS_BEHAVIOUR = "camera_uploads_behaviour"
    private val CAMERA_UPLOADS_CATEGORY = "camera_uploads_category"
    private val CAMERA_PICTURE_PATH = "camera_picture_uploads_path"
    private val CAMERA_VIDEO_PATH = "camera_video_uploads_path"
    private val CAMERA_SOURCE_PATH = "camera_uploads_source_path"

    @Before
    fun setUp() {

        //Set preferences as enabled with defaults
        preferencesEditor.putBoolean(CAMERA_PICTURE_UPLOADS, true);
        preferencesEditor.putBoolean(CAMERA_VIDEO_UPLOADS, true);
        preferencesEditor.putBoolean(CAMERA_PICTURE_UPLOADS_WIFI, false);
        preferencesEditor.putBoolean(CAMERA_VIDEO_UPLOADS_WIFI, false);
        preferencesEditor.commit();

        //To set the initial UI status
        mPrefCameraPictureUploads = activityRule.activity.findPreference(CAMERA_PICTURE_UPLOADS) as CheckBoxPreference
        mPrefCameraVideoUploads = activityRule.activity.findPreference(CAMERA_VIDEO_UPLOADS) as CheckBoxPreference
        mPrefCameraUploadsSourcePath = activityRule.activity.findPreference(CAMERA_SOURCE_PATH) as Preference
        mPrefCameraUploadsBehaviour = activityRule.activity.findPreference(CAMERA_UPLOADS_BEHAVIOUR) as ListPreference

        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraPictureUploads.setChecked(true)
            mPrefCameraVideoUploads.setChecked(true)
            mPrefCameraUploadsSourcePath.isEnabled = true
            mPrefCameraUploadsBehaviour.setValue("NOTHING")
        })

    }

    @Test
    fun disablePictureUploadsAccept() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        //Asserts
        assertFalse(mPrefCameraPictureUploads.isChecked)
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(doesNotExist())
    }

    @Test
    fun disablePictureUploadsRefuse() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        //Asserts
        assertTrue(mPrefCameraPictureUploads.isChecked)
    }

    @Test
    fun disableVideoUploadsAccept() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        //Asserts
        assertFalse(mPrefCameraVideoUploads.isChecked)
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_video_upload_on_wifi)).check(doesNotExist())
    }

    @Test
    fun disableVideoUploadsRefuse() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        //Asserts
        assertTrue(mPrefCameraVideoUploads.isChecked)
    }

    @Test
    fun cameraFolderView() {
        //Asserts
        onView(withText(String.format(
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required))))
        .check(matches(isDisplayed()))
    }

    @Test
    fun cameraOpenPicker() {
        val cameraFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).absolutePath + "/Camera";
        Intents.init()
        mPrefCameraUploadsSourcePath = activityRule.activity.findPreference(CAMERA_SOURCE_PATH) as Preference
        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraUploadsSourcePath.isEnabled = true
        })
        //Asserts
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
        //Asserts
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(matches(isDisplayed()))
    }

    @Test
    fun originalFileWillBeOptions() {
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        //Asserts
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))
    }

    @Test
    fun switchOriginalFileWillBe() {
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        onView(withText(R.string.pref_behaviour_entries_move)).perform(click())
        //Asserts
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))
    }

}