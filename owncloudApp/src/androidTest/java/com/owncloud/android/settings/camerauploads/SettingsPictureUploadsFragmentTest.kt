/**
 * ownCloud Android client application
 *
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

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_SOURCE
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY
import com.owncloud.android.presentation.ui.settings.fragments.SettingsPictureUploadsFragment
import com.owncloud.android.presentation.viewmodels.settings.SettingsPictureUploadsViewModel
import com.owncloud.android.utils.matchers.verifyPreference
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@Ignore("Needs a little refactor")
class SettingsPictureUploadsFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsPictureUploadsFragment>

    private lateinit var prefEnablePictureUploads: SwitchPreferenceCompat
    private lateinit var prefPictureUploadsPath: Preference
    private lateinit var prefPictureUploadsOnWifi: CheckBoxPreference
    private lateinit var prefPictureUploadsSourcePath: Preference
    private lateinit var prefPictureUploadsBehaviour: ListPreference
    private lateinit var prefPictureUploadsAccount: ListPreference

    private lateinit var picturesViewModel: SettingsPictureUploadsViewModel
    private lateinit var context: Context

    private val exampleUploadPath = "/Upload/Path"
    private val exampleUploadSourcePath = "/Upload/Source/Path"
    private val listOfLoggedAccounts = arrayOf("first", "second", "third")

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        picturesViewModel = mockk(relaxUnitFun = true)

        every { picturesViewModel.getPictureUploadsPath() } returns exampleUploadPath
        every { picturesViewModel.getPictureUploadsSourcePath() } returns exampleUploadSourcePath
        every { picturesViewModel.getLoggedAccountNames() } returns listOfLoggedAccounts
        every { picturesViewModel.getPictureUploadsAccount() } returns listOfLoggedAccounts.first()

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        picturesViewModel
                    }
                }
            )
        }

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            prefEnablePictureUploads = fragment.findPreference(PREF__CAMERA_PICTURE_UPLOADS_ENABLED)!!
            prefPictureUploadsPath = fragment.findPreference(PREF__CAMERA_PICTURE_UPLOADS_PATH)!!
            prefPictureUploadsOnWifi = fragment.findPreference(PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY)!!
            prefPictureUploadsSourcePath = fragment.findPreference(PREF__CAMERA_PICTURE_UPLOADS_SOURCE)!!
            prefPictureUploadsBehaviour = fragment.findPreference(PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR)!!
            prefPictureUploadsAccount = fragment.findPreference(PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME)!!
        }
    }

    @After
    fun tearDown() {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun pictureUploadsView() {
        prefEnablePictureUploads.verifyPreference(
            keyPref = PREF__CAMERA_PICTURE_UPLOADS_ENABLED,
            titlePref = context.getString(R.string.prefs_camera_picture_upload),
            summaryPref = context.getString(R.string.prefs_camera_picture_upload_summary),
            visible = true,
            enabled = true
        )
        assertFalse(prefEnablePictureUploads.isChecked)

        prefPictureUploadsPath.verifyPreference(
            keyPref = PREF__CAMERA_PICTURE_UPLOADS_PATH,
            titlePref = context.getString(R.string.prefs_camera_picture_upload_path_title),
            summaryPref = exampleUploadPath,
            visible = true,
            enabled = false
        )

        prefPictureUploadsOnWifi.verifyPreference(
            keyPref = PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY,
            titlePref = context.getString(R.string.prefs_camera_picture_upload_on_wifi),
            visible = true,
            enabled = false
        )
        assertFalse(prefPictureUploadsOnWifi.isChecked)

        val comment =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) context.getString(
                R.string.prefs_camera_upload_source_path_title_optional
            )
            else context.getString(
                R.string.prefs_camera_upload_source_path_title_required
            )
        prefPictureUploadsSourcePath.verifyPreference(
            keyPref = PREF__CAMERA_PICTURE_UPLOADS_SOURCE,
            titlePref = String.format(prefPictureUploadsSourcePath.title.toString(), comment),
            summaryPref = exampleUploadSourcePath,
            visible = true,
            enabled = false
        )

        prefPictureUploadsBehaviour.verifyPreference(
            keyPref = PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR,
            titlePref = context.getString(R.string.prefs_camera_upload_behaviour_title),
            summaryPref = context.getString(R.string.pref_behaviour_entries_keep_file),
            visible = true,
            enabled = false
        )

        prefPictureUploadsAccount.verifyPreference(
            keyPref = PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME,
            titlePref = context.getString(R.string.prefs_picture_upload_account),
            summaryPref = prefPictureUploadsAccount.context.getString(androidx.preference.R.string.not_set),
            visible = true,
            enabled = false
        )
    }

    @Test
    fun enablePictureUploads() {
        firstEnablePictureUploads()
        checkPreferencesEnabled(true)
        checkPreferencesInitialized()
    }

    @Test
    fun disablePictureUploadsAccept() {
        firstEnablePictureUploads()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        checkPreferencesEnabled(false)
        checkPreferencesReset()
    }

    @Test
    fun disablePictureUploadsRefuse() {
        firstEnablePictureUploads()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        checkPreferencesEnabled(true)
    }

    @Ignore("Makes the subsequent tests crash. Will have to be updated when changed to Android's file picker")
    @Test
    fun openPictureUploadSourcePathPicker() {
        firstEnablePictureUploads()
        val cameraFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        ).absolutePath + "/Camera"
        Intents.init()
        onView(
            withText(
                String.format(
                    context.getString(R.string.prefs_camera_upload_source_path_title),
                    context.getString(R.string.prefs_camera_upload_source_path_title_required)
                )
            )
        ).perform(click())
//        intended(hasComponent(LocalFolderPickerActivity::class.java.name))
//        hasExtra(LocalFolderPickerActivity.EXTRA_PATH, cameraFolder)
        Intents.release()
        onView(withText(android.R.string.cancel)).perform(click())
    }

    private fun firstEnablePictureUploads() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
    }

    private fun checkPreferencesEnabled(enabled: Boolean) {
        assertEquals(enabled, prefEnablePictureUploads.isChecked)
        assertEquals(enabled, prefPictureUploadsPath.isEnabled)
        assertEquals(enabled, prefPictureUploadsOnWifi.isEnabled)
        assertEquals(enabled, prefPictureUploadsSourcePath.isEnabled)
        assertEquals(enabled, prefPictureUploadsBehaviour.isEnabled)
        assertEquals(enabled, prefPictureUploadsAccount.isEnabled)
    }

    private fun checkPreferencesInitialized() {
        assertEquals(listOfLoggedAccounts.first(), prefPictureUploadsAccount.summary)
        assertEquals(exampleUploadPath, prefPictureUploadsPath.summary)
    }

    private fun checkPreferencesReset() {
        assertEquals(context.getString(androidx.preference.R.string.not_set), prefPictureUploadsAccount.summary)
        assertEquals(picturesViewModel.getPictureUploadsPath(), prefPictureUploadsPath.summary)
    }
}
