/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
 * Copyright (C) 2022 ownCloud GmbH.
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

import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.settings.fragments.SettingsAdvancedFragment.Companion.PREF_SHOW_HIDDEN_FILES
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SettingsAdvancedViewModelTest {
    private lateinit var advancedViewModel: SettingsAdvancedViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk()

        advancedViewModel = SettingsAdvancedViewModel(preferencesProvider)
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
}