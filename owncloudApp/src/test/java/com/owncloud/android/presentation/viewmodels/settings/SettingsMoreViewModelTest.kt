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

package com.owncloud.android.presentation.viewmodels.settings

import com.owncloud.android.R
import com.owncloud.android.presentation.settings.more.SettingsMoreViewModel
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.testutil.OC_SECURE_BASE_URL
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsMoreViewModelTest : ViewModelTest() {
    private lateinit var moreViewModel: SettingsMoreViewModel
    private lateinit var contextProvider: ContextProvider

    @Before
    fun setUp() {
        contextProvider = mockk()

        moreViewModel = SettingsMoreViewModel(contextProvider)
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `is help enabled - ok - true`() {
        every { contextProvider.getBoolean(any()) } returns true

        val helpEnabled = moreViewModel.isHelpEnabled()

        assertTrue(helpEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.help_enabled)
        }
    }

    @Test
    fun `is help enabled - ok - false`() {
        every { contextProvider.getBoolean(any()) } returns false

        val helpEnabled = moreViewModel.isHelpEnabled()

        assertFalse(helpEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.help_enabled)
        }
    }

    @Test
    fun `get help url - ok`() {
        every { contextProvider.getString(any()) } returns OC_SECURE_BASE_URL

        val helpUrl = moreViewModel.getHelpUrl()

        assertEquals(OC_SECURE_BASE_URL, helpUrl)

        verify(exactly = 1) {
            contextProvider.getString(R.string.url_help)
        }
    }

    @Test
    fun `is sync enabled - ok - true`() {
        every { contextProvider.getBoolean(any()) } returns true

        val syncEnabled = moreViewModel.isSyncEnabled()

        assertTrue(syncEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.sync_calendar_contacts_enabled)
        }
    }

    @Test
    fun `is sync enabled - ok - false`() {
        every { contextProvider.getBoolean(any()) } returns false

        val syncEnabled = moreViewModel.isSyncEnabled()

        assertFalse(syncEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.sync_calendar_contacts_enabled)
        }
    }

    @Test
    fun `get sync url - ok`() {
        every { contextProvider.getString(any()) } returns OC_SECURE_BASE_URL

        val syncUrl = moreViewModel.getSyncUrl()

        assertEquals(OC_SECURE_BASE_URL, syncUrl)

        verify(exactly = 1) {
            contextProvider.getString(R.string.url_sync_calendar_contacts)
        }
    }

    @Test
    fun `is access doc provider - ok - true`() {
        every { contextProvider.getBoolean(any()) } returns true

        val accessDocProviderEnabled = moreViewModel.isDocProviderAppEnabled()

        assertTrue(accessDocProviderEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.access_document_provider_app_enabled)
        }
    }

    @Test
    fun `is access doc provider - ok - false`() {
        every { contextProvider.getBoolean(any()) } returns false

        val accessDocProviderEnabled = moreViewModel.isDocProviderAppEnabled()

        assertFalse(accessDocProviderEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.access_document_provider_app_enabled)
        }
    }

    @Test
    fun `get access doc provider url - ok`() {
        every { contextProvider.getString(any()) } returns OC_SECURE_BASE_URL

        val accessDocProviderUrl = moreViewModel.getDocProviderAppUrl()

        assertEquals(OC_SECURE_BASE_URL, accessDocProviderUrl)

        verify(exactly = 1) {
            contextProvider.getString(R.string.url_document_provider_app)
        }
    }

    @Test
    fun `is recommend enabled - ok - true`() {
        every { contextProvider.getBoolean(any()) } returns true

        val recommendEnabled = moreViewModel.isRecommendEnabled()

        assertTrue(recommendEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.recommend_enabled)
        }
    }

    @Test
    fun `is recommend enabled - ok - false`() {
        every { contextProvider.getBoolean(any()) } returns false

        val recommendEnabled = moreViewModel.isRecommendEnabled()

        assertFalse(recommendEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.recommend_enabled)
        }
    }

    @Test
    fun `is feedback enabled - ok - true`() {
        every { contextProvider.getBoolean(any()) } returns true

        val feedbackEnabled = moreViewModel.isFeedbackEnabled()

        assertTrue(feedbackEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.feedback_enabled)
        }
    }

    @Test
    fun `is feedback enabled - ok - false`() {
        every { contextProvider.getBoolean(any()) } returns false

        val feedbackEnabled = moreViewModel.isFeedbackEnabled()

        assertFalse(feedbackEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.feedback_enabled)
        }
    }

    @Test
    fun `is privacy policy enabled - ok - true`() {
        every { contextProvider.getBoolean(any()) } returns true

        val privacyPolicyEnabled = moreViewModel.isPrivacyPolicyEnabled()

        assertTrue(privacyPolicyEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.privacy_policy_enabled)
        }
    }

    @Test
    fun `is privacy policy enabled - ok - false`() {
        every { contextProvider.getBoolean(any()) } returns false

        val privacyPolicyEnabled = moreViewModel.isPrivacyPolicyEnabled()

        assertFalse(privacyPolicyEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.privacy_policy_enabled)
        }
    }

    @Test
    fun `is imprint enabled - ok - true`() {
        every { contextProvider.getBoolean(any()) } returns true

        val imprintEnabled = moreViewModel.isImprintEnabled()

        assertTrue(imprintEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.imprint_enabled)
        }
    }

    @Test
    fun `is imprint enabled - ok - false`() {
        every { contextProvider.getBoolean(any()) } returns false

        val imprintEnabled = moreViewModel.isImprintEnabled()

        assertFalse(imprintEnabled)

        verify(exactly = 1) {
            contextProvider.getBoolean(R.bool.imprint_enabled)
        }
    }

    @Test
    fun `get imprint url - ok`() {
        every { contextProvider.getString(any()) } returns OC_SECURE_BASE_URL

        val imprintUrl = moreViewModel.getImprintUrl()

        assertEquals(OC_SECURE_BASE_URL, imprintUrl)

        verify(exactly = 1) {
            contextProvider.getString(R.string.url_imprint)
        }
    }

}
