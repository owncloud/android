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

package com.owncloud.android.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.presentation.viewmodels.settings.SettingsMoreViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import com.owncloud.android.utils.matchers.verifyPreference
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsFragment>

    private lateinit var subsectionSecurity: Preference
    private lateinit var subsectionLogging: Preference
    private lateinit var subsectionPictureUploads: Preference
    private lateinit var subsectionVideoUploads: Preference
    private lateinit var subsectionMore: Preference
    private lateinit var prefAboutApp: Preference

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var moreViewModel: SettingsMoreViewModel
    private lateinit var context: Context

    private lateinit var version: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        settingsViewModel = mockk(relaxUnitFun = true)
        moreViewModel = mockk(relaxUnitFun = true)

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        settingsViewModel
                    }
                    viewModel {
                        moreViewModel
                    }
                }
            )
        }

        version = String.format(
            context.getString(R.string.prefs_app_version_summary),
            context.getString(R.string.app_name),
            BuildConfig.BUILD_TYPE,
            BuildConfig.VERSION_NAME,
            BuildConfig.COMMIT_SHA1
        )
    }

    private fun launchTest(attachedAccount: Boolean, moreSectionVisible: Boolean = true) {
        every { settingsViewModel.isThereAttachedAccount() } returns attachedAccount
        every { moreViewModel.shouldMoreSectionBeVisible() } returns moreSectionVisible

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            subsectionSecurity = fragment.findPreference(SUBSECTION_SECURITY)!!
            subsectionLogging = fragment.findPreference(SUBSECTION_LOGGING)!!
            subsectionPictureUploads = fragment.findPreference(SUBSECTION_PICTURE_UPLOADS)!!
            subsectionVideoUploads = fragment.findPreference(SUBSECTION_VIDEO_UPLOADS)!!
            subsectionMore = fragment.findPreference(SUBSECTION_MORE)!!
            prefAboutApp = fragment.findPreference(PREFERENCE_ABOUT_APP)!!
        }
    }

    @Test
    fun settingsViewCommon() {
        launchTest(attachedAccount = false)

        subsectionSecurity.verifyPreference(
            keyPref = SUBSECTION_SECURITY,
            titlePref = context.getString(R.string.prefs_subsection_security),
            summaryPref = context.getString(R.string.prefs_subsection_security_summary),
            visible = true,
            enabled = true
        )

        subsectionLogging.verifyPreference(
            keyPref = SUBSECTION_LOGGING,
            titlePref = context.getString(R.string.prefs_subsection_logging),
            summaryPref = context.getString(R.string.prefs_subsection_logging_summary),
            visible = true,
            enabled = true
        )

        subsectionMore.verifyPreference(
            keyPref = SUBSECTION_MORE,
            titlePref = context.getString(R.string.prefs_subsection_more),
            summaryPref = context.getString(R.string.prefs_subsection_more_summary),
            visible = true,
            enabled = true
        )

        prefAboutApp.verifyPreference(
            keyPref = PREFERENCE_ABOUT_APP,
            titlePref = context.getString(R.string.prefs_app_version),
            summaryPref = version,
            visible = true,
            enabled = true
        )
    }

    @Test
    fun settingsViewNoAccountAttached() {
        launchTest(attachedAccount = false)

        subsectionPictureUploads.verifyPreference(
            keyPref = SUBSECTION_PICTURE_UPLOADS,
            titlePref = context.getString(R.string.prefs_subsection_picture_uploads),
            summaryPref = context.getString(R.string.prefs_subsection_picture_uploads_summary),
            visible = false
        )

        subsectionVideoUploads.verifyPreference(
            keyPref = SUBSECTION_VIDEO_UPLOADS,
            titlePref = context.getString(R.string.prefs_subsection_video_uploads),
            summaryPref = context.getString(R.string.prefs_subsection_video_uploads_summary),
            visible = false
        )
    }

    @Test
    fun settingsMoreSectionHidden() {
        launchTest(attachedAccount = false, moreSectionVisible = false)

        subsectionMore.verifyPreference(
            keyPref = SUBSECTION_MORE,
            titlePref = context.getString(R.string.prefs_subsection_more),
            summaryPref = context.getString(R.string.prefs_subsection_more_summary),
            visible = false
        )
    }

    @Test
    fun settingsViewAccountAttached() {
        launchTest(attachedAccount = true)

        subsectionPictureUploads.verifyPreference(
            keyPref = SUBSECTION_PICTURE_UPLOADS,
            titlePref = context.getString(R.string.prefs_subsection_picture_uploads),
            summaryPref = context.getString(R.string.prefs_subsection_picture_uploads_summary),
            visible = true,
            enabled = true
        )

        subsectionVideoUploads.verifyPreference(
            keyPref = SUBSECTION_VIDEO_UPLOADS,
            titlePref = context.getString(R.string.prefs_subsection_video_uploads),
            summaryPref = context.getString(R.string.prefs_subsection_video_uploads_summary),
            visible = true,
            enabled = true
        )
    }

    @Test
    fun clickOnAppVersion() {
        launchTest(attachedAccount = false)

        onView(withText(R.string.prefs_app_version)).perform(click())
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?

        onView(withText(R.string.clipboard_text_copied)).check(matches(isEnabled()))
        assertEquals(version, clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context))
    }

    companion object {
        private const val SUBSECTION_SECURITY = "security_subsection"
        private const val SUBSECTION_LOGGING = "logging_subsection"
        private const val SUBSECTION_PICTURE_UPLOADS = "picture_uploads_subsection"
        private const val SUBSECTION_VIDEO_UPLOADS = "video_uploads_subsection"
        private const val SUBSECTION_MORE = "more_subsection"
        private const val PREFERENCE_ABOUT_APP = "about_app"
    }
}
