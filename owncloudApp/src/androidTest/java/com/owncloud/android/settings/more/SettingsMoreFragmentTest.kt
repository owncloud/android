/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.presentation.settings.more.SettingsMoreFragment
import com.owncloud.android.presentation.settings.more.SettingsMoreViewModel
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

class SettingsMoreFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsMoreFragment>

    private var prefHelp: Preference? = null
    private var prefSync: Preference? = null
    private var prefAccessDocProvider: Preference? = null
    private var prefRecommend: Preference? = null
    private var prefFeedback: Preference? = null
    private var prefImprint: Preference? = null

    private lateinit var moreViewModel: SettingsMoreViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        moreViewModel = mockk(relaxed = true)

        stopKoin()

        startKoin {
            context
            allowOverride(override = true)
            modules(
                module {
                    viewModel {
                        moreViewModel
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
        docProviderAppEnabled: Boolean = true,
        recommendEnabled: Boolean = true,
        feedbackEnabled: Boolean = true,
        imprintEnabled: Boolean = true
    ) {
        every { moreViewModel.isHelpEnabled() } returns helpEnabled
        every { moreViewModel.isSyncEnabled() } returns syncEnabled
        every { moreViewModel.isDocProviderAppEnabled() } returns docProviderAppEnabled
        every { moreViewModel.isRecommendEnabled() } returns recommendEnabled
        every { moreViewModel.isFeedbackEnabled() } returns feedbackEnabled
        every { moreViewModel.isImprintEnabled() } returns imprintEnabled

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
    }

    @Test
    fun moreView() {
        launchTest()

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

        prefAccessDocProvider = getPreference(PREFERENCE_ACCESS_DOCUMENT_PROVIDER)
        assertNotNull(prefAccessDocProvider)
        prefAccessDocProvider?.verifyPreference(
            keyPref = PREFERENCE_ACCESS_DOCUMENT_PROVIDER,
            titlePref = context.getString(R.string.prefs_access_document_provider),
            summaryPref = context.getString(R.string.prefs_access_document_provider_summary),
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
            titlePref = context.getString(R.string.prefs_send_feedback),
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
    fun accessDocumentProviderNotEnabledView() {
        launchTest(docProviderAppEnabled = false)
        prefAccessDocProvider = getPreference(PREFERENCE_ACCESS_DOCUMENT_PROVIDER)

        assertNull(prefAccessDocProvider)
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
    fun imprintNotEnabledView() {
        launchTest(imprintEnabled = false)
        prefImprint = getPreference(PREFERENCE_IMPRINT)

        assertNull(prefImprint)
    }

    @Test
    fun helpOpensNotEmptyUrl() {
        every { moreViewModel.getHelpUrl() } returns context.getString(R.string.url_help)

        launchTest()

        mockIntent(action = Intent.ACTION_VIEW)
        onView(withText(R.string.prefs_help)).perform(click())

        intended(hasData(context.getString(R.string.url_help)))
    }

    @Test
    fun syncOpensNotEmptyUrl() {
        every { moreViewModel.getSyncUrl() } returns context.getString(R.string.url_sync_calendar_contacts)

        launchTest()

        mockIntent(action = Intent.ACTION_VIEW)
        onView(withText(R.string.prefs_sync_calendar_contacts)).perform(click())

        intended(hasData(context.getString(R.string.url_sync_calendar_contacts)))
    }

    @Test
    fun accessDocumentProviderOpensNotEmptyUrl() {
        every { moreViewModel.getDocProviderAppUrl() } returns context.getString(R.string.url_document_provider_app)

        launchTest()

        mockIntent(action = Intent.ACTION_VIEW)
        onView(withText(R.string.prefs_access_document_provider)).perform(click())

        intended(hasData(context.getString(R.string.url_document_provider_app)))
    }

    @Test
    fun recommendOpensSender() {
        launchTest()

        mockIntent(action = Intent.ACTION_SENDTO)

        onView(withText(R.string.prefs_recommend)).perform(click())
        // Delay needed since depending on the performance of the device where tests are executed,
        // sender can interfere with the subsequent tests
        Thread.sleep(1000)
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
    fun feedbackOpensSenderIfFeedbackMailExists() {
        launchTest()
        every { moreViewModel.getFeedbackMail() } returns FEEDBACK_MAIL
        mockIntent(action = Intent.ACTION_SENDTO)

        onView(withText(R.string.prefs_send_feedback)).perform(click())
        // Delay needed since depending on the performance of the device where tests are executed,
        // sender can interfere with the subsequent tests
        Thread.sleep(1000)
        intended(
            allOf(
                hasAction(Intent.ACTION_SENDTO),
                hasExtra(
                    EXTRA_SUBJECT,
                    "Android v" + BuildConfig.VERSION_NAME + " - " + context.getText(R.string.prefs_feedback)
                ),
                hasData(Uri.parse(FEEDBACK_MAIL)),
                hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )
    }

    @Test
    fun feedbackOpensAlertDialogIfFeedbackMailIsEmpty() {
        launchTest()
        every { moreViewModel.getFeedbackMail() } returns ""

        onView(withText(R.string.prefs_send_feedback)).perform(click())

        onView(withText(R.string.drawer_feedback)).check(ViewAssertions.matches(isDisplayed()))
        onView(withText(R.string.feedback_dialog_description)).check(ViewAssertions.matches(isDisplayed()))
    }

    @Test
    fun imprintOpensUrl() {
        every { moreViewModel.getImprintUrl() } returns "https://owncloud.com/mobile"

        launchTest()

        mockIntent(action = Intent.ACTION_VIEW)

        onView(withText(R.string.prefs_imprint)).perform(click())
        intended(hasData("https://owncloud.com/mobile"))
    }

    companion object {
        private const val PREFERENCE_HELP = "help"
        private const val PREFERENCE_SYNC_CALENDAR_CONTACTS = "syncCalendarContacts"
        private const val PREFERENCE_ACCESS_DOCUMENT_PROVIDER = "accessDocumentProvider"
        private const val PREFERENCE_RECOMMEND = "recommend"
        private const val PREFERENCE_FEEDBACK = "feedback"
        private const val PREFERENCE_IMPRINT = "imprint"
        private const val FEEDBACK_MAIL = "mailto:android-app@owncloud.com"

    }

}
