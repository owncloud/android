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

package com.owncloud.android.presentation.ui.security

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.PatternLockView.Dot
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.presentation.viewmodels.security.PatternViewModel
import com.owncloud.android.utils.DocumentProviderUtils.Companion.notifyDocumentProviderRoots
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.lang.IllegalArgumentException

class PatternActivity : AppCompatActivity() {

    // ViewModel
    private val patternViewModel by viewModel<PatternViewModel>()

    private var confirmingPattern = false
    private var patternValue: String? = null
    private var newPatternValue: String? = null

    private lateinit var bCancel: Button
    private lateinit var patternHeader: TextView
    private lateinit var patternExplanation: TextView
    private lateinit var patternLockView: PatternLockView
    private lateinit var patternError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        setContentView(R.layout.activity_pattern_lock)

        val activityPatternLockLayout = findViewById<LinearLayout>(R.id.activityPatternLockLayout)
        bCancel = findViewById(R.id.cancel_pattern)
        patternHeader = findViewById(R.id.header_pattern)
        patternExplanation = findViewById(R.id.explanation_pattern)
        patternLockView = findViewById(R.id.pattern_lock_view)
        patternLockView.clearPattern()
        patternError = findViewById(R.id.error_pattern)

        // Allow or disallow touches with other visible windows
        activityPatternLockLayout.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        /**
         * patternExpShouldVisible holds the boolean value that signifies whether the patternExplanation should be
         * visible or not.
         * it is set to true when the pattern is set and when the pattern is removed.
         */
        var patternExpShouldVisible = false

        if (ACTION_CHECK == intent.action) {
            /**
             * This block is executed when the user opens the app after setting the pattern lock
             * this block takes the pattern input by the user and checks it with the pattern initially set by the user.
             */
            patternHeader.text = getString(R.string.pattern_enter_pattern)
            patternExplanation.visibility = View.INVISIBLE
            setCancelButtonEnabled(false)
        } else if (ACTION_REQUEST_WITH_RESULT == intent.action) {
            /**
             * This block is executed when the user is setting the pattern lock (i.e enabling the pattern lock)
             */
            var patternHeaderViewText = ""
            if (savedInstanceState != null) {
                confirmingPattern = savedInstanceState.getBoolean(KEY_CONFIRMING_PATTERN)
                patternValue = savedInstanceState.getString(KEY_PATTERN_STRING)
                patternHeaderViewText = savedInstanceState.getString(PATTERN_HEADER_VIEW_TEXT)!!
                patternExpShouldVisible = savedInstanceState.getBoolean(PATTERN_EXP_VIEW_STATE)
            }
            if (confirmingPattern) {
                patternHeader.text = patternHeaderViewText
                if (!patternExpShouldVisible) {
                    patternExplanation.visibility = View.INVISIBLE
                }
            } else {
                patternHeader.text = getString(R.string.pattern_configure_pattern)
                patternExplanation.visibility = View.VISIBLE
                setCancelButtonEnabled(true)
            }
        } else if (ACTION_CHECK_WITH_RESULT == intent.action) {
            /**
             * This block is executed when the user is removing the pattern lock (i.e disabling the pattern lock)
             */
            patternHeader.text = getString(R.string.pattern_remove_pattern)
            patternExplanation.text = getString(R.string.pattern_no_longer_required)
            patternExplanation.visibility = View.VISIBLE
            setCancelButtonEnabled(true)
        } else {
            throw IllegalArgumentException(R.string.illegal_argument_exception_message.toString() + " ")
        }

        setPatternListener()
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled 'True' makes the cancel button available, 'false' hides it.
     */
    protected fun setCancelButtonEnabled(enabled: Boolean) {
        if (enabled) {
            bCancel.visibility = View.VISIBLE
            bCancel.setOnClickListener { finish() }
        } else {
            bCancel.visibility = View.INVISIBLE
            bCancel.setOnClickListener(null)
        }
    }

    /**
     * Binds the appropriate listener to the pattern view.
     */
    protected fun setPatternListener() {
        patternLockView.addPatternLockListener(object : PatternLockViewListener {
            override fun onStarted() {
                Timber.d("Pattern Drawing Started")
            }

            override fun onProgress(list: List<Dot>) {
                Timber.d("Pattern Progress %s", PatternLockUtils.patternToString(patternLockView, list))
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
                        patternValue = PatternLockUtils.patternToString(patternLockView, list)
                    } else {
                        newPatternValue = PatternLockUtils.patternToString(patternLockView, list)
                    }
                } else {
                    patternValue = PatternLockUtils.patternToString(patternLockView, list)
                }
                Timber.d("Pattern %s", PatternLockUtils.patternToString(patternLockView, list))
                processPattern()
            }

            override fun onCleared() {
                Timber.d("Pattern has been cleared")
            }
        })
    }

    private fun processPattern() {
        if (ACTION_CHECK == intent.action) {
            /**
             * This block is executed when the user opens the app after setting the pattern lock
             * this block takes the pattern input by the user and checks it with the pattern initially set by the user.
             */
            if (patternViewModel.checkPatternIsValid(patternValue)) {
                patternError.visibility = View.INVISIBLE
                val preferencesProvider = SharedPreferencesProviderImpl(applicationContext)
                preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, System.currentTimeMillis())
                finish()
            } else {
                showErrorAndRestart(
                    R.string.pattern_incorrect_pattern,
                    R.string.pattern_enter_pattern, View.INVISIBLE
                )
            }
        } else if (ACTION_CHECK_WITH_RESULT == intent.action) {
            //This block is executed when the user is removing the pattern lock (i.e disabling the pattern lock)
            if (patternViewModel.checkPatternIsValid(patternValue)) {
                patternViewModel.removePattern()
                val result = Intent()
                setResult(RESULT_OK, result)
                patternError.visibility = View.INVISIBLE
                notifyDocumentProviderRoots(applicationContext)
                finish()
            } else {
                showErrorAndRestart(
                    R.string.pattern_incorrect_pattern,
                    R.string.pattern_enter_pattern, View.INVISIBLE
                )
            }
        } else if (ACTION_REQUEST_WITH_RESULT == intent.action) {
            //This block is executed when the user is setting the pattern lock (i.e enabling the pattern lock)
            if (!confirmingPattern) {
                patternError.visibility = View.INVISIBLE
                requestPatternConfirmation()
            } else if (confirmPattern()) {
                savePatternAndExit()
            } else {
                showErrorAndRestart(
                    R.string.pattern_not_same_pattern,
                    R.string.pattern_enter_pattern, View.VISIBLE
                )
            }
        }
    }

    private fun showErrorAndRestart(
        errorMessage: Int, headerMessage: Int,
        explanationVisibility: Int
    ) {
        patternValue = null
        patternError.setText(errorMessage)
        patternError.visibility = View.VISIBLE
        patternHeader.setText(headerMessage)
        patternExplanation.visibility = explanationVisibility
    }

    /**
     * Ask to the user to re-enter the pattern just entered before saving it as the current pattern.
     */
    protected fun requestPatternConfirmation() {
        patternLockView.clearPattern()
        patternHeader.setText(R.string.pattern_reenter_pattern)
        patternExplanation.visibility = View.INVISIBLE
        confirmingPattern = true
    }

    protected fun confirmPattern(): Boolean {
        confirmingPattern = false
        return newPatternValue != null && newPatternValue == patternValue
    }

    private fun savePatternAndExit() {
        val result = Intent()
        patternViewModel.setPattern(patternValue!!)
        setResult(RESULT_OK, result)
        notifyDocumentProviderRoots(applicationContext)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean(KEY_CONFIRMING_PATTERN, confirmingPattern)
            putString(KEY_PATTERN_STRING, patternValue)
            putString(PATTERN_HEADER_VIEW_TEXT, patternHeader.text.toString())
            if (patternExplanation.visibility == View.VISIBLE) {
                putBoolean(PATTERN_EXP_VIEW_STATE, true)
            } else {
                putBoolean(PATTERN_EXP_VIEW_STATE, false)
            }
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
            if (ACTION_REQUEST_WITH_RESULT == intent.action || ACTION_CHECK_WITH_RESULT == intent.action) {
                finish()
            } // else, do nothing, but report that the key was consumed to stay alive
            return true
        }
        return super.onKeyDown(keyCode, event)
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
