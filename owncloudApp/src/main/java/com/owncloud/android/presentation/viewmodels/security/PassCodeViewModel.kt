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

import android.os.CountDownTimer
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.security.PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP
import com.owncloud.android.presentation.ui.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment.Companion.PREFERENCE_LOCK_ATTEMPTS
import com.owncloud.android.providers.ContextProvider
import kotlin.math.max
import kotlin.math.pow

class PassCodeViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider
) : ViewModel() {

    private val _getTimeToUnlockLiveData = MutableLiveData<Event<UIResult<String>>>()
    val getTimeToUnlockLiveData: LiveData<Event<UIResult<String>>>
        get() = _getTimeToUnlockLiveData

    private val _getFinishedTimeToUnlockLiveData = MutableLiveData<Event<UIResult<Boolean>>>()
    val getFinishedTimeToUnlockLiveData: LiveData<Event<UIResult<Boolean>>>
        get() = _getFinishedTimeToUnlockLiveData

    fun getPassCode() = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, loadPinFromOldFormatIfPossible())

    fun setPassCode(passcode: String) {
        preferencesProvider.putString(PassCodeActivity.PREFERENCE_PASSCODE, passcode)
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
    }

    fun removePassCode() {
        preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
    }

    fun checkPassCodeIsValid(passCodeDigits: Array<String?>): Boolean {
        val passCodeString = getPassCode()
        if (passCodeString.isNullOrEmpty()) return false
        var isValid = true
        var i = 0
        while (i < passCodeDigits.size && isValid) {
            val originalDigit = passCodeString[i].toString()
            isValid = passCodeDigits[i] != null && passCodeDigits[i] == originalDigit
            i++
        }
        return isValid
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

    fun resetNumberOfAttempts() =
        preferencesProvider.putInt(PREFERENCE_LOCK_ATTEMPTS, 0)

    fun getTimeToUnlockLeft(): Long {
        val timeLocked = 1.5.pow(getNumberOfAttempts()).toLong().times(1000)
        val lastUnlockAttempt = preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP, 0)
        return max(0, (lastUnlockAttempt + timeLocked) - SystemClock.elapsedRealtime())
    }

    fun initUnlockTimer() {
        object : CountDownTimer(getTimeToUnlockLeft(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished.plus(1000).div(1000).div(60)
                val seconds = millisUntilFinished.plus(1000).div(1000).rem(60)
                val timeString = String.format("%02d:%02d", minutes, seconds)
                _getTimeToUnlockLiveData.postValue(Event(UIResult.Success(timeString)))
            }

            override fun onFinish() {
                _getFinishedTimeToUnlockLiveData.postValue(Event(UIResult.Success(true)))
            }
        }.start()
    }

    private fun loadPinFromOldFormatIfPossible(): String? {
        var pinString = ""
        for (i in 1..4) {
            val pinChar = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE_D + i, null)
            pinChar?.let { pinString += pinChar }
        }
        return if (pinString.isEmpty()) null else pinString
    }
}
