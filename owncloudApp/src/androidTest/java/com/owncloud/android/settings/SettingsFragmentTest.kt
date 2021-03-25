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
import com.owncloud.android.utils.matchers.verifyPreference
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsFragment>

    private lateinit var prefSecurity: Preference
    private lateinit var prefLogging: Preference
    private lateinit var prefMore: Preference
    private lateinit var prefAboutApp: Preference

    private lateinit var context: Context

    private lateinit var version: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            prefSecurity = fragment.findPreference(PREFERENCE_SECURITY)!!
            prefLogging = fragment.findPreference(PREFERENCE_LOGGING)!!
            prefMore = fragment.findPreference(PREFERENCE_MORE)!!
            prefAboutApp = fragment.findPreference(PREFERENCE_ABOUT_APP)!!
        }
        version = String.format(
            context.getString(R.string.prefs_app_version_summary),
            context.getString(R.string.app_name),
            BuildConfig.BUILD_TYPE,
            BuildConfig.VERSION_NAME,
            BuildConfig.COMMIT_SHA1
        )
    }

    @Test
    fun settingsView() {
        prefSecurity.verifyPreference(
            keyPref = PREFERENCE_SECURITY,
            titlePref = context.getString(R.string.prefs_subsection_security),
            summaryPref = context.getString(R.string.prefs_subsection_security_summary),
            visible = true,
            enabled = true
        )

        prefLogging.verifyPreference(
            keyPref = PREFERENCE_LOGGING,
            titlePref = context.getString(R.string.prefs_subsection_logging),
            summaryPref = context.getString(R.string.prefs_subsection_logging_summary),
            visible = true,
            enabled = true
        )

        prefMore.verifyPreference(
            keyPref = PREFERENCE_MORE,
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
    fun clickOnAppVersion() {
        onView(withText(R.string.prefs_app_version)).perform(click())
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?

        onView(withText(R.string.clipboard_text_copied)).check(matches(isEnabled()))
        assertEquals(version, clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context))
    }

    companion object {
        private const val PREFERENCE_SECURITY = "security_subsection"
        private const val PREFERENCE_LOGGING = "logging_subsection"
        private const val PREFERENCE_MORE = "more_subsection"
        private const val PREFERENCE_ABOUT_APP = "about_app"
    }
}
