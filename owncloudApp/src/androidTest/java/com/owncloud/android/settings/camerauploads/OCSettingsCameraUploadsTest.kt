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

package com.owncloud.android.settings.camerauploads

import android.os.Environment
import android.preference.CheckBoxPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import android.preference.PreferenceScreen
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.PreferenceMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.LocalFolderPickerActivity
import com.owncloud.android.ui.activity.Preferences
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OCSettingsCameraUploadsTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(Preferences::class.java, true, true)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val cameraPictureUploads = "camera_picture_uploads"
    private val cameraVideoUploads = "camera_video_uploads"

    private lateinit var mPrefCameraPictureUploads: CheckBoxPreference
    private lateinit var mPrefCameraVideoUploads: CheckBoxPreference

    @Before
    fun setUp() {

        mPrefCameraPictureUploads = activityRule.activity.findPreference(cameraPictureUploads) as CheckBoxPreference
        mPrefCameraVideoUploads = activityRule.activity.findPreference(cameraVideoUploads) as CheckBoxPreference

        //Only interested in "Camera Uploads" section, so we can get rid of the other categories.
        val preferenceScreen = activityRule.activity.preferenceScreen as PreferenceScreen
        val securityCategory =
            activityRule.activity.findPreference("security_category") as PreferenceCategory
        val moreCategory =
            activityRule.activity.findPreference("more") as PreferenceCategory
        preferenceScreen.removePreference(securityCategory)
        preferenceScreen.removePreference(moreCategory)
    }

    @After
    fun tearDown() {
        //Clean preferences
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
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
        onView(
            withText(
                String.format(
                    activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
                    activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required)
                )
            )
        )
            .check(doesNotExist())
        onView(withText(R.string.prefs_camera_upload_behaviour_title)).check(doesNotExist())
        onView(withText(R.string.pref_behaviour_entries_keep_file)).check(doesNotExist())
    }

    @Test
    fun enablePictureUploads() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
        //Asserts
        assertTrue(mPrefCameraPictureUploads.isChecked)
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(matches(isDisplayed()))
    }

    @Test
    fun enableVideoUploads() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
        //Asserts
        assertTrue(mPrefCameraVideoUploads.isChecked)
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_video_upload_on_wifi)).check(matches(isDisplayed()))
    }

    @Test
    fun disablePictureUploadsAccept() {
        enableCameraPictureUploads()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        //Asserts
        assertFalse(mPrefCameraPictureUploads.isChecked)
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(doesNotExist())
    }

    @Test
    fun disablePictureUploadsRefuse() {
        enableCameraPictureUploads()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        //Asserts
        assertTrue(mPrefCameraPictureUploads.isChecked)
        onView(withText(R.string.prefs_camera_picture_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_picture_upload_on_wifi)).check(matches(isDisplayed()))
    }

    @Test
    fun disableVideoUploadsAccept() {
        enableCameraVideoUploads()
        onView(withText(R.string.prefs_camera_video_upload)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        //Asserts
        assertFalse(mPrefCameraVideoUploads.isChecked)
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(doesNotExist())
        onView(withText(R.string.camera_video_upload_on_wifi)).check(doesNotExist())
    }

    @Test
    fun disableVideoUploadsRefuse() {
        enableCameraVideoUploads()
        onView(withText(R.string.prefs_camera_video_upload)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        //Asserts
        assertTrue(mPrefCameraVideoUploads.isChecked)
        onView(withText(R.string.prefs_camera_video_upload_path_title)).check(matches(isDisplayed()))
        onView(withText(R.string.camera_video_upload_on_wifi)).check(matches(isDisplayed()))
    }

    @Test
    fun cameraFolderView() {
        enableCameraPictureUploads()
        //Asserts
        onView(
            withText(
                String.format(
                    activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
                    activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required)
                )
            )
        )
            .check(matches(isDisplayed()))
    }

    @Test
    fun cameraOpenPicker() {
        enableCameraPictureUploads()
        val cameraFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).absolutePath + "/Camera"
        Intents.init()
        //Asserts
        onView(
            withText(
                String.format(
                    activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title),
                    activityRule.activity.getString(R.string.prefs_camera_upload_source_path_title_required)
                )
            )
        )
            .perform(click())
        Intents.intended(IntentMatchers.hasComponent(LocalFolderPickerActivity::class.java.name))
        IntentMatchers.hasExtra(LocalFolderPickerActivity.EXTRA_PATH, cameraFolder)
        Intents.release()
        onView(withText(android.R.string.cancel)).perform(click())
    }

    @Test
    fun switchOriginalFileWillBe() {
        enableCameraPictureUploads()
        onData(PreferenceMatchers.withTitle(R.string.prefs_camera_upload_behaviour_title)).perform(click())
        onView(withText(R.string.pref_behaviour_entries_move)).perform(click())
        //Asserts
        onView(withText(R.string.pref_behaviour_entries_move)).check(matches(isDisplayed()))
    }

    private fun enableCameraPictureUploads() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
    }

    private fun enableCameraVideoUploads() {
        onView(withText(R.string.prefs_camera_video_upload)).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
    }
}
