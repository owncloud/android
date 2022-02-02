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
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment.Companion.BIOMETRIC_ENABLED_FROM_DIALOG_EXTRA
import com.owncloud.android.presentation.viewmodels.security.BiometricViewModel
import com.owncloud.android.presentation.viewmodels.security.PassCodeViewModel
import com.owncloud.android.testutil.security.OC_PASSCODE_4_DIGITS
import com.owncloud.android.testutil.security.OC_PASSCODE_6_DIGITS
import com.owncloud.android.utils.click
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.nthChildOf
import com.owncloud.android.utils.matchers.withChildCountAndId
import com.owncloud.android.utils.matchers.withText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class PassCodeActivityTest {

    private lateinit var activityScenario: ActivityScenario<PassCodeActivity>

    private lateinit var context: Context

    private lateinit var timeToUnlockLiveData: MutableLiveData<Event<String>>
    private lateinit var finishTimeToUnlockLiveData: MutableLiveData<Event<Boolean>>

    private val defaultPassCode = arrayOf('1', '1', '1', '1', '1', '1')
    private val wrongPassCode = arrayOf('1', '1', '1', '2', '2', '2')

    private lateinit var passCodeViewModel: PassCodeViewModel
    private lateinit var biometricViewModel: BiometricViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        passCodeViewModel = mockk(relaxUnitFun = true)
        biometricViewModel = mockk(relaxUnitFun = true)

        timeToUnlockLiveData = MutableLiveData()
        finishTimeToUnlockLiveData = MutableLiveData()

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

        every { passCodeViewModel.getPassCode() } returns OC_PASSCODE_4_DIGITS
        every { passCodeViewModel.getNumberOfPassCodeDigits() } returns 4
        every { passCodeViewModel.getNumberOfAttempts() } returns 0
        every { passCodeViewModel.getTimeToUnlockLiveData } returns timeToUnlockLiveData
        every { passCodeViewModel.getFinishedTimeToUnlockLiveData } returns finishTimeToUnlockLiveData
    }

    @After
    fun tearDown() {
        // Clean preferences
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun passcodeCheckNotLockedView() {
        // Open Activity in passcode check mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_enter_pass_code)
        }

        R.id.explanation.isDisplayed(false)

        // Check if required amount of input fields are actually displayed
        with(R.id.passCodeTxtLayout) {
            isDisplayed(true)
            withChildCountAndId(passCodeViewModel.getNumberOfPassCodeDigits(), R.id.passCodeEditText)
        }

        R.id.cancel.isDisplayed(false)

        R.id.lock_time.isDisplayed(false)
    }

    @Test
    fun passcodeCheckLockedView() {
        every { passCodeViewModel.getNumberOfAttempts() } returns 3
        every { passCodeViewModel.getTimeToUnlockLeft() } returns 3000

        timeToUnlockLiveData.postValue(Event("00:03"))

        // Open Activity in passcode check mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_enter_pass_code)
        }

        R.id.explanation.isDisplayed(false)

        // Check if required amount of input fields are actually displayed
        with(R.id.passCodeTxtLayout) {
            isDisplayed(true)
            withChildCountAndId(passCodeViewModel.getNumberOfPassCodeDigits(), R.id.passCodeEditText)
        }

        R.id.cancel.isDisplayed(false)

        R.id.lock_time.isDisplayed(true)
    }

    @Test
    fun passcodeView() {
        // Open Activity in passcode creation mode
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
        with(R.id.passCodeTxtLayout) {
            isDisplayed(true)
            withChildCountAndId(passCodeViewModel.getNumberOfPassCodeDigits(), R.id.passCodeEditText)
        }

        with(R.id.cancel) {
            isDisplayed(true)
            withText(android.R.string.cancel)
        }

        R.id.lock_time.isDisplayed(false)
    }

    @Ignore("Flaky test, it fails many times")
    @Test
    fun passcodeViewCancelButton() {
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        R.id.cancel.click()

        assertEquals(activityScenario.result.resultCode, Activity.RESULT_CANCELED)
    }

    @Test
    fun firstTry() {
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        // First typing
        typePasscode(defaultPassCode)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_reenter_your_pass_code)
        }
        onView(withText(R.string.pass_code_configure_your_pass_code)).check(doesNotExist())
    }

    @Test
    fun secondTryCorrect() {
        every { passCodeViewModel.isBiometricLockAvailable() } returns true

        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        // First typing
        typePasscode(defaultPassCode)
        // Second typing
        typePasscode(defaultPassCode)

        // Click dialog's enable option
        onView(withText(R.string.biometric_dialog_yes_option)).perform(click())

        // Checking that the result returned is OK
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
    }

    @Test
    fun secondTryIncorrect() {
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        // First typing
        // Type incorrect passcode
        typePasscode(defaultPassCode)
        // Second typing
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
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        for (i in 0..2) {
            onView(nthChildOf(withId(R.id.passCodeTxtLayout), i)).perform(replaceText("1"))
        }

        R.id.cancel.click()
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_CANCELED)
    }

    @Test
    fun cancelSecondTry() {
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        // First typing
        typePasscode(defaultPassCode)

        // Type incomplete passcode
        for (i in 0..1) {
            onView(nthChildOf(withId(R.id.passCodeTxtLayout), i)).perform(replaceText("1"))
        }

        R.id.cancel.click()
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_CANCELED)
    }

    @Test
    fun deletePasscodeView() {
        // Save a passcode in Preferences
        storePasscode()

        // Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_remove_your_pass_code)
        }
    }

    @Ignore("Flaky test, it fails many times")
    @Test
    fun deletePasscodeViewCancelButton() {
        // Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        R.id.cancel.click()
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_CANCELED)
    }

    @Test
    fun deletePasscodeCorrect() {
        every { passCodeViewModel.checkPassCodeIsValid(any()) } returns true

        // Save a passcode in Preferences
        storePasscode(OC_PASSCODE_6_DIGITS)

        // Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        // Type correct passcode
        typePasscode(defaultPassCode)

        verify { passCodeViewModel.removePassCode() }
    }

    @Test
    fun deletePasscodeIncorrect() {
        every { passCodeViewModel.checkPassCodeIsValid(any()) } returns false

        // Save a passcode in Preferences
        storePasscode(OC_PASSCODE_6_DIGITS)

        // Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_CHECK_WITH_RESULT)

        // Type incorrect passcode
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

    @Test
    fun checkEnableBiometricDialogIsVisible() {
        every { passCodeViewModel.isBiometricLockAvailable() } returns true

        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        // First typing
        typePasscode(defaultPassCode)
        // Second typing
        typePasscode(defaultPassCode)

        // Check if dialog is visible
        onView(withText(R.string.biometric_dialog_yes_option)).inRoot(isDialog()).check(matches(isDisplayed())).perform(click())

        // Checking that the result returned is OK
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
        assertEquals(activityScenario.result.resultData.getBooleanExtra(BIOMETRIC_ENABLED_FROM_DIALOG_EXTRA, false), true)

    }

    @Test
    fun checkEnableBiometricDialogIsNotVisible() {
        every { passCodeViewModel.isBiometricLockAvailable() } returns false

        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)

        // First typing
        typePasscode(defaultPassCode)
        // Second typing
        typePasscode(defaultPassCode)

        // Checking that the result returned is false
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
        assertEquals(activityScenario.result.resultData.getBooleanExtra(BIOMETRIC_ENABLED_FROM_DIALOG_EXTRA, false), false)
    }

    private fun openPasscodeActivity(mode: String) {
        val intent = Intent(context, PassCodeActivity::class.java).apply {
            action = mode
        }
        activityScenario = ActivityScenario.launch(intent)
    }

    private fun typePasscode(digits: Array<Char>) {
        for (i in 0 until passCodeViewModel.getNumberOfPassCodeDigits())
            onView(nthChildOf(withId(R.id.passCodeTxtLayout), i)).perform(replaceText(digits[i].toString()))
    }

    private fun storePasscode(passcode: String = OC_PASSCODE_6_DIGITS) {
        val appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit()

        appPrefs.putString(PassCodeActivity.PREFERENCE_PASSCODE, passcode.substring(0, passCodeViewModel.getNumberOfPassCodeDigits()))
        appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        appPrefs.apply()
    }
}
