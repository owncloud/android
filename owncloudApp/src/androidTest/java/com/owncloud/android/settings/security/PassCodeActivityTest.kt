/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
 * @author Christian Schabesberger (@theScrabi)
 * @author Juan Carlos Garrote Gascón (@JuancaG05)
 * @author David Crespo Ríos (@davcres)
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
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.security.passcode.PassCodeActivity
import com.owncloud.android.presentation.security.passcode.PasscodeAction
import com.owncloud.android.presentation.security.passcode.PasscodeType
import com.owncloud.android.presentation.security.passcode.Status
import com.owncloud.android.presentation.security.biometric.BiometricViewModel
import com.owncloud.android.presentation.security.passcode.PassCodeViewModel
import com.owncloud.android.testutil.security.OC_PASSCODE_4_DIGITS
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.withChildCountAndId
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
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
    private lateinit var statusLiveData: MutableLiveData<Status>
    private lateinit var passcodeLiveData: MutableLiveData<String>

    private lateinit var passCodeViewModel: PassCodeViewModel
    private lateinit var biometricViewModel: BiometricViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        passCodeViewModel = mockk(relaxed = true)
        biometricViewModel = mockk(relaxed = true)

        timeToUnlockLiveData = MutableLiveData()
        finishTimeToUnlockLiveData = MutableLiveData()
        statusLiveData = MutableLiveData()
        passcodeLiveData = MutableLiveData()

        stopKoin()

        startKoin {
            allowOverride(override = true)
            context
            modules(
                module {
                    viewModel {
                        passCodeViewModel
                    }
                    viewModel {
                        biometricViewModel
                    }
                }
            )
        }

        every { passCodeViewModel.getPassCode() } returns OC_PASSCODE_4_DIGITS
        every { passCodeViewModel.getNumberOfPassCodeDigits() } returns 4
        every { passCodeViewModel.getNumberOfAttempts() } returns 0
        every { passCodeViewModel.getTimeToUnlockLiveData } returns timeToUnlockLiveData
        every { passCodeViewModel.getFinishedTimeToUnlockLiveData } returns finishTimeToUnlockLiveData
        every { passCodeViewModel.status } returns statusLiveData
        every { passCodeViewModel.passcode } returns passcodeLiveData
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
        with(R.id.layout_code) {
            isDisplayed(true)
            withChildCountAndId(passCodeViewModel.getNumberOfPassCodeDigits(), R.id.passCodeEditText)
        }

        R.id.lock_time.isDisplayed(false)

        R.id.numberKeyboard.isDisplayed(true)
        R.id.key0.isDisplayed(true)
        R.id.key1.isDisplayed(true)
        R.id.key2.isDisplayed(true)
        R.id.key3.isDisplayed(true)
        R.id.key4.isDisplayed(true)
        R.id.key5.isDisplayed(true)
        R.id.key6.isDisplayed(true)
        R.id.key7.isDisplayed(true)
        R.id.key8.isDisplayed(true)
        R.id.key9.isDisplayed(true)
        R.id.backspaceBtn.isDisplayed(true)
        R.id.biometricBtn.isDisplayed(false)
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
        with(R.id.layout_code) {
            isDisplayed(true)
            withChildCountAndId(passCodeViewModel.getNumberOfPassCodeDigits(), R.id.passCodeEditText)
        }

        R.id.lock_time.isDisplayed(true)
    }

    @Test
    fun passcodeView() {
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_CREATE)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_configure_your_pass_code)
        }
        with(R.id.explanation) {
            isDisplayed(true)
            withText(R.string.pass_code_configure_your_pass_code_explanation)
        }

        // Check if required amount of input fields are actually displayed
        with(R.id.layout_code) {
            isDisplayed(true)
            withChildCountAndId(passCodeViewModel.getNumberOfPassCodeDigits(), R.id.passCodeEditText)
        }

        R.id.lock_time.isDisplayed(false)

        R.id.error.isDisplayed(false)
    }

    @Test
    fun firstTry() {
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_CREATE)

        statusLiveData.postValue(Status(PasscodeAction.CREATE, PasscodeType.NO_CONFIRM))

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_reenter_your_pass_code)
        }
        onView(withText(R.string.pass_code_configure_your_pass_code)).check(doesNotExist())

        R.id.error.isDisplayed(false)
    }

    @Test
    fun secondTryCorrect() {
        every { biometricViewModel.isBiometricLockAvailable() } returns true

        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_CREATE)

        statusLiveData.postValue(Status(PasscodeAction.CREATE, PasscodeType.CONFIRM))

        // Click dialog's enable option
        onView(withText(R.string.common_yes)).perform(click())

        // Checking that the result returned is OK
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
    }

    @Test
    fun secondTryIncorrect() {
        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_CREATE)

        statusLiveData.postValue(Status(PasscodeAction.CREATE, PasscodeType.ERROR))

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

        R.id.lock_time.isDisplayed(false)
    }

    @Test
    fun deletePasscodeView() {
        // Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_REMOVE)

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_remove_your_pass_code)
        }

        R.id.explanation.isDisplayed(false)

        R.id.error.isDisplayed(false)

        R.id.lock_time.isDisplayed(false)
    }

    @Test
    fun deletePasscodeCorrect() {
        // Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_REMOVE)

        statusLiveData.postValue(Status(PasscodeAction.REMOVE, PasscodeType.OK))

        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
    }

    @Test
    fun deletePasscodeIncorrect() {
        // Open Activity in passcode deletion mode
        openPasscodeActivity(PassCodeActivity.ACTION_REMOVE)

        statusLiveData.postValue(Status(PasscodeAction.REMOVE, PasscodeType.ERROR))

        with(R.id.header) {
            isDisplayed(true)
            withText(R.string.pass_code_enter_pass_code)
        }
        with(R.id.error) {
            isDisplayed(true)
            withText(R.string.pass_code_wrong)
        }

        R.id.explanation.isDisplayed(false)

        R.id.lock_time.isDisplayed(false)
    }

    @Test
    fun checkEnableBiometricDialogIsVisible() {
        every { biometricViewModel.isBiometricLockAvailable() } returns true

        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_CREATE)

        statusLiveData.postValue(Status(PasscodeAction.CREATE, PasscodeType.CONFIRM))

        onView(withText(R.string.biometric_dialog_title)).check(matches(isDisplayed()))
        onView(withText(R.string.common_yes)).check(matches(isDisplayed()))
        onView(withText(R.string.common_no)).check(matches(isDisplayed()))
    }

    @Test
    fun checkEnableBiometricDialogYesOption() {
        every { biometricViewModel.isBiometricLockAvailable() } returns true

        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_CREATE)

        statusLiveData.postValue(Status(PasscodeAction.CREATE, PasscodeType.CONFIRM))

        onView(withText(R.string.common_yes)).perform(click())

        // Checking that the result returned is OK
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
    }

    @Test
    fun checkEnableBiometricDialogNoOption() {
        every { biometricViewModel.isBiometricLockAvailable() } returns true

        // Open Activity in passcode creation mode
        openPasscodeActivity(PassCodeActivity.ACTION_CREATE)

        statusLiveData.postValue(Status(PasscodeAction.CREATE, PasscodeType.CONFIRM))

        onView(withText(R.string.common_no)).perform(click())

        // Checking that the result returned is OK
        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
    }

    private fun openPasscodeActivity(mode: String) {
        val intent = Intent(context, PassCodeActivity::class.java).apply {
            action = mode
        }
        activityScenario = ActivityScenario.launch(intent)
    }
}
