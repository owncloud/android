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

import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.presentation.ui.security.BiometricActivity
import com.owncloud.android.presentation.ui.security.BiometricManager
import com.owncloud.android.presentation.ui.security.PatternActivity
import com.owncloud.android.providers.ContextProvider

class PatternViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider,
) : ViewModel() {

    fun setPattern(pattern: String) {
        preferencesProvider.putString(PatternActivity.PREFERENCE_PATTERN, pattern)
        preferencesProvider.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, true)
    }

    fun removePattern() {
        preferencesProvider.removePreference(PatternActivity.PREFERENCE_PATTERN)
        preferencesProvider.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)
    }

    fun checkPatternIsValid(patternValue: String?): Boolean {
        val savedPattern = preferencesProvider.getString(PatternActivity.PREFERENCE_PATTERN, null)
        return savedPattern != null && savedPattern == patternValue
    }

    fun isBiometricLockAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            false
        } else if (!BiometricManager.isHardwareDetected()) { // Biometric not supported
            false
        } else BiometricManager.hasEnrolledBiometric() // Biometric not enrolled
    }

    private fun getPreferenceManagerEditor(): SharedPreferences.Editor {
        val appPrefsEditor = PreferenceManager.getDefaultSharedPreferences(contextProvider.getContext())
        return appPrefsEditor.edit()
    }

    fun setBiometricsState(enabled: Boolean) {
        val editor = getPreferenceManagerEditor()
        editor.putBoolean(BiometricActivity.PREFERENCE_SET_BIOMETRIC, enabled)
        editor.apply()
    }
}
