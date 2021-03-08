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

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SENDTO
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import com.owncloud.android.ui.activity.PrivacyPolicyActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsFragmentMoreSectionTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsFragment>

    private lateinit var prefSecurityCategory: PreferenceCategory
    private lateinit var prefMoreCategory: PreferenceCategory
    private var prefHelp: Preference? = null
    private var prefSync: Preference? = null
    private var prefRecommend: Preference? = null
    private var prefFeedback: Preference? = null
    private var prefPrivacyPolicy: Preference? = null
    private var prefImprint: Preference? = null
    private lateinit var prefAboutApp: Preference

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        settingsViewModel = mockk(relaxed = true)

        prefHelp = null
        prefSync = null
        prefRecommend = null
        prefFeedback = null
        prefPrivacyPolicy = null
        prefImprint = null

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

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)

        fragmentScenario.onFragment { fragment ->
            prefSecurityCategory = fragment.findPreference(PREFERENCE_SECURITY_CATEGORY)!!
            prefMoreCategory = fragment.findPreference(PREFERENCE_MORE_CATEGORY)!!
            if (helpEnabled) prefHelp = fragment.findPreference(PREFERENCE_HELP)!!
            if (syncEnabled) prefSync = fragment.findPreference(PREFERENCE_SYNC_CALENDAR_CONTACTS)!!
            if (recommendEnabled) prefRecommend = fragment.findPreference(PREFERENCE_RECOMMEND)!!
            if (feedbackEnabled) prefFeedback = fragment.findPreference(PREFERENCE_FEEDBACK)!!
            if (privacyPolicyEnabled) prefPrivacyPolicy = fragment.findPreference(PREFERENCE_PRIVACY_POLICY)!!
            if (imprintEnabled) prefImprint = fragment.findPreference(PREFERENCE_IMPRINT)!!
            prefAboutApp = fragment.findPreference(PREFERENCE_ABOUT_APP)!!
        }

        prefSecurityCategory.isVisible = false
    }

    @Test
    fun moreView() {
        launchTest()

        onView(withText(R.string.prefs_category_more)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_MORE_CATEGORY, prefMoreCategory.key)
        assertEquals(context.getString(R.string.prefs_category_more), prefMoreCategory.title)
        assertNull(prefMoreCategory.summary)
        assertTrue(prefMoreCategory.isVisible)

        onView(withText(R.string.prefs_help)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_HELP, prefHelp?.key)
        assertEquals(context.getString(R.string.prefs_help), prefHelp?.title)
        assertNull(prefHelp?.summary)
        assertTrue(prefHelp?.isVisible == true)
        assertTrue(prefHelp?.isEnabled == true)

        onView(withText(R.string.prefs_sync_calendar_contacts)).check(matches(isDisplayed()))
        onView(withText(R.string.prefs_sync_calendar_contacts_summary)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_SYNC_CALENDAR_CONTACTS, prefSync?.key)
        assertEquals(context.getString(R.string.prefs_sync_calendar_contacts), prefSync?.title)
        assertEquals(context.getString(R.string.prefs_sync_calendar_contacts_summary), prefSync?.summary)
        assertTrue(prefSync?.isVisible == true)
        assertTrue(prefSync?.isEnabled == true)

        onView(withText(R.string.prefs_recommend)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_RECOMMEND, prefRecommend?.key)
        assertEquals(context.getString(R.string.prefs_recommend), prefRecommend?.title)
        assertNull(prefRecommend?.summary)
        assertTrue(prefRecommend?.isVisible == true)
        assertTrue(prefRecommend?.isEnabled == true)

        onView(withText(R.string.prefs_feedback)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_FEEDBACK, prefFeedback?.key)
        assertEquals(context.getString(R.string.prefs_feedback), prefFeedback?.title)
        assertNull(prefFeedback?.summary)
        assertTrue(prefFeedback?.isVisible == true)
        assertTrue(prefFeedback?.isEnabled == true)

        onView(withText(R.string.prefs_privacy_policy)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_PRIVACY_POLICY, prefPrivacyPolicy?.key)
        assertEquals(context.getString(R.string.prefs_privacy_policy), prefPrivacyPolicy?.title)
        assertNull(prefPrivacyPolicy?.summary)
        assertTrue(prefPrivacyPolicy?.isVisible == true)
        assertTrue(prefPrivacyPolicy?.isEnabled == true)

        onView(withText(R.string.prefs_imprint)).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_IMPRINT, prefImprint?.key)
        assertEquals(context.getString(R.string.prefs_imprint), prefImprint?.title)
        assertNull(prefImprint?.summary)
        assertTrue(prefImprint?.isVisible == true)

        val appVersion = BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + " " + BuildConfig.COMMIT_SHA1
        onView(
            withText(
                String.format(
                    context.getString(R.string.about_android),
                    context.getString(R.string.app_name)
                )
            )
        ).check(matches(isDisplayed()))
        onView(
            withText(
                String.format(
                    context.getString(R.string.about_version),
                    appVersion
                )
            )
        ).check(matches(isDisplayed()))
        assertEquals(PREFERENCE_ABOUT_APP, prefAboutApp.key)
        assertEquals(
            String.format(context.getString(R.string.about_android), context.getString(R.string.app_name)),
            prefAboutApp.title
        )
        assertEquals(String.format(context.getString(R.string.about_version), appVersion), prefAboutApp.summary)
        assertTrue(prefAboutApp.isVisible)
        assertTrue(prefAboutApp.isEnabled)
    }

    @Test
    fun helpNotEnabledView() {
        launchTest(helpEnabled = false)

        assertNull(prefHelp)
    }

    @Test
    fun syncNotEnabledView() {
        launchTest(syncEnabled = false)

        assertNull(prefSync)
    }

    @Test
    fun recommendNotEnabledView() {
        launchTest(recommendEnabled = false)

        assertNull(prefRecommend)
    }

    @Test
    fun feedbackNotEnabledView() {
        launchTest(feedbackEnabled = false)

        assertNull(prefFeedback)
    }

    @Test
    fun privacyPolicyNotEnabledView() {
        launchTest(privacyPolicyEnabled = false)

        assertNull(prefPrivacyPolicy)
    }

    @Test
    fun imprintNotEnabledView() {
        launchTest(imprintEnabled = false)

        assertNull(prefImprint)
    }

    @Ignore
    @Test
    fun helpOpensNotEmptyUrl() {
        every { settingsViewModel.getHelpUrl() } returns context.getString(R.string.url_help)

        launchTest()

        onView(withText(R.string.prefs_help)).perform(click())
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult)
        intended(hasData(context.getString(R.string.url_help)))
    }

    @Test
    fun syncOpensNotEmptyUrl() {
        every { settingsViewModel.getSyncUrl() } returns context.getString(R.string.url_sync_calendar_contacts)

        launchTest()

        onView(withText(R.string.prefs_sync_calendar_contacts)).perform(click())
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult)
        intended(hasData(context.getString(R.string.url_sync_calendar_contacts)))
    }

    @Test
    fun recommendOpensSender() {
        launchTest()

        onView(withText(R.string.prefs_recommend)).perform(click())
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(ACTION_SENDTO)).respondWith(intentResult)
        intended(
            allOf(
                hasAction(ACTION_SENDTO), hasExtra(
                    EXTRA_SUBJECT, String.format(
                        context.getString(R.string.recommend_subject),
                        context.getString(R.string.app_name)
                    )
                ),
                hasExtra(
                    EXTRA_TEXT,
                    String.format(
                        context.getString(R.string.recommend_text),
                        context.getString(R.string.app_name),
                        context.getString(R.string.url_app_download)
                    )
                ),
                hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )
    }

    @Ignore
    @Test
    fun feedbackOpensSender() {
        launchTest()

        onView(withText(R.string.prefs_feedback)).perform(click())
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(ACTION_SENDTO)).respondWith(intentResult)
        intended(
            allOf(
                hasAction(ACTION_SENDTO),
                hasExtra(
                    EXTRA_SUBJECT,
                    "Android v" + BuildConfig.VERSION_NAME + " - " + context.getText(R.string.prefs_feedback)
                ),
                hasData(Uri.parse(context.getString(R.string.mail_feedback))),
                hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )
    }

    @Ignore
    @Test
    fun privacyPolicyOpensPrivacyPolicyActivity() {
        launchTest()

        onView(withText(R.string.prefs_privacy_policy)).perform(click())
        intended(hasComponent(PrivacyPolicyActivity::class.java.name))
    }

    @Test
    fun imprintOpensUrl() {
        every { settingsViewModel.getImprintUrl() } returns "https://owncloud.com/mobile"

        launchTest()

        onView(withText(R.string.prefs_imprint)).perform(click())
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(intentResult)
        intended(hasData("https://owncloud.com/mobile"))
    }

    companion object {
        private const val PREFERENCE_SECURITY_CATEGORY = "security_category"
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
