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

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.owncloud.android.R
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.ui.security.BiometricActivity
import com.owncloud.android.presentation.ui.security.BiometricManager
import com.owncloud.android.presentation.ui.security.LockTimeout
import com.owncloud.android.presentation.ui.security.PREFERENCE_LOCK_TIMEOUT
import com.owncloud.android.presentation.ui.security.PassCodeActivity
import com.owncloud.android.presentation.ui.security.PatternActivity
import com.owncloud.android.presentation.viewmodels.settings.SettingsSecurityViewModel
import com.owncloud.android.utils.DocumentProviderUtils.Companion.notifyDocumentProviderRoots
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsSecurityFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val securityViewModel by viewModel<SettingsSecurityViewModel>()

    private var screenSecurity: PreferenceScreen? = null
    private var prefPasscode: CheckBoxPreference? = null
    private var prefPattern: CheckBoxPreference? = null
    private var prefBiometric: CheckBoxPreference? = null
    private var prefLockApplication: ListPreference? = null
    private var prefAccessDocumentProvider: CheckBoxPreference? = null
    private var prefTouchesWithOtherVisibleWindows: CheckBoxPreference? = null

    private val enablePasscodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            else {
                prefPasscode?.isChecked = true
                result.data?.getBooleanExtra(BIOMETRIC_ENABLED_FROM_DIALOG_EXTRA, false).apply {
                    securityViewModel.setBiometricsState(this ?: false)
                    prefBiometric?.isChecked = this ?: false
                }

                // Allow to use biometric lock, lock delay and access from document provider since Passcode lock has been enabled
                enableBiometricAndLockApplicationAndAccessFromDocumentProvider()
            }
        }

    private val disablePasscodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            else {
                prefPasscode?.isChecked = false

                // Do not allow to use biometric lock, lock delay nor access from document provider since Passcode lock has been disabled
                disableBiometric()
                prefLockApplication?.isEnabled = false
                disableAccessFromDocumentProvider()
            }
        }

    private val enablePatternLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            else {
                prefPattern?.isChecked = true
                result.data?.getBooleanExtra(BIOMETRIC_ENABLED_FROM_DIALOG_EXTRA, false).apply {
                    securityViewModel.setBiometricsState(this ?: false)
                    prefBiometric?.isChecked = this ?: false
                }

                // Allow to use biometric lock, lock delay and access from document provider since Pattern lock has been enabled
                enableBiometricAndLockApplicationAndAccessFromDocumentProvider()
            }
        }

    private val disablePatternLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            else {
                prefPattern?.isChecked = false

                // Do not allow to use biometric lock, lock delay nor access from document provider since Pattern lock has been disabled
                disableBiometric()
                prefLockApplication?.isEnabled = false
                disableAccessFromDocumentProvider()
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_security, rootKey)

        screenSecurity = findPreference(SCREEN_SECURITY)
        prefPasscode = findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE)
        prefPattern = findPreference(PatternActivity.PREFERENCE_SET_PATTERN)
        prefBiometric = findPreference(BiometricActivity.PREFERENCE_SET_BIOMETRIC)
        prefLockApplication = findPreference<ListPreference>(PREFERENCE_LOCK_TIMEOUT)?.apply {
            entries = listOf(
                getString(R.string.prefs_lock_application_entries_immediately),
                getString(R.string.prefs_lock_application_entries_1minute),
                getString(R.string.prefs_lock_application_entries_5minutes),
                getString(R.string.prefs_lock_application_entries_30minutes)
            ).toTypedArray()
            entryValues = listOf(
                LockTimeout.IMMEDIATELY.name,
                LockTimeout.ONE_MINUTE.name,
                LockTimeout.FIVE_MINUTES.name,
                LockTimeout.THIRTY_MINUTES.name
            ).toTypedArray()
        }
        prefAccessDocumentProvider = findPreference(PREFERENCE_ACCESS_FROM_DOCUMENT_PROVIDER)
        prefTouchesWithOtherVisibleWindows = findPreference(PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS)

        prefPasscode?.isVisible = !securityViewModel.isSecurityEnforcedEnabled()
        prefPattern?.isVisible = !securityViewModel.isSecurityEnforcedEnabled()

        // Passcode lock
        prefPasscode?.setOnPreferenceChangeListener { _: Preference?, newValue: Any ->
            if (securityViewModel.isPatternSet()) {
                showMessageInSnackbar(getString(R.string.pattern_already_set))
            } else {
                val intent = Intent(activity, PassCodeActivity::class.java)
                if (newValue as Boolean) {
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
            if (securityViewModel.isPasscodeSet()) {
                showMessageInSnackbar(getString(R.string.passcode_already_set))
            } else {
                val intent = Intent(activity, PatternActivity::class.java)
                if (newValue as Boolean) {
                    intent.action = PatternActivity.ACTION_REQUEST_WITH_RESULT
                    enablePatternLauncher.launch(intent)
                } else {
                    intent.action = PatternActivity.ACTION_CHECK_WITH_RESULT
                    disablePatternLauncher.launch(intent)
                }
            }
            false
        }

        // Biometric lock
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            screenSecurity?.removePreference(prefBiometric)
        } else if (prefBiometric != null) {
            if (!BiometricManager.isHardwareDetected()) { // Biometric not supported
                screenSecurity?.removePreference(prefBiometric)
            } else {
                if (prefPasscode?.isChecked == false && prefPattern?.isChecked == false) { // Disable biometric lock if Passcode or Pattern locks are disabled
                    disableBiometric()
                }

                prefBiometric?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val incomingValue = newValue as Boolean

                    // No biometric enrolled yet
                    if (incomingValue && !BiometricManager.hasEnrolledBiometric()) {
                        showMessageInSnackbar(getString(R.string.biometric_not_enrolled))
                        return@setOnPreferenceChangeListener false
                    }
                    true
                }
            }
        }

        // Lock application and Access from document provider visibility
        if (prefPasscode?.isChecked == false && prefPattern?.isChecked == false) {
            prefLockApplication?.isEnabled = false
            disableAccessFromDocumentProvider()
        }

        // Access from document provider
        prefAccessDocumentProvider?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            if (newValue as Boolean) {
                activity?.let {
                    AlertDialog.Builder(it)
                        .setTitle(getString(R.string.confirmation_access_from_document_provider_title))
                        .setMessage(getString(R.string.confirmation_access_from_document_provider_message))
                        .setNegativeButton(getString(R.string.common_no), null)
                        .setPositiveButton(
                            getString(R.string.common_yes)
                        ) { dialog: DialogInterface?, which: Int ->
                            securityViewModel.setPrefAccessDocumentProvider(true)
                            prefAccessDocumentProvider?.isChecked = true
                            notifyDocumentProviderRoots(requireContext())
                        }
                        .show()
                }
                return@setOnPreferenceChangeListener false
            }
            notifyDocumentProviderRoots(requireContext())
            true
        }

        // Touches with other visible windows
        prefTouchesWithOtherVisibleWindows?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            if (newValue as Boolean) {
                activity?.let {
                    AlertDialog.Builder(it)
                        .setTitle(getString(R.string.confirmation_touches_with_other_windows_title))
                        .setMessage(getString(R.string.confirmation_touches_with_other_windows_message))
                        .setNegativeButton(getString(R.string.common_no), null)
                        .setPositiveButton(
                            getString(R.string.common_yes)
                        ) { dialog: DialogInterface?, which: Int ->
                            securityViewModel.setPrefTouchesWithOtherVisibleWindows(true)
                            prefTouchesWithOtherVisibleWindows?.isChecked = true
                        }
                        .show()
                }
                return@setOnPreferenceChangeListener false
            }
            true
        }
    }

    private fun enableBiometricAndLockApplicationAndAccessFromDocumentProvider() {
        prefBiometric?.apply {
            isEnabled = true
            summary = null
        }
        prefLockApplication?.isEnabled = true
        prefAccessDocumentProvider?.isEnabled = true
    }

    private fun disableBiometric() {
        prefBiometric?.apply {
            isChecked = false
            isEnabled = false
            summary = getString(R.string.prefs_biometric_summary)
        }
    }

    private fun disableAccessFromDocumentProvider() {
        prefAccessDocumentProvider?.apply {
            isChecked = false
            isEnabled = false
        }
    }

    companion object {
        private const val SCREEN_SECURITY = "security_screen"
        const val PREFERENCE_ACCESS_FROM_DOCUMENT_PROVIDER = "access_from_document_provider"
        const val PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS = "touches_with_other_visible_windows"
        const val EXTRAS_LOCK_ENFORCED = "EXTRAS_LOCK_ENFORCED"
        const val PREFERENCE_LOCK_ATTEMPTS = "PrefLockAttempts"
        const val BIOMETRIC_ENABLED_FROM_DIALOG_EXTRA = "BIOMETRIC_ENABLED_FROM_DIALOG_EXTRA"
    }

}
