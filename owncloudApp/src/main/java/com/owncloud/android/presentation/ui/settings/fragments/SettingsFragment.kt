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
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.BiometricManager
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.sendEmail
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import com.owncloud.android.ui.activity.BiometricActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.ui.activity.PrivacyPolicyActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val settingsViewModel by viewModel<SettingsViewModel>()

    // Security section
    private var prefSecurityCategory: PreferenceCategory? = null
    private var prefPasscode: CheckBoxPreference? = null
    private var prefPattern: CheckBoxPreference? = null
    private var prefBiometric: CheckBoxPreference? = null
    private var biometricManager: BiometricManager? = null
    private var prefTouchesWithOtherVisibleWindows: CheckBoxPreference? = null

    // More section
    private var prefMoreCategory: PreferenceCategory? = null
    private var prefHelp: Preference? = null
    private var prefSync: Preference? = null
    private var prefRecommend: Preference? = null
    private var prefFeedback: Preference? = null
    private var prefPrivacyPolicy: Preference? = null
    private var prefImprint: Preference? = null
    private var prefAboutApp: Preference? = null

    // Launchers for security section
    private val enablePasscodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val passcodeEnableResult = settingsViewModel.handleEnablePasscode(result.data)
            if (passcodeEnableResult.isSuccess) {
                prefPasscode?.isChecked = true

                // Allow to use biometric lock since Passcode lock has been enabled
                enableBiometric()
            } else {
                showMessageInSnackbar(getString(R.string.pass_code_error_set))
            }
        }
    private val disablePasscodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val passcodeDisableResult = settingsViewModel.handleDisablePasscode(result.data)
            if (passcodeDisableResult.isSuccess) {
                prefPasscode?.isChecked = false

                // Do not allow to use biometric lock since Passcode lock has been disabled
                disableBiometric(getString(R.string.prefs_biometric_summary))
            } else {
                showMessageInSnackbar(getString(R.string.pass_code_error_remove))
            }
        }
    private val enablePatternLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val patternEnableResult = settingsViewModel.handleEnablePattern(result.data)
            if (patternEnableResult.isSuccess) {
                prefPattern?.isChecked = true

                // Allow to use biometric lock since Pattern lock has been enabled
                enableBiometric()
            } else {
                showMessageInSnackbar(getString(R.string.pattern_error_set))
            }
        }
    private val disablePatternLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val patternDisableResult = settingsViewModel.handleDisablePattern(result.data)
            if (patternDisableResult.isSuccess) {
                prefPattern?.isChecked = false

                // Do not allow to use biometric lock since Pattern lock has been disabled
                disableBiometric(getString(R.string.prefs_biometric_summary))
            } else {
                showMessageInSnackbar(getString(R.string.pattern_error_remove))
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        manageSecuritySettings()
        manageMoreSettings()

    }

    private fun manageSecuritySettings() {

        prefSecurityCategory = findPreference(PREFERENCE_SECURITY_CATEGORY)
        prefPasscode = findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE)
        prefPattern = findPreference(PatternLockActivity.PREFERENCE_SET_PATTERN)
        prefBiometric = findPreference(BiometricActivity.PREFERENCE_SET_BIOMETRIC)
        prefTouchesWithOtherVisibleWindows = findPreference(PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS)

        // Passcode lock
        prefPasscode?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            if (settingsViewModel.isPatternSet()) {
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
            if (settingsViewModel.isPasscodeSet()) {
                showMessageInSnackbar(getString(R.string.passcode_already_set))
            } else {
                val intent = Intent(activity, PatternLockActivity::class.java)
                if (newValue as Boolean) {
                    intent.action = PatternLockActivity.ACTION_REQUEST_WITH_RESULT
                    enablePatternLauncher.launch(intent)
                } else {
                    intent.action = PatternLockActivity.ACTION_CHECK_WITH_RESULT
                    disablePatternLauncher.launch(intent)
                }
            }
            false
        }

        // Biometric lock
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            prefSecurityCategory?.removePreference(prefBiometric)
        } else if (prefBiometric != null) {
            biometricManager = BiometricManager.getBiometricManager(activity)

            // Disable biometric lock if Passcode or Pattern locks are disabled
            if (prefPasscode?.isChecked == false && prefPattern?.isChecked == false) {
                disableBiometric(getString(R.string.prefs_biometric_summary))
            }
            prefBiometric?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                val incomingValue = newValue as Boolean

                // Biometric not supported
                if (incomingValue && biometricManager?.isHardwareDetected == false) {
                    showMessageInSnackbar(getString(R.string.biometric_not_hardware_detected))
                    return@setOnPreferenceChangeListener false
                }

                // No biometric enrolled yet
                if (incomingValue && biometricManager?.hasEnrolledBiometric() == false) {
                    showMessageInSnackbar(getString(R.string.biometric_not_enrolled))
                    return@setOnPreferenceChangeListener false
                }
                true
            }
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
                            settingsViewModel.setPrefTouchesWithOtherVisibleWindows(true)
                            prefTouchesWithOtherVisibleWindows?.isChecked = true
                        }
                        .show()
                }
                return@setOnPreferenceChangeListener false
            }
            true
        }

    }

    private fun manageMoreSettings() {

        prefMoreCategory = findPreference(PREFERENCE_MORE_CATEGORY)
        prefHelp = findPreference(PREFERENCE_HELP)
        prefSync = findPreference(PREFERENCE_SYNC_CALENDAR_CONTACTS)
        prefRecommend = findPreference(PREFERENCE_RECOMMEND)
        prefFeedback = findPreference(PREFERENCE_FEEDBACK)
        prefPrivacyPolicy = findPreference(PREFERENCE_PRIVACY_POLICY)
        prefImprint = findPreference(PREFERENCE_IMPRINT)
        prefAboutApp = findPreference(PREFERENCE_ABOUT_APP)

        // Help
        if (settingsViewModel.isHelpEnabled()) {
            prefHelp?.setOnPreferenceClickListener {
                val helpUrl = settingsViewModel.getHelpUrl()
                requireActivity().goToUrl(helpUrl)
                true
            }
        } else {
            prefMoreCategory?.removePreference(prefHelp)
        }

        // Sync contacts, calendars and tasks
        if (settingsViewModel.isSyncEnabled()) {
            prefSync?.setOnPreferenceClickListener {
                val syncUrl = settingsViewModel.getSyncUrl()
                requireActivity().goToUrl(syncUrl)
                true
            }
        } else {
            prefMoreCategory?.removePreference(prefSync)
        }

        // Recommend
        if (settingsViewModel.isRecommendEnabled()) {
            prefRecommend?.setOnPreferenceClickListener {
                val appName = getString(R.string.app_name)
                val downloadUrl = getString(R.string.url_app_download)

                val recommendEmail = getString(R.string.mail_recommend)
                val recommendSubject = String.format(getString(R.string.recommend_subject), appName)
                val recommendText = String.format(getString(R.string.recommend_text), appName, downloadUrl)

                requireActivity().sendEmail(email = recommendEmail, subject = recommendSubject, text = recommendText)
                true
            }
        } else {
            prefMoreCategory?.removePreference(prefRecommend)
        }

        // Feedback
        if (settingsViewModel.isFeedbackEnabled()) {
            prefFeedback?.setOnPreferenceClickListener {
                val feedbackMail = getString(R.string.mail_feedback)
                val feedback = "Android v" + BuildConfig.VERSION_NAME + " - " + getString(R.string.prefs_feedback)

                requireActivity().sendEmail(email = feedbackMail, subject = feedback)
                true
            }
        } else {
            prefMoreCategory?.removePreference(prefFeedback)
        }

        // Privacy policy
        if (settingsViewModel.isPrivacyPolicyEnabled()) {
            prefPrivacyPolicy?.setOnPreferenceClickListener {
                val intent = Intent(context, PrivacyPolicyActivity::class.java)
                startActivity(intent)
                true
            }
        } else {
            prefMoreCategory?.removePreference(prefPrivacyPolicy)
        }

        // Imprint
        if (settingsViewModel.isImprintEnabled()) {
            prefImprint?.setOnPreferenceClickListener {
                val imprintUrl = settingsViewModel.getImprintUrl()
                requireActivity().goToUrl(imprintUrl)
                true
            }
        } else {
            prefMoreCategory?.removePreference(prefImprint)
        }

        // About app
        prefAboutApp?.let {
            it.title = String.format(getString(R.string.about_android), getString(R.string.app_name))
            val appVersion = "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE} ${BuildConfig.COMMIT_SHA1}"
            it.summary = String.format(getString(R.string.about_version), appVersion)
            it.setOnPreferenceClickListener {
                // Enable logs with 5 taps, to be done when logs section is approached
                true
            }
        }
    }

    private fun enableBiometric() {
        prefBiometric?.isEnabled = true
        prefBiometric?.summary = null
    }

    private fun disableBiometric(summary: String) {
        prefBiometric?.isChecked = false
        prefBiometric?.isEnabled = false
        prefBiometric?.summary = summary
    }

    companion object {
        private const val PREFERENCE_SECURITY_CATEGORY = "security_category"
        private const val PREFERENCE_MORE_CATEGORY = "more_category"

        const val PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS = "touches_with_other_visible_windows"
        private const val PREFERENCE_HELP = "help"
        private const val PREFERENCE_SYNC_CALENDAR_CONTACTS = "syncCalendarContacts"
        private const val PREFERENCE_RECOMMEND = "recommend"
        private const val PREFERENCE_FEEDBACK = "feedback"
        private const val PREFERENCE_PRIVACY_POLICY = "privacyPolicy"
        private const val PREFERENCE_IMPRINT = "imprint"
        private const val PREFERENCE_ABOUT_APP = "about_app"
    }

}
