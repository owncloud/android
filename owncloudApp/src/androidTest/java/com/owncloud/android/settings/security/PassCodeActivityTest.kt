/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
 * @author Christian Schabesberger (@theScrabi)
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

import android.app.Activity
import android.content.Intent
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.withText
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.owncloud.android.presentation.viewmodels.security.PassCodeViewModel
import io.mockk.every
import io.mockk.mockk
import nthChildOf
import org.junit.Before
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import withChildViewCount

class PassCodeActivityTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(PassCodeActivity::class.java, true, false)

    private val intent = Intent()
    private val errorMessage = "PassCode Activity error"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val defaultPassCode = arrayOf('1', '1', '1', '1', '1', '1')
    private val wrongPassCode = arrayOf('1', '1', '1', '2', '2', '2')
    private val passCodeToSave = "111111"

    private lateinit var passCodeViewModel: PassCodeViewModel

    @Before
    fun setUp() {
        passCodeViewModel = mockk(relaxUnitFun = true)

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        passCodeViewModel
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
    fun passcodeView() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_configure_your_pass_code)
        }
        with(R.id.explanation) {
            isDisplayed(true)
            withText(R.string.pass_code_configure_your_pass_code_explanation)
        }

        // Check if required amount of input fields are actually displayed
        onView(withId(R.id.passCodeTxtLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.passCodeTxtLayout)).check(matches(withChildViewCount(PassCodeActivity.numberOfPassInputs, withId(R.id.passCodeEditText))))

        with(R.id.cancel) {
            isDisplayed(true)
            withText(android.R.string.cancel)
        }
    }

    @Test
    fun firstTry() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        //First typing
        typePasscode(defaultPassCode)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_reenter_your_pass_code)
        }
        onView(withText(R.string.pass_code_configure_your_pass_code)).check(doesNotExist())
    }

    @Test
    fun secondTryCorrect() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        //First typing
        typePasscode(defaultPassCode)
        //Second typing
        typePasscode(defaultPassCode)

        //Checking that the result returned is OK
        assertThat(activityRule.activityResult, hasResultCode(Activity.RESULT_OK))

        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    fun secondTryIncorrect() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        //First typing
        //Type incorrect passcode
        typePasscode(defaultPassCode)
        //Second typing
        typePasscode(wrongPassCode)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_configure_your_pass_code)
        }
        with(R.id.explanation) {
            isDisplayed(true)
            withText(R.string.pass_code_configure_your_pass_code_explanation)
        }
        with(R.id.error) {
            isDisplayed(true)
            withText(R.string.pass_code_mismatch)
        }
    }

    @Test
    fun cancelFirstTry() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        for (i in 0..2) {
            onView(nthChildOf(withId(R.id.passCodeTxtLayout), i)).perform(replaceText("1"))
        }

        onView(withId(R.id.cancel)).perform(click())
        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    fun cancelSecondTry() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        //First typing
        typePasscode(defaultPassCode)

        //Type incorrect passcode
        for (i in 0..1) {
            onView(nthChildOf(withId(R.id.passCodeTxtLayout), i)).perform(replaceText("1"))
        }

        onView(withId(R.id.cancel)).perform(click())
        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    fun deletePasscodeView() {
        //Save a passcode in Preferences
        storePasscode()

        //Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_remove_your_pass_code)
        }
    }

    @Test
    fun deletePasscodeCorrect() {
        every { passCodeViewModel.checkPassCodeIsValid(any()) } returns true

        //Save a passcode in Preferences
        storePasscode(passCodeToSave)

        //Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        //Type correct passcode
        typePasscode(defaultPassCode)

        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    fun deletePasscodeIncorrect() {
        every { passCodeViewModel.checkPassCodeIsValid(any()) } returns false

        //Save a passcode in Preferences
        storePasscode(passCodeToSave)

        //Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        //Type incorrect passcode
        typePasscode(wrongPassCode)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_enter_pass_code)
        }
        with(R.id.error) {
            isDisplayed(true)
            withText(R.string.pass_code_wrong)
        }
    }

    private fun openPasscodeActivity(mode: String) {
        intent.action = mode
        activityRule.launchActivity(intent)
    }

    private fun typePasscode(digits: Array<Char>) {
        for (i in 0 until PassCodeActivity.numberOfPassInputs)
            onView(nthChildOf(withId(R.id.passCodeTxtLayout), i)).perform(replaceText(digits[i].toString()))
    }

    private fun storePasscode(passcode: String = passCodeToSave) {
        val appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit()

        appPrefs.putString(PassCodeActivity.PREFERENCE_PASSCODE, passcode.substring(0, PassCodeActivity.numberOfPassInputs))
        appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        appPrefs.apply()
    }
}
