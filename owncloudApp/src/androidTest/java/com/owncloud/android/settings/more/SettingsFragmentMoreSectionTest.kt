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
import android.content.Intent
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.PrivacyPolicyActivity
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import com.owncloud.android.utils.matchers.verifyPreference
import com.owncloud.android.utils.mockIntent
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsFragmentMoreSectionTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsFragment>

    private lateinit var prefSecurityCategory: PreferenceCategory
    private lateinit var prefLogsCategory: PreferenceCategory
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
        settingsViewModel = mockk(relaxUnitFun = true)

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

    private fun getPreference(key: String): Preference? {
        var preference: Preference? = null
        fragmentScenario.onFragment { fragment ->
            preference = fragment.findPreference(key)
        }
        return preference
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

        // These categories and preferences are not brandable
        fragmentScenario.onFragment { fragment ->
            prefSecurityCategory = fragment.findPreference(PREFERENCE_SECURITY_CATEGORY)!!
            prefLogsCategory = fragment.findPreference(PREFERENCE_LOGS_CATEGORY)!!
            prefMoreCategory = fragment.findPreference(PREFERENCE_MORE_CATEGORY)!!
            prefAboutApp = fragment.findPreference(PREFERENCE_ABOUT_APP)!!
        }

        prefSecurityCategory.isVisible = false
        prefLogsCategory.isVisible = false

        // Not a good solution but tests only pass if this is here
        Thread.sleep(250)
    }

    @Test
    fun moreView() {
        launchTest()

        prefMoreCategory.verifyPreference(
            keyPref = PREFERENCE_MORE_CATEGORY,
            titlePref = context.getString(R.string.prefs_subsection_more),
            visible = true
        )

        prefHelp = getPreference(PREFERENCE_HELP)
        assertNotNull(prefHelp)
        prefHelp?.verifyPreference(
            keyPref = PREFERENCE_HELP,
            titlePref = context.getString(R.string.prefs_help),
            visible = true,
            enabled = true
        )

        prefSync = getPreference(PREFERENCE_SYNC_CALENDAR_CONTACTS)
        assertNotNull(prefSync)
        prefSync?.verifyPreference(
            keyPref = PREFERENCE_SYNC_CALENDAR_CONTACTS,
            titlePref = context.getString(R.string.prefs_sync_calendar_contacts),
            summaryPref = context.getString(R.string.prefs_sync_calendar_contacts_summary),
            visible = true,
            enabled = true
        )

        prefRecommend = getPreference(PREFERENCE_RECOMMEND)
        assertNotNull(prefRecommend)
        prefRecommend?.verifyPreference(
            keyPref = PREFERENCE_RECOMMEND,
            titlePref = context.getString(R.string.prefs_recommend),
            visible = true,
            enabled = true
        )

        prefFeedback = getPreference(PREFERENCE_FEEDBACK)
        assertNotNull(prefFeedback)
        prefFeedback?.verifyPreference(
            keyPref = PREFERENCE_FEEDBACK,
            titlePref = context.getString(R.string.prefs_feedback),
            visible = true,
            enabled = true
        )

        prefPrivacyPolicy = getPreference(PREFERENCE_PRIVACY_POLICY)
        assertNotNull(prefPrivacyPolicy)
        prefPrivacyPolicy?.verifyPreference(
            keyPref = PREFERENCE_PRIVACY_POLICY,
            titlePref = context.getString(R.string.prefs_privacy_policy),
            visible = true,
            enabled = true
        )

        prefImprint = getPreference(PREFERENCE_IMPRINT)
        assertNotNull(prefImprint)
        prefImprint?.verifyPreference(
            keyPref = PREFERENCE_IMPRINT,
            titlePref = context.getString(R.string.prefs_imprint),
            visible = true,
            enabled = true
        )

        prefAboutApp.verifyPreference(
            keyPref = PREFERENCE_ABOUT_APP,
            titlePref = context.getString(R.string.prefs_app_version),
            summaryPref = String.format(
                context.getString(R.string.prefs_app_version_summary),
                context.getString(R.string.app_name),
                BuildConfig.BUILD_TYPE,
                BuildConfig.VERSION_NAME,
                BuildConfig.COMMIT_SHA1
            ),
            visible = true,
            enabled = true
        )
    }

    @Test
    fun helpNotEnabledView() {
        launchTest(helpEnabled = false)
        prefHelp = getPreference(PREFERENCE_HELP)

        assertNull(prefHelp)
    }

    @Test
    fun syncNotEnabledView() {
        launchTest(syncEnabled = false)
        prefSync = getPreference(PREFERENCE_SYNC_CALENDAR_CONTACTS)

        assertNull(prefSync)
    }

    @Test
    fun recommendNotEnabledView() {
        launchTest(recommendEnabled = false)
        prefRecommend = getPreference(PREFERENCE_RECOMMEND)

        assertNull(prefRecommend)
    }

    @Test
    fun feedbackNotEnabledView() {
        launchTest(feedbackEnabled = false)
        prefFeedback = getPreference(PREFERENCE_FEEDBACK)

        assertNull(prefFeedback)
    }

    @Test
    fun privacyPolicyNotEnabledView() {
        launchTest(privacyPolicyEnabled = false)
        prefPrivacyPolicy = getPreference(PREFERENCE_PRIVACY_POLICY)

        assertNull(prefPrivacyPolicy)
    }

    @Test
    fun imprintNotEnabledView() {
        launchTest(imprintEnabled = false)
        prefImprint = getPreference(PREFERENCE_IMPRINT)

        assertNull(prefImprint)
    }

    @Test
    fun helpOpensNotEmptyUrl() {
        every { settingsViewModel.getHelpUrl() } returns context.getString(R.string.url_help)

        launchTest()

        onView(withText(R.string.prefs_help)).perform(click())
        mockIntent(action = Intent.ACTION_VIEW)
        intended(hasData(context.getString(R.string.url_help)))
    }

    @Test
    fun syncOpensNotEmptyUrl() {
        every { settingsViewModel.getSyncUrl() } returns context.getString(R.string.url_sync_calendar_contacts)

        launchTest()

        onView(withText(R.string.prefs_sync_calendar_contacts)).perform(click())
        mockIntent(action = Intent.ACTION_VIEW)
        intended(hasData(context.getString(R.string.url_sync_calendar_contacts)))
    }

    @Test
    fun recommendOpensSender() {
        launchTest()

        onView(withText(R.string.prefs_recommend)).perform(click())
        mockIntent(action = Intent.ACTION_SENDTO)
        intended(
            allOf(
                hasAction(Intent.ACTION_SENDTO), hasExtra(
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

    @Test
    fun feedbackOpensSender() {
        launchTest()

        onView(withText(R.string.prefs_feedback)).perform(click())
        mockIntent(action = Intent.ACTION_SENDTO)
        intended(
            allOf(
                hasAction(Intent.ACTION_SENDTO),
                hasExtra(
                    EXTRA_SUBJECT,
                    "Android v" + BuildConfig.VERSION_NAME + " - " + context.getText(R.string.prefs_feedback)
                ),
                hasData(Uri.parse(context.getString(R.string.mail_feedback))),
                hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )
    }

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
        mockIntent(action = Intent.ACTION_VIEW)
        intended(hasData("https://owncloud.com/mobile"))
    }

    companion object {
        private const val PREFERENCE_SECURITY_CATEGORY = "security_category"
        private const val PREFERENCE_LOGS_CATEGORY = "logs_category"
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
