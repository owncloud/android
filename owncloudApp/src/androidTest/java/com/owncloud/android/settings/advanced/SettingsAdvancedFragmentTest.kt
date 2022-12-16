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

package com.owncloud.android.settings.advanced

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.SwitchPreferenceCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.R
import com.owncloud.android.presentation.settings.advanced.SettingsAdvancedFragment
import com.owncloud.android.presentation.settings.advanced.SettingsAdvancedFragment.Companion.PREF_SHOW_HIDDEN_FILES
import com.owncloud.android.presentation.settings.advanced.SettingsAdvancedViewModel
import com.owncloud.android.utils.matchers.verifyPreference
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsAdvancedFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsAdvancedFragment>

    private var prefShowHiddenFiles: SwitchPreferenceCompat? = null

    private lateinit var advancedViewModel: SettingsAdvancedViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        advancedViewModel = mockk(relaxed = true)

        stopKoin()

        startKoin {
            context
            allowOverride(override = true)
            modules(
                module {
                    viewModel {
                        advancedViewModel
                    }
                }
            )
        }

        every { advancedViewModel.isHiddenFilesShown() } returns true

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)

        fragmentScenario.onFragment { fragment ->
            prefShowHiddenFiles = fragment.findPreference(PREF_SHOW_HIDDEN_FILES)
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun advancedView() {
        assertNotNull(prefShowHiddenFiles)
        prefShowHiddenFiles?.verifyPreference(
            keyPref = PREF_SHOW_HIDDEN_FILES,
            titlePref = context.getString(R.string.prefs_show_hidden_files),
            visible = true,
            enabled = true
        )
    }

    @Test
    fun disableShowHiddenFiles() {
        prefShowHiddenFiles?.isChecked = advancedViewModel.isHiddenFilesShown()

        onView(withText(context.getString(R.string.prefs_show_hidden_files))).perform(click())

        prefShowHiddenFiles?.isChecked?.let { assertFalse(it) }
    }
}
