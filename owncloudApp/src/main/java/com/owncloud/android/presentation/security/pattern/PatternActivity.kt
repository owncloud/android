/**
 * ownCloud Android client application
 *
 * @author Shashvat Kedia
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.presentation.security.pattern

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.andrognito.patternlockview.PatternLockView.Dot
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider
import com.owncloud.android.databinding.ActivityPatternLockBinding
import com.owncloud.android.extensions.showBiometricDialog
import com.owncloud.android.presentation.documentsprovider.DocumentsProviderUtils.Companion.notifyDocumentsProviderRoots
import com.owncloud.android.presentation.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.security.biometric.BiometricStatus
import com.owncloud.android.presentation.security.biometric.BiometricViewModel
import com.owncloud.android.presentation.security.biometric.EnableBiometrics
import com.owncloud.android.presentation.settings.security.SettingsSecurityFragment.Companion.EXTRAS_LOCK_ENFORCED
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class PatternActivity : AppCompatActivity(), EnableBiometrics {

    // ViewModel
    private val patternViewModel by viewModel<PatternViewModel>()
    private val biometricViewModel by viewModel<BiometricViewModel>()

    private var _binding: ActivityPatternLockBinding? = null
    val binding get() = _binding!!

    private var confirmingPattern = false
    private var patternValue: String? = null
    private var newPatternValue: String? = null

    val resultIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        _binding = ActivityPatternLockBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.patternLockView.clearPattern()

        // Allow or disallow touches with other visible windows
        binding.activityPatternLockLayout.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        /**
         * patternExpShouldVisible holds the boolean value that signifies whether the explanationPattern should be
         * visible or not.
         * it is set to true when the pattern is set and when the pattern is removed.
         */
        var patternExpShouldVisible = false

        when (intent.action) {
            ACTION_CHECK -> {
                /**
                 * This block is executed when the user opens the app after setting the pattern lock
                 * this block takes the pattern input by the user and checks it with the pattern initially set by the user.
                 */
                binding.headerPattern.text = getString(R.string.pattern_enter_pattern)
                binding.explanationPattern.visibility = View.INVISIBLE
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
            ACTION_REQUEST_WITH_RESULT -> {
                /**
                 * This block is executed when the user is setting the pattern lock (i.e enabling the pattern lock)
                 */
                var headerPatternViewText = ""
                if (savedInstanceState != null) {
                    confirmingPattern = savedInstanceState.getBoolean(KEY_CONFIRMING_PATTERN)
                    patternValue = savedInstanceState.getString(KEY_PATTERN_STRING)
                    headerPatternViewText = savedInstanceState.getString(PATTERN_HEADER_VIEW_TEXT)!!
                    patternExpShouldVisible = savedInstanceState.getBoolean(PATTERN_EXP_VIEW_STATE)
                }
                if (confirmingPattern) {
                    binding.headerPattern.text = headerPatternViewText
                    if (!patternExpShouldVisible) {
                        binding.explanationPattern.visibility = View.INVISIBLE
                    }
                } else {
                    binding.headerPattern.text = getString(R.string.pattern_configure_pattern)
                    binding.explanationPattern.visibility = View.VISIBLE
                    if (intent.extras?.getBoolean(EXTRAS_LOCK_ENFORCED) == true) {
                        supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    } else {
                        supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    }
                }
            }
            ACTION_CHECK_WITH_RESULT -> {
                /**
                 * This block is executed when the user is removing the pattern lock (i.e disabling the pattern lock)
                 */
                binding.headerPattern.text = getString(R.string.pattern_remove_pattern)
                binding.explanationPattern.text = getString(R.string.pattern_no_longer_required)
                binding.explanationPattern.visibility = View.VISIBLE
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            else -> {
                throw IllegalArgumentException(R.string.illegal_argument_exception_message.toString() + " ")
            }
        }

        setPatternListener()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        PatternManager.onActivityStopped(this)
        super.onBackPressed()
    }

    /**
     * Binds the appropriate listener to the pattern view.
     */
    private fun setPatternListener() {
        binding.patternLockView.addPatternLockListener(object : PatternLockViewListener {
            override fun onStarted() {
                Timber.d("Pattern Drawing Started")
            }

            override fun onProgress(list: List<Dot>) {
                Timber.d("Pattern Progress %s", PatternLockUtils.patternToString(binding.patternLockView, list))
            }

            override fun onComplete(list: List<Dot>) {
                if (ACTION_REQUEST_WITH_RESULT == intent.action) {
                    /**
                     * This block gets executed when the pattern has to be set.
                     * count variable holds the number of time the pattern has been input.
                     * if the value of count is two then the pattern input first (which is stored in patternValue
                     * variable)
                     * is compared with the pattern value input the second time
                     * (which is stored in newPatternValue) if both the variables hold the same value
                     * then the pattern is set.
                     */
                    if (patternValue.isNullOrEmpty()) {
                        patternValue = PatternLockUtils.patternToString(binding.patternLockView, list)
                    } else {
                        newPatternValue = PatternLockUtils.patternToString(binding.patternLockView, list)
                    }
                } else {
                    patternValue = PatternLockUtils.patternToString(binding.patternLockView, list)
                }
                Timber.d("Pattern %s", PatternLockUtils.patternToString(binding.patternLockView, list))
                processPattern()
            }

            override fun onCleared() {
                Timber.d("Pattern has been cleared")
            }
        })
    }

    private fun processPattern() {
        when (intent.action) {
            ACTION_CHECK -> {
                /**
                 * This block is executed when the user opens the app after setting the pattern lock
                 * this block takes the pattern input by the user and checks it with the pattern initially set by the user.
                 */
                handleActionCheck()
            }
            ACTION_CHECK_WITH_RESULT -> {
                //This block is executed when the user is removing the pattern lock (i.e disabling the pattern lock)
                handleActionCheckWithResult()
            }
            ACTION_REQUEST_WITH_RESULT -> {
                //This block is executed when the user is setting the pattern lock (i.e enabling the pattern lock)
                handleActionRequestWithResult()
            }
        }
    }

    private fun handleActionCheck() {
        if (patternViewModel.checkPatternIsValid(patternValue)) {
            binding.errorPattern.visibility = View.INVISIBLE
            val preferencesProvider = OCSharedPreferencesProvider(applicationContext)
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, SystemClock.elapsedRealtime())
            PatternManager.onActivityStopped(this)
            finish()
        } else {
            showErrorAndRestart(
                errorMessage = R.string.pattern_incorrect_pattern,
                headerMessage = R.string.pattern_enter_pattern, explanationVisibility = View.INVISIBLE
            )
        }
    }

    private fun handleActionCheckWithResult() {
        if (patternViewModel.checkPatternIsValid(patternValue)) {
            patternViewModel.removePattern()
            val result = Intent()
            setResult(RESULT_OK, result)
            binding.errorPattern.visibility = View.INVISIBLE
            notifyDocumentsProviderRoots(applicationContext)
            finish()
        } else {
            showErrorAndRestart(
                errorMessage = R.string.pattern_incorrect_pattern,
                headerMessage = R.string.pattern_enter_pattern, explanationVisibility = View.INVISIBLE
            )
        }
    }

    private fun handleActionRequestWithResult() {
        if (!confirmingPattern) {
            binding.errorPattern.visibility = View.INVISIBLE
            requestPatternConfirmation()
        } else if (confirmPattern()) {
            savePatternAndExit()
        } else {
            showErrorAndRestart(
                errorMessage = R.string.pattern_not_same_pattern,
                headerMessage = R.string.pattern_enter_pattern, explanationVisibility = View.VISIBLE
            )
        }
    }

    private fun showErrorAndRestart(
        errorMessage: Int, headerMessage: Int,
        explanationVisibility: Int
    ) {
        patternValue = null
        binding.errorPattern.setText(errorMessage)
        binding.errorPattern.visibility = View.VISIBLE
        binding.headerPattern.setText(headerMessage)
        binding.explanationPattern.visibility = explanationVisibility
    }

    /**
     * Ask to the user to re-enter the pattern just entered before saving it as the current pattern.
     */
    private fun requestPatternConfirmation() {
        binding.patternLockView.clearPattern()
        binding.headerPattern.setText(R.string.pattern_reenter_pattern)
        binding.explanationPattern.visibility = View.INVISIBLE
        confirmingPattern = true
    }

    private fun confirmPattern(): Boolean {
        confirmingPattern = false
        return newPatternValue != null && newPatternValue == patternValue
    }

    private fun savePatternAndExit() {
        patternViewModel.setPattern(patternValue!!)
        setResult(RESULT_OK, resultIntent)
        notifyDocumentsProviderRoots(applicationContext)
        if (biometricViewModel.isBiometricLockAvailable()) {
            showBiometricDialog(this)
        } else {
            PatternManager.onActivityStopped(this)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean(KEY_CONFIRMING_PATTERN, confirmingPattern)
            putString(KEY_PATTERN_STRING, patternValue)
            putString(PATTERN_HEADER_VIEW_TEXT, binding.headerPattern.text.toString())
            putBoolean(PATTERN_EXP_VIEW_STATE, binding.explanationPattern.isVisible)
        }
    }

    /**
     * Overrides click on the BACK arrow to correctly cancel ACTION_ENABLE or ACTION_DISABLE, while
     * preventing than ACTION_CHECK may be worked around.
     *
     * @param keyCode       Key code of the key that triggered the down event.
     * @param event         Event triggered.
     * @return              'True' when the key event was processed by this method.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            if (ACTION_REQUEST_WITH_RESULT == intent.action &&
                intent.extras?.getBoolean(EXTRAS_LOCK_ENFORCED) != true ||
                ACTION_CHECK_WITH_RESULT == intent.action
            ) {
                finish()
            } // else, do nothing, but report that the key was consumed to stay alive
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionSelected(optionSelected: BiometricStatus) {
        when (optionSelected) {
            BiometricStatus.ENABLED_BY_USER -> {
                patternViewModel.setBiometricsState(true)
            }
            BiometricStatus.DISABLED_BY_USER -> {
                patternViewModel.setBiometricsState(false)
            }
        }
        PatternManager.onActivityStopped(this)
        finish()
    }

    companion object {
        const val ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT"
        const val ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT"
        const val ACTION_CHECK = "ACTION_CHECK_PATTERN"

        // NOTE: PREFERENCE_SET_PATTERN must have the same value as settings_security.xml-->android:key for pattern preference
        const val PREFERENCE_SET_PATTERN = "set_pattern"
        const val PREFERENCE_PATTERN = "KEY_PATTERN"

        private const val KEY_CONFIRMING_PATTERN = "CONFIRMING_PATTERN"
        private const val KEY_PATTERN_STRING = "PATTERN_STRING"
        private const val PATTERN_HEADER_VIEW_TEXT = "PATTERN_HEADER_VIEW_TEXT"
        private const val PATTERN_EXP_VIEW_STATE = "PATTERN_EXP_VIEW_STATE"
    }
}
