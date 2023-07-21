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

package com.owncloud.android.presentation.security.passcode

import android.os.CountDownTimer
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.R
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.security.PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP
import com.owncloud.android.presentation.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.security.biometric.BiometricActivity
import com.owncloud.android.presentation.settings.security.SettingsSecurityFragment.Companion.PREFERENCE_LOCK_ATTEMPTS
import com.owncloud.android.providers.ContextProvider
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.pow

class PassCodeViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider,
    private val action: PasscodeAction
) : ViewModel() {

    private val _getTimeToUnlockLiveData = MutableLiveData<Event<String>>()
    val getTimeToUnlockLiveData: LiveData<Event<String>>
        get() = _getTimeToUnlockLiveData

    private val _getFinishedTimeToUnlockLiveData = MutableLiveData<Event<Boolean>>()
    val getFinishedTimeToUnlockLiveData: LiveData<Event<Boolean>>
        get() = _getFinishedTimeToUnlockLiveData

    private var _passcode = MutableLiveData<String>()
    val passcode: LiveData<String>
        get() = _passcode

    private var _status = MutableLiveData<Status>()
    val status: LiveData<Status>
        get() = _status

    private var numberOfPasscodeDigits: Int
    private var passcodeString = StringBuilder()
    private lateinit var firstPasscode: String
    private var confirmingPassCode = false

    init {
        numberOfPasscodeDigits = (getPassCode()?.length ?: getNumberOfPassCodeDigits())
    }

    fun onNumberClicked(number: Int) {
        if (passcodeString.length < numberOfPasscodeDigits && (getNumberOfAttempts() < 3 || getTimeToUnlockLeft() == 0.toLong())) {
            passcodeString.append(number.toString())
            _passcode.postValue(passcodeString.toString())

            if (passcodeString.length == numberOfPasscodeDigits) {
                processFullPassCode()
            }
        }
    }

    fun onBackspaceClicked() {
        if (passcodeString.isNotEmpty()) {
            passcodeString.deleteCharAt(passcodeString.length - 1)
            _passcode.postValue(passcodeString.toString())
        }
    }

    /**
     * Processes the passcode entered by the user just after the last digit was in.
     *
     * Takes into account the action requested to the activity, the currently saved pass code and
     * the previously typed pass code, if any.
     */
    private fun processFullPassCode() {
        when (action) {
            PasscodeAction.CHECK -> {
                actionCheckPasscode()
            }
            PasscodeAction.REMOVE -> {
                actionRemovePasscode()
            }
            PasscodeAction.CREATE -> {
                actionCreatePasscode()
            }
        }
    }

    private fun actionCheckPasscode() {
        if (checkPassCodeIsValid(passcodeString.toString())) {
            // pass code accepted in request, user is allowed to access the app
            setLastUnlockTimestamp()
            val passCode = getPassCode()
            if (passCode != null && passCode.length < getNumberOfPassCodeDigits()) {
                setMigrationRequired(true)
                removePassCode()
                _status.postValue(Status(PasscodeAction.CHECK, PasscodeType.MIGRATION))
            }
            _status.postValue(Status(PasscodeAction.CHECK, PasscodeType.OK))
            resetNumberOfAttempts()
        } else {
            increaseNumberOfAttempts()
            passcodeString = StringBuilder()
            _status.postValue(Status(PasscodeAction.CHECK, PasscodeType.ERROR))
        }
    }

    private fun actionRemovePasscode() {
        if (checkPassCodeIsValid(passcodeString.toString())) {
            removePassCode()
            _status.postValue(Status(PasscodeAction.REMOVE, PasscodeType.OK))
        } else {
            passcodeString = StringBuilder()
            _status.postValue(Status(PasscodeAction.REMOVE, PasscodeType.ERROR))
        }
    }

    private fun actionCreatePasscode() {
        // enabling pass code
        if (!confirmingPassCode) {
            requestPassCodeConfirmation()
            _status.postValue(Status(PasscodeAction.CREATE, PasscodeType.NO_CONFIRM))
        } else if (confirmPassCode()) {
            setPassCode()
            _status.postValue(Status(PasscodeAction.CREATE, PasscodeType.CONFIRM))
        } else {
            passcodeString = StringBuilder()
            _status.postValue(Status(PasscodeAction.CREATE, PasscodeType.ERROR))
        }
    }

    fun getPassCode() = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, loadPinFromOldFormatIfPossible())

    fun setPassCode() {
        preferencesProvider.putString(PassCodeActivity.PREFERENCE_PASSCODE, firstPasscode)
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        numberOfPasscodeDigits = (getPassCode()?.length ?: getNumberOfPassCodeDigits())
    }

    fun removePassCode() {
        preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        numberOfPasscodeDigits = (getPassCode()?.length ?: getNumberOfPassCodeDigits())
    }

    fun checkPassCodeIsValid(passcode: String): Boolean {
        val passCodeString = getPassCode()
        if (passCodeString.isNullOrEmpty()) return false
        return passcode == passCodeString
    }

    fun getNumberOfPassCodeDigits(): Int {
        val numberOfPassCodeDigits = contextProvider.getInt(R.integer.passcode_digits)
        return maxOf(numberOfPassCodeDigits, PassCodeActivity.PASSCODE_MIN_LENGTH)
    }

    fun setMigrationRequired(required: Boolean) =
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_MIGRATION_REQUIRED, required)

    fun setLastUnlockTimestamp() =
        preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, SystemClock.elapsedRealtime())

    fun getNumberOfAttempts() = preferencesProvider.getInt(PREFERENCE_LOCK_ATTEMPTS, 0)

    fun increaseNumberOfAttempts() {
        preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, getNumberOfAttempts().plus(1))
        preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP, SystemClock.elapsedRealtime())
    }

    fun resetNumberOfAttempts() = preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, 0)

    fun getTimeToUnlockLeft(): Long {
        val timeLocked = 1.5.pow(getNumberOfAttempts()).toLong().times(1000)
        val lastUnlockAttempt = preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP, 0)
        return max(0, (lastUnlockAttempt + timeLocked) - SystemClock.elapsedRealtime())
    }

    fun initUnlockTimer() {
        object : CountDownTimer(getTimeToUnlockLeft(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.HOURS.convert(millisUntilFinished.plus(1000), TimeUnit.MILLISECONDS)
                val minutes =
                    if (hours > 0) TimeUnit.MINUTES.convert(
                        TimeUnit.SECONDS.convert(
                            millisUntilFinished.plus(1000),
                            TimeUnit.MILLISECONDS
                        ) - hours.times(3600), TimeUnit.SECONDS
                    )
                    else TimeUnit.MINUTES.convert(millisUntilFinished.plus(1000), TimeUnit.MILLISECONDS)
                val seconds = TimeUnit.SECONDS.convert(millisUntilFinished.plus(1000), TimeUnit.MILLISECONDS).rem(60)
                val timeString =
                    if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
                _getTimeToUnlockLiveData.postValue(Event(timeString))
            }

            override fun onFinish() {
                _getFinishedTimeToUnlockLiveData.postValue(Event(true))
            }
        }.start()
    }

    private fun loadPinFromOldFormatIfPossible(): String? {
        var pinString = ""
        for (i in 1..4) {
            val pinChar = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE_D + i, null)
            pinChar?.let { pinString += pinChar }
        }
        return pinString.ifEmpty { null }
    }

    fun setBiometricsState(enabled: Boolean) {
        preferencesProvider.putBoolean(BiometricActivity.PREFERENCE_SET_BIOMETRIC, enabled)
    }

    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass
     * code.
     */
    private fun requestPassCodeConfirmation() {
        confirmingPassCode = true
        firstPasscode = passcodeString.toString()
        passcodeString = StringBuilder()
    }

    /**
     * Compares pass code retyped by the user in the input fields with the value entered just
     * before.
     *
     * @return     'True' if retyped pass code equals to the entered before.
     */
    private fun confirmPassCode(): Boolean {
        confirmingPassCode = false
        return firstPasscode == passcodeString.toString()
    }
}
