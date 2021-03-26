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

package com.owncloud.android.presentation.viewmodels.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.settings.fragments.SettingsSecurityFragment
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity

class SettingsSecurityViewModel(
    private val preferencesProvider: SharedPreferencesProvider
) : ViewModel() {

    fun isPatternSet() = preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)

    fun handleEnablePasscode(data: Intent?): UIResult<Unit> {
        val passcode =
            data?.getStringExtra(PassCodeActivity.KEY_PASSCODE).takeIf { it?.length == 4 } ?: return UIResult.Error()
        for (i in 1..4) {
            preferencesProvider.putString(
                PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                passcode.substring(i - 1, i)
            )
        }
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        return UIResult.Success()
    }

    fun handleDisablePasscode(data: Intent?): UIResult<Unit> {
        data?.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false).takeIf { it == true } ?: return UIResult.Error()
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        return UIResult.Success()
    }

    fun isPasscodeSet() = preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)

    fun handleEnablePattern(data: Intent?): UIResult<Unit> {
        val pattern = data?.getStringExtra(PatternLockActivity.KEY_PATTERN) ?: return UIResult.Error()
        preferencesProvider.putString(PatternLockActivity.KEY_PATTERN, pattern)
        preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true)
        return UIResult.Success()
    }

    fun handleDisablePattern(data: Intent?): UIResult<Unit> {
        data?.getBooleanExtra(PatternLockActivity.KEY_CHECK_RESULT, false).takeIf { it == true }
            ?: return UIResult.Error()
        preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        return UIResult.Success()
    }

    fun setPrefTouchesWithOtherVisibleWindows(value: Boolean) =
        preferencesProvider.putBoolean(SettingsSecurityFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, value)
}
