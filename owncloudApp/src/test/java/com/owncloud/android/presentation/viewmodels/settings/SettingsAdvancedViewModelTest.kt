/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.presentation.settings.advanced.RemoveLocalFiles
import com.owncloud.android.presentation.settings.advanced.SettingsAdvancedFragment.Companion.PREF_SHOW_HIDDEN_FILES
import com.owncloud.android.presentation.settings.advanced.SettingsAdvancedViewModel
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.workers.RemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker.Companion.DELETE_FILES_OLDER_GIVEN_TIME_WORKER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SettingsAdvancedViewModelTest {
    private lateinit var advancedViewModel: SettingsAdvancedViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var workManagerProvider: WorkManagerProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk()
        workManagerProvider = mockk(relaxed = true)

        advancedViewModel = SettingsAdvancedViewModel(
            preferencesProvider,
            workManagerProvider,
        )
    }

    @Test
    fun `is hidden files shown - ok - true`() {
        every { preferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, any()) } returns true

        val shown = advancedViewModel.isHiddenFilesShown()

        Assert.assertTrue(shown)
    }

    @Test
    fun `is hidden files shown - ok - false`() {
        every { preferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, any()) } returns false

        val shown = advancedViewModel.isHiddenFilesShown()

        Assert.assertFalse(shown)
    }

    @Test
    fun `scheduleDeleteLocalFiles cancels the worker and enqueues a new worker when Never option is not selected`() {
        val newValue = RemoveLocalFiles.ONE_HOUR.name

        advancedViewModel.scheduleDeleteLocalFiles(newValue)

        verify(exactly = 1) {
            workManagerProvider.cancelAllWorkByTag(DELETE_FILES_OLDER_GIVEN_TIME_WORKER)
            workManagerProvider.enqueueRemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker()
        }
    }

    @Test
    fun `scheduleDeleteLocalFiles cancels the worker when Never option is selected`() {
        val newValue = RemoveLocalFiles.NEVER.name

        advancedViewModel.scheduleDeleteLocalFiles(newValue)

        verify(exactly = 1) {
            workManagerProvider.cancelAllWorkByTag(DELETE_FILES_OLDER_GIVEN_TIME_WORKER)
        }
    }
}
