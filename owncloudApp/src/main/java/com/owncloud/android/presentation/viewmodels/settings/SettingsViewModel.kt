package com.owncloud.android.presentation.viewmodels.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity

class SettingsViewModel (
    private val preferencesProvider: SharedPreferencesProvider
) : ViewModel() {

    fun isPatternSet() : Boolean {
        return preferencesProvider.getBoolean(
            PatternLockActivity.PREFERENCE_SET_PATTERN,
            false
        )
    }

    fun handleEnablePasscode(data: Intent?): Boolean {
        val passcode = data?.getStringExtra(PassCodeActivity.KEY_PASSCODE)
        if (passcode != null && passcode.length == 4) {
            for (i in 1..4) {
                preferencesProvider.putString(
                    PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                    passcode.substring(i - 1, i)
                )
            }
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)

            // Allow to use biometric lock since Passcode lock has been enabled
            //enableBiometric()
            return true
        }
        return false
    }

    fun handleDisablePasscode(data: Intent?): Boolean {
        if (data?.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false) == true) {
            preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)

            // Do not allow to use biometric lock since Passcode lock has been disabled
            //disableBiometric(getString(R.string.prefs_biometric_summary))
            return true
        }
        return false
    }

    fun isPasscodeSet() : Boolean {
        return preferencesProvider.getBoolean(
            PassCodeActivity.PREFERENCE_SET_PASSCODE,
            false
        )
    }

    fun handleEnablePattern(data: Intent?): Boolean {
        val pattern = data?.getStringExtra(PatternLockActivity.KEY_PATTERN)
        if (pattern != null) {
            preferencesProvider.putString(PatternLockActivity.KEY_PATTERN, pattern)
            preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true)

            // Allow to use biometric lock since Pattern lock has been enabled
            //enableBiometric()
            return true
        }
        return false
    }

    fun handleDisablePattern(data: Intent?): Boolean {
        if (data?.getBooleanExtra(PatternLockActivity.KEY_CHECK_RESULT, false) == true) {
            preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)

            // Do not allow to use biometric lock since Pattern lock has been disabled
            //disableBiometric(getString(R.string.prefs_biometric_summary))
            return true
        }
        return false
    }

}