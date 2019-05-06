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

package com.owncloud.android.settings.camerauploads

import android.os.Environment
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
import com.owncloud.android.ui.activity.Preferences
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OCSettingsCameraUploadsTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val CAMERA_PICTURE_UPLOADS = "camera_picture_uploads"
    private val CAMERA_PICTURE_UPLOADS_WIFI = "camera_picture_uploads_on_wifi"
    private val CAMERA_VIDEO_UPLOADS = "camera_video_uploads"
    private val CAMERA_VIDEO_UPLOADS_WIFI = "camera_video_uploads_on_wifi"
    private val CAMERA_SOURCE_PATH = "camera_uploads_source_path"

    private lateinit var mPrefCameraPictureUploads: CheckBoxPreference
    private lateinit var mPrefCameraVideoUploads: CheckBoxPreference
    private lateinit var mPrefCameraUploadsSourcePath: Preference

    @Before
    fun setUp() {
        //Set preferences as disabled with defaults
        var preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferencesEditor.putBoolean(CAMERA_PICTURE_UPLOADS, false);
        preferencesEditor.putBoolean(CAMERA_VIDEO_UPLOADS, false);
        preferencesEditor.putBoolean(CAMERA_PICTURE_UPLOADS_WIFI, false);
        preferencesEditor.putBoolean(CAMERA_VIDEO_UPLOADS_WIFI, false);
        preferencesEditor.commit();

        //To set the initial UI status
        mPrefCameraPictureUploads = activityRule.activity.findPreference(CAMERA_PICTURE_UPLOADS) as CheckBoxPreference
        mPrefCameraVideoUploads = activityRule.activity.findPreference(CAMERA_VIDEO_UPLOADS) as CheckBoxPreference

        activityRule.activity.runOnUiThread(Runnable {
            mPrefCameraPictureUploads.setChecked(false)
            mPrefCameraVideoUploads.setChecked(false)
        })

    }

    @Test
    fun checkTitle() {
        //Asserts
        onView(withText(R.string.actionbar_settings)).check(matches(isDisplayed()))
    }

    @Test
    fun pictureUploadsView() {
        //Asserts
        onView(withText(R.string.prefs_camera_picture_upload)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_camera_picture_upload_summary)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(doesNotExist())
    }

    @Test
    fun videoUploadsView() {
        //Asserts
        onView(withText(R.string.prefs_camera_video_upload)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_camera_video_upload_summary)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_video_upload_on_wifi)).check(doesNotExist())
    }

    @Test
    fun optionsCameraFolderBehaviour() {
        //Asserts
        onView(withText(String.format(
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required))))
            .check(doesNotExist())
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(doesNotExist())
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(doesNotExist())
    }

    @Test
    fun enablePictureUploadsShowsWarning() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        //Asserts
        onView(withText(R.string.common_important)).check(matches(isDisplayed()))
        onView(withText(R.string.proper_pics_folder_warning_camera_upload)).check(matches(isDisplayed()))
        //Reset suboptions
        onView(withText(android.R.string.ok)).perform(click())
        removePictureSubOptions()
    }

    @Test
    fun enablePictureUploads() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
        //Asserts
        assertTrue(mPrefCameraPictureUploads.isChecked)
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(matches(isDisplayed()))
        //Reset suboptions
        removePictureSubOptions()
    }

    @Test
    fun enableVideoUploadsShowsWarning() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        //Asserts
        onView(withText(R.string.common_important)).check(matches(isDisplayed()));
        onView(withText(R.string.proper_videos_folder_warning_camera_upload)).check(matches(isDisplayed()));
        //Reset suboptions
        onView(withText(android.R.string.ok)).perform(click())
        removeVideoSubOptions()
    }

    @Test
    fun enableVideoUploads() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
        //Asserts
        assertTrue(mPrefCameraVideoUploads.isChecked)
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_video_upload_on_wifi)).check(matches(isDisplayed()))
        //Reset suboptions
        removeVideoSubOptions()
    }

    @Test
    fun disablePictureUploadsAccept() {
        //First, enable
        enablePictureSubOptions()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        //Asserts
        Assert.assertFalse(mPrefCameraPictureUploads.isChecked)
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(doesNotExist())
        //Reset suboptions
    }

    @Test
    fun disablePictureUploadsRefuse() {
        enablePictureSubOptions()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        //Asserts
        assertTrue(mPrefCameraPictureUploads.isChecked)
        removePictureSubOptions()

    }

    @Test
    fun disableVideoUploadsAccept() {
        enableVideoSubOptions()
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
        //Asserts
        Assert.assertFalse(mPrefCameraVideoUploads.isChecked)
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_video_upload_on_wifi)).check(doesNotExist())
    }

    @Test
    fun disableVideoUploadsRefuse() {
        enableVideoSubOptions()
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_no)).perform(click());
        //Asserts
        assertTrue(mPrefCameraVideoUploads.isChecked)
        removeVideoSubOptions()
    }

    @Test
    fun cameraFolderView() {
        enablePictureSubOptions()
        //Asserts
        onView(withText(String.format(
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
            activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required))))
            .check(matches(isDisplayed()))
        removePictureSubOptions()
    }

    @Test
    fun cameraOpenPicker() {
        enablePictureSubOptions()
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
        Intents.intended(IntentMatchers.hasComponent(LocalFolderPickerActivity::class.java.name))
        IntentMatchers.hasExtra(
            LocalFolderPickerActivity.EXTRA_PATH, cameraFolder
        )
        Intents.release()
        onView(withText(android.R.string.cancel)).perform(click())
        removePictureSubOptions()
    }

    /*@Test
    fun originalFileWillBeView() {
        enablePictureSubOptions()
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(matches(isDisplayed()))
        /*onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        //Asserts
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))*/
        removePictureSubOptions()
    }*/

    @Test
    fun originalFileWillBeOptions() {
        enablePictureSubOptions()
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        //Asserts
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(matches(isDisplayed()))
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))

        onView(withText(R.string.pref_behaviour_entries_keep_file)).perform(click())
        removePictureSubOptions()
    }

    @Test
    fun switchOriginalFileWillBe() {
        enablePictureSubOptions()
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        onView(withText(R.string.pref_behaviour_entries_move)).perform(click())
        //Asserts
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))

        onView(withText(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        onView(withText(R.string.pref_behaviour_entries_keep_file)).perform(click())
        removePictureSubOptions()

    }

    fun removePictureSubOptions(){
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
    }

    fun removeVideoSubOptions(){
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(R.string.common_yes)).perform(click());
    }

    fun enablePictureSubOptions(){
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
    }

    fun enableVideoSubOptions(){
        onView(withText(R.string.prefs_camera_video_upload)).perform(click());
        onView(withText(android.R.string.ok)).perform(click())
    }

}