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

package com.owncloud.android.presentation.viewmodels.security

import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.security.PatternActivity
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PatternViewModelTest : ViewModelTest() {
    private lateinit var patternViewModel: PatternViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider

    private val pattern = "1234"

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        patternViewModel = PatternViewModel(preferencesProvider)
    }

    @Test
    fun `set pattern - ok`() {
        patternViewModel.setPattern(pattern)

        verify(exactly = 1) {
            preferencesProvider.putString(PatternActivity.PREFERENCE_PATTERN, pattern)
            preferencesProvider.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, true)
        }
    }

    @Test
    fun `remove pattern - ok`() {
        patternViewModel.removePattern()

        verify(exactly = 1) {
            preferencesProvider.removePreference(PatternActivity.PREFERENCE_PATTERN)
            preferencesProvider.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)
        }
    }

    @Test
    fun `check pattern is valid - ok`() {
        every { preferencesProvider.getString(any(), any()) } returns pattern

        val patternValue = "1234"

        val patternCheckResult = patternViewModel.checkPatternIsValid(patternValue)

        assertTrue(patternCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PatternActivity.PREFERENCE_PATTERN, null)
        }
    }

    @Test
    fun `check pattern is valid - ko - saved pattern is null`() {
        every { preferencesProvider.getString(any(), any()) } returns null

        val patternValue = "1234"

        val patternCheckResult = patternViewModel.checkPatternIsValid(patternValue)

        assertFalse(patternCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PatternActivity.PREFERENCE_PATTERN, null)
        }
    }

    @Test
    fun `check pattern is valid - ko - different pattern`() {
        every { preferencesProvider.getString(any(), any()) } returns pattern

        val patternValue = "1235"

        val patternCheckResult = patternViewModel.checkPatternIsValid(patternValue)

        assertFalse(patternCheckResult)

        verify(exactly = 1) {
            preferencesProvider.getString(PatternActivity.PREFERENCE_PATTERN, null)
        }
    }

}
