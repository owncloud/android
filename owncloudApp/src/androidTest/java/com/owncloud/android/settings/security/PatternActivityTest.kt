/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
 * @author Juan Carlos Garrote Gascón (@JuancaG05)
 *
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.settings.security

import android.content.Intent
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.security.PatternActivity
import com.owncloud.android.presentation.viewmodels.security.PatternViewModel
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class PatternActivityTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(PatternActivity::class.java, true, false)

    private val intent = Intent()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val patternToSave = "1234"

    private lateinit var patternViewModel: PatternViewModel

    @Before
    fun setUp() {
        patternViewModel = mockk(relaxUnitFun = true)

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        patternViewModel
                    }
                }
            )
        }
    }

    @After
    fun tearDown() {
        //Clean preferences
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun patternLockView() {
        //Open Activity in pattern creation mode
        openPatternActivity(PatternActivity.ACTION_REQUEST_WITH_RESULT)

        onView(withText(R.string.pattern_configure_pattern)).check(matches(isDisplayed()))
        onView(withText(R.string.pattern_configure_your_pattern_explanation)).check(matches(isDisplayed()))
        onView(withId(R.id.pattern_lock_view)).check(matches(isDisplayed()))
        onView(withId(R.id.cancel_pattern)).check(matches(isDisplayed()))
    }

    @Test
    fun removePatternLock() {
        //Save a pattern in Preferences
        storePattern()

        //Open Activity in pattern deletion mode
        openPatternActivity(PatternActivity.ACTION_CHECK_WITH_RESULT)

        onView(withText(R.string.pattern_remove_pattern)).check(matches(isDisplayed()))
        onView(withText(R.string.pattern_no_longer_required)).check(matches(isDisplayed()))
    }

    private fun storePattern() {
        val appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit()
        appPrefs.apply {
            putString(PatternActivity.PREFERENCE_PATTERN, patternToSave)
            putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, true)
            apply()
        }
    }

    private fun openPatternActivity(mode: String) {
        intent.action = mode
        activityRule.launchActivity(intent)
    }
}
