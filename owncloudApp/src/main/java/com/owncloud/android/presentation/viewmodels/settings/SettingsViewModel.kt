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

import android.app.Activity
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.settings.fragments.SettingsFragment
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity

class SettingsViewModel(
    private val preferencesProvider: SharedPreferencesProvider
) : ViewModel() {

    fun isPatternSet() = preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)

    fun handleEnablePasscode(result: ActivityResult?): UIResult<Unit> {
        if (result?.resultCode != Activity.RESULT_OK) return UIResult.Error()
        val passcode = result.data?.getStringExtra(PassCodeActivity.KEY_PASSCODE).takeIf { it?.length == 4 }
            ?: return UIResult.Error()
        for (i in 1..4) {
            preferencesProvider.putString(
                PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                passcode.substring(i - 1, i)
            )
        }
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true)
        return UIResult.Success()
    }

    fun handleDisablePasscode(result: ActivityResult?): UIResult<Unit> {
        if (result?.resultCode != Activity.RESULT_OK || result.data?.getBooleanExtra(
                PassCodeActivity.KEY_CHECK_RESULT,
                false
            ) == false
        ) return UIResult.Error()
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        return UIResult.Success()
    }

    fun isPasscodeSet() = preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)

    fun handleEnablePattern(result: ActivityResult?): UIResult<Unit> {
        if (result?.resultCode != Activity.RESULT_OK) return UIResult.Error()
        val pattern = result.data?.getStringExtra(PatternLockActivity.KEY_PATTERN)
        pattern?.let {
            preferencesProvider.putString(PatternLockActivity.KEY_PATTERN, it)
            preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true)
            return UIResult.Success()
        }
        return UIResult.Error()
    }

    fun handleDisablePattern(result: ActivityResult?): UIResult<Unit> {
        if (result?.resultCode != Activity.RESULT_OK || result.data?.getBooleanExtra(
                PatternLockActivity.KEY_CHECK_RESULT,
                false
            ) == false
        ) return UIResult.Error()
        preferencesProvider.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        return UIResult.Success()
    }

    fun setPrefTouchesWithOtherVisibleWindows(value: Boolean) =
        preferencesProvider.putBoolean(SettingsFragment.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, value)

}
