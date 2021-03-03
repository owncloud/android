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

package com.owncloud.android.settings.more

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsFragmentMoreSectionTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsFragment>

    private lateinit var prefMoreCategory: PreferenceCategory
    private lateinit var prefHelp: Preference
    private lateinit var prefSync: Preference
    private lateinit var prefRecommend: Preference
    private lateinit var prefFeedback: Preference
    private lateinit var prefPrivacyPolicy: Preference
    private var prefImprint: Preference? = null
    private lateinit var prefAboutApp: Preference

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        settingsViewModel = mockk(relaxed = true)

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        settingsViewModel
                    }
                }
            )
        }

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            prefMoreCategory = fragment.findPreference(PREFERENCE_MORE_CATEGORY)!!
            prefHelp = fragment.findPreference(PREFERENCE_HELP)!!
            prefSync = fragment.findPreference(PREFERENCE_SYNC_CALENDAR_CONTACTS)!!
            prefRecommend = fragment.findPreference(PREFERENCE_RECOMMEND)!!
            prefFeedback = fragment.findPreference(PREFERENCE_FEEDBACK)!!
            prefPrivacyPolicy = fragment.findPreference(PREFERENCE_PRIVACY_POLICY)!!
            prefImprint = fragment.findPreference(PREFERENCE_IMPRINT)
            prefAboutApp = fragment.findPreference(PREFERENCE_ABOUT_APP)!!
        }
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        unmockkAll()
    }

    private fun launchTest(
        helpEnabled: Boolean = true,
        syncEnabled: Boolean = true,
        recommendEnabled: Boolean = true,
        feedbackEnabled: Boolean = true,
        privacyPolicyEnabled: Boolean = true,
        imprintEnabled: Boolean = true,
    ) {
        every { settingsViewModel.isHelpEnabled() } returns helpEnabled
        every { settingsViewModel.isSyncEnabled() } returns syncEnabled
        every { settingsViewModel.isRecommendEnabled() } returns recommendEnabled
        every { settingsViewModel.isFeedbackEnabled() } returns feedbackEnabled
        every { settingsViewModel.isPrivacyPolicyEnabled() } returns privacyPolicyEnabled
        every { settingsViewModel.isImprintEnabled() } returns imprintEnabled
    }

    @Test
    fun moreView() {
        onView(withText(R.string.prefs_category_more)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_MORE_CATEGORY, prefMoreCategory.key)
        assertEquals(context.getString(R.string.prefs_category_more), prefMoreCategory.title)
        assertEquals(null, prefMoreCategory.summary)
        assertTrue(prefMoreCategory.isVisible)

        // ADD HERE ALL THE SETTINGS (MERGE THE REST OF FUNCTIONS IN THIS ONE)
    }

    @Test
    fun helpView() {
        onView(withText(R.string.prefs_help)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_HELP, prefHelp.key)
        assertEquals(context.getString(R.string.prefs_help), prefHelp.title)
        assertEquals(null, prefHelp.summary)
        assertTrue(prefHelp.isVisible)
        assertTrue(prefHelp.isEnabled)
    }

    @Test
    fun syncView() {
        onView(withText(R.string.prefs_sync_calendar_contacts)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_sync_calendar_contacts_summary)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_SYNC_CALENDAR_CONTACTS, prefSync.key)
        assertEquals(context.getString(R.string.prefs_sync_calendar_contacts), prefSync.title)
        assertEquals(context.getString(R.string.prefs_sync_calendar_contacts_summary), prefSync.summary)
        assertTrue(prefSync.isVisible)
        assertTrue(prefSync.isEnabled)
    }

    @Test
    fun recommendView() {
        onView(withText(R.string.prefs_recommend)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_RECOMMEND, prefRecommend.key)
        assertEquals(context.getString(R.string.prefs_recommend), prefRecommend.title)
        assertEquals(null, prefRecommend.summary)
        assertTrue(prefRecommend.isVisible)
        assertTrue(prefRecommend.isEnabled)
    }

    @Test
    fun feedbackView() {
        onView(withText(R.string.prefs_feedback)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_FEEDBACK, prefFeedback.key)
        assertEquals(context.getString(R.string.prefs_feedback), prefFeedback.title)
        assertEquals(null, prefFeedback.summary)
        assertTrue(prefFeedback.isVisible)
        assertTrue(prefFeedback.isEnabled)
    }

    @Test
    fun privacyPolicyView() {
        onView(withText(R.string.prefs_privacy_policy)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_PRIVACY_POLICY, prefPrivacyPolicy.key)
        assertEquals(context.getString(R.string.prefs_privacy_policy), prefPrivacyPolicy.title)
        assertEquals(null, prefPrivacyPolicy.summary)
        assertTrue(prefPrivacyPolicy.isVisible)
        assertTrue(prefPrivacyPolicy.isEnabled)
    }

    /*@Test
    fun imprintView() {
        onView(withText(R.string.prefs_imprint)).check(matches(not(isDisplayed())))
        assertEquals(PREFERENCE_IMPRINT, prefImprint?.key)
        assertEquals(context.getString(R.string.prefs_imprint), prefImprint?.title)
        assertEquals(null, prefImprint?.summary)
        assertFalse(prefImprint.isVisible)
    }*/

    @Test
    fun aboutAppView() {
        val appVersion = BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + " " + BuildConfig.COMMIT_SHA1
        onView(withText(String.format(context.getString(R.string.about_android), context.getString(R.string.app_name)))).check(matches(isDisplayed()))
        onView(withText(String.format(context.getString(R.string.about_version), appVersion))).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_ABOUT_APP, prefAboutApp.key)
        assertEquals(String.format(context.getString(R.string.about_android), context.getString(R.string.app_name)), prefAboutApp.title)
        assertEquals(String.format(context.getString(R.string.about_version), appVersion), prefHelp.summary)
        assertTrue(prefAboutApp.isVisible)
        assertTrue(prefAboutApp.isEnabled)
    }

    companion object {
        private const val PREFERENCE_MORE_CATEGORY = "more_category"
        private const val PREFERENCE_HELP = "help"
        private const val PREFERENCE_SYNC_CALENDAR_CONTACTS = "syncCalendarContacts"
        private const val PREFERENCE_RECOMMEND = "recommend"
        private const val PREFERENCE_FEEDBACK = "feedback"
        private const val PREFERENCE_PRIVACY_POLICY = "privacyPolicy"
        private const val PREFERENCE_IMPRINT = "imprint"
        private const val PREFERENCE_ABOUT_APP = "about_app"
    }
}