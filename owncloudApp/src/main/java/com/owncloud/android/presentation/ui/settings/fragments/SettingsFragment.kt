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

package com.owncloud.android.presentation.ui.settings.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.owncloud.android.R
import com.owncloud.android.authentication.BiometricManager
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import com.owncloud.android.ui.activity.BiometricActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : PreferenceFragmentCompat() {

    private val settingsViewModel by viewModel<SettingsViewModel>()

    private var prefSecurityCategory: PreferenceCategory? = null
    private var prefPasscode: CheckBoxPreference? = null
    private var prefPattern: CheckBoxPreference? = null
    private var prefBiometric: CheckBoxPreference? = null
    private var biometricManager: BiometricManager? = null
    private var prefTouchesWithOtherVisibleWindows: CheckBoxPreference? = null

    private val enablePasscodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val passcodeEnableOk = settingsViewModel.handleEnablePasscode(result.data)
                if (passcodeEnableOk) {
                    prefPasscode?.isChecked = true
                    showMessageInSnackbar(getString(R.string.pass_code_stored))
                }
            }
        }
    private val disablePasscodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val passcodeDisableOk = settingsViewModel.handleDisablePasscode(result.data)
                if (passcodeDisableOk) {
                    prefPasscode?.isChecked = false
                    showMessageInSnackbar(getString(R.string.pass_code_removed))
                }
            }
        }
    private val enablePatternLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val patternEnableOk = settingsViewModel.handleEnablePattern(result.data)
                if (patternEnableOk) {
                    prefPattern?.isChecked = true
                    showMessageInSnackbar(getString(R.string.pattern_stored))
                }
            }
        }
    private val disablePatternLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val patternDisableOk = settingsViewModel.handleDisablePattern(result.data)
                if (patternDisableOk) {
                    prefPattern?.isChecked = false
                    showMessageInSnackbar(getString(R.string.pattern_removed))
                }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        manageSecuritySettings()

    }

    private fun manageSecuritySettings() {

        prefSecurityCategory = findPreference(PREFERENCE_SECURITY_CATEGORY)
        prefPasscode = findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE)
        prefPattern = findPreference(PatternLockActivity.PREFERENCE_SET_PATTERN)
        prefBiometric = findPreference(BiometricActivity.PREFERENCE_SET_BIOMETRIC)
        prefTouchesWithOtherVisibleWindows = findPreference(PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS)

        // Passcode lock
        prefPasscode?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            val intent = Intent(activity, PassCodeActivity::class.java)
            val incomingValue = newValue as Boolean
            val patternSet = settingsViewModel.isPatternSet()
            if (patternSet) {
                showMessageInSnackbar(getString(R.string.pattern_already_set))
            } else {
                if (incomingValue) {
                    intent.action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
                    enablePasscodeLauncher.launch(intent)
                } else {
                    intent.action = PassCodeActivity.ACTION_CHECK_WITH_RESULT
                    disablePasscodeLauncher.launch(intent)
                }
            }
            false
        }

        // Pattern lock
        prefPattern?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            val intent = Intent(activity, PatternLockActivity::class.java)
            val incomingValue = newValue as Boolean
            val passcodeSet = settingsViewModel.isPasscodeSet()
            if (passcodeSet) {
                showMessageInSnackbar(getString(R.string.passcode_already_set))
            } else {
                if (incomingValue) {
                    intent.action = PatternLockActivity.ACTION_REQUEST_WITH_RESULT
                    enablePatternLauncher.launch(intent)
                } else {
                    intent.action = PatternLockActivity.ACTION_CHECK_WITH_RESULT
                    disablePatternLauncher.launch(intent)
                }
            }
            false
        }

        /*
        // Biometric lock
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPrefSecurityCategory.removePreference(mBiometric)
        } else if (mBiometric != null) {
            // Disable biometric lock if Passcode or Pattern locks are disabled
            if (mPasscode != null && mPattern != null && !mPasscode.isChecked() && !mPattern.isChecked()) {
                mBiometric.setEnabled(false)
                mBiometric.setSummary(R.string.prefs_biometric_summary)
            }
            mBiometric.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                val incoming = newValue as Boolean

                // Biometric not supported
                if (incoming && mBiometricManager != null && !mBiometricManager.isHardwareDetected()) {
                    showSnackMessage(R.string.biometric_not_hardware_detected)
                    return@setOnPreferenceChangeListener false
                }

                // No biometric enrolled yet
                if (incoming && mBiometricManager != null && !mBiometricManager.hasEnrolledBiometric()) {
                    showSnackMessage(R.string.biometric_not_enrolled)
                    return@setOnPreferenceChangeListener false
                }
                true
            })
        }

        if (mPrefTouchesWithOtherVisibleWindows != null) {
            mPrefTouchesWithOtherVisibleWindows.setOnPreferenceChangeListener(
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    if (newValue as Boolean) {
                        AlertDialog.Builder(this)
                            .setTitle(getString(R.string.confirmation_touches_with_other_windows_title))
                            .setMessage(getString(R.string.confirmation_touches_with_other_windows_message))
                            .setNegativeButton(getString(R.string.common_no), null)
                            .setPositiveButton(
                                getString(R.string.common_yes)
                            ) { dialog: DialogInterface?, which: Int ->
                                mPreferencesProvider.putBoolean(
                                    Preferences.PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS,
                                    true
                                )
                                mPrefTouchesWithOtherVisibleWindows.setChecked(true)
                            }
                            .show()
                        return@setOnPreferenceChangeListener false
                    }
                    true
                }
            )
        }

        */
    }

    /*
    override fun onResume() {
        super.onResume()
        val passCodeState: Boolean =
            preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        prefPasscode?.isChecked = passCodeState
        val patternState: Boolean =
            preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        prefPattern?.isChecked = patternState
        var biometricState: Boolean = preferencesProvider.getBoolean(
            BiometricActivity.PREFERENCE_SET_BIOMETRIC,
            false
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && biometricManager != null &&
            !biometricManager!!.hasEnrolledBiometric()
        ) {
            biometricState = false
        }
        prefBiometric?.isChecked = biometricState
    }
    */

    companion object {
        private const val PREFERENCE_SECURITY_CATEGORY = "security_category"
        const val PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS = "touches_with_other_visible_windows"
    }

}
