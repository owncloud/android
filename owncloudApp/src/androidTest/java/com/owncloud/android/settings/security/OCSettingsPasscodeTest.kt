/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
 * Copyright (C) 2020 ownCloud GmbH.
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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.PassCodeActivity
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class OCSettingsPasscodeTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(PassCodeActivity::class.java, true, false)

    private val intent = Intent()
    private val errorMessage = "PassCode Activity error"
    private val keyPassCode = "KEY_PASSCODE"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val defaultPassCode = arrayOf('1', '1', '1', '1')
    private val wrongPassCode = arrayOf('1', '1', '1', '2')
    private val passCodeToSave = "1111"

    @After
    fun tearDown() {
        //Clean preferences
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun passcodeView() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        onView(withId(R.id.header)).check(matches(isDisplayed()))
        onView(withId(R.id.explanation)).check(matches(isDisplayed()))
        onView(withText(R.string.pass_code_configure_your_pass_code)).check(matches(isDisplayed()))
        onView(withText(R.string.pass_code_configure_your_pass_code_explanation)).check(matches(isDisplayed()))
        onView(withId(R.id.txt0)).check(matches(isDisplayed()))
        onView(withId(R.id.txt1)).check(matches(isDisplayed()))
        onView(withId(R.id.txt2)).check(matches(isDisplayed()))
        onView(withId(R.id.txt3)).check(matches(isDisplayed()))
        onView(withId(R.id.cancel)).check(matches(isDisplayed()))
    }

    @Test
    fun firstTry() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        //First typing
        typePasscode(defaultPassCode)

        onView(withText(R.string.pass_code_reenter_your_pass_code)).check(matches(isDisplayed()))
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

        //Checking that the setResult returns the typed passcode
        assertThat(activityRule.activityResult, hasResultCode(Activity.RESULT_OK))
        assertThat(activityRule.activityResult, hasResultData(hasExtra(keyPassCode, passCodeToSave)))

        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    @Ignore
    fun secondTryIncorrect() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        //First typing
        typePasscode(defaultPassCode)
        //Second typing
        typePasscode(wrongPassCode)

        onView(withText(R.string.pass_code_reenter_your_pass_code)).check(doesNotExist())
        onView(withText(R.string.pass_code_configure_your_pass_code)).check(matches(isDisplayed()))
        onView(withText(R.string.pass_code_configure_your_pass_code_explanation)).check(matches(isDisplayed()))
        onView(withText(R.string.pass_code_mismatch)).check(matches(isDisplayed()))
    }

    @Test
    fun cancelFirstTry() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        onView(withId(R.id.txt0)).perform(replaceText("1"))
        onView(withId(R.id.txt1)).perform(replaceText("1"))
        onView(withId(R.id.txt2)).perform(replaceText("1"))

        onView(withId(R.id.cancel)).perform(click())
        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    fun cancelSecondTry() {
        //Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        //First typing
        typePasscode(defaultPassCode)

        onView(withId(R.id.txt0)).perform(replaceText("1"))
        onView(withId(R.id.txt1)).perform(replaceText("1"))

        onView(withId(R.id.cancel)).perform(click())
        assertTrue(errorMessage, activityRule.activity.isFinishing)
    }

    @Test
    fun deletePasscodeView() {
        //Save a passcode in Preferences
        storePasscode()

        //Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        onView(withText(R.string.pass_code_remove_your_pass_code)).check(matches(isDisplayed()))
    }

    @Test
    fun deletePasscodeCorrect() {
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
        //Save a passcode in Preferences
        storePasscode(passCodeToSave)

        //Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        //Type incorrect passcode
        typePasscode(wrongPassCode)

        onView(withText(R.string.pass_code_enter_pass_code)).check(matches(isDisplayed()))
    }

    private fun openPasscodeActivity(mode: String) {
        intent.action = mode
        activityRule.launchActivity(intent)
    }

    private fun typePasscode(digits: Array<Char>) {
        onView(withId(R.id.txt0)).perform(replaceText(digits[0].toString()))
        onView(withId(R.id.txt1)).perform(replaceText(digits[1].toString()))
        onView(withId(R.id.txt2)).perform(replaceText(digits[2].toString()))
        onView(withId(R.id.txt3)).perform(replaceText(digits[3].toString()))
    }

    private fun storePasscode(passcode: String = passCodeToSave) {
        val appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit()
        for (i in 1..4) {
            appPrefs.putString(
                PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                passcode.substring(i - 1, i)
            )
        }
        appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        appPrefs.apply()
    }

}
