/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * Copyright (C) 2011 Bartek Przybylski
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

import android.content.Context
import com.owncloud.android.utils.DocumentProviderUtils.Companion.notifyDocumentProviderRoots
import android.widget.TextView
import android.widget.EditText
import android.os.Bundle
import android.view.WindowManager
import com.owncloud.android.R
import android.widget.LinearLayout
import android.view.View.OnFocusChangeListener
import android.content.Intent
import android.text.TextWatcher
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import com.owncloud.android.BuildConfig
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.presentation.viewmodels.security.PassCodeViewModel
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.util.Arrays

class PassCodeActivity : BaseActivity() {

    // ViewModel
    private val passCodeViewModel by viewModel<PassCodeViewModel>()

    private lateinit var bCancel: Button
    private lateinit var passCodeHdr: TextView
    private lateinit var passCodeHdrExplanation: TextView
    private lateinit var passCodeError: TextView
    private val passCodeEditTexts = arrayOfNulls<EditText>(numberOfPassInputs)
    private var passCodeDigits: Array<String?> = arrayOfNulls(numberOfPassInputs)
    private var confirmingPassCode = false
    private var bChange = true // to control that only one blocks jump

    /**
     * Initializes the activity.
     *
     * @param savedInstanceState    Previously saved state - irrelevant in this case
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /// protection against screen recording
        if (!BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } // else, let it go, or taking screenshots & testing will not be possible

        setContentView(R.layout.passcodelock)
        val passcodeLockLayout = findViewById<LinearLayout>(R.id.passcodeLockLayout)
        bCancel = findViewById(R.id.cancel)
        passCodeHdr = findViewById(R.id.header)
        passCodeHdrExplanation = findViewById(R.id.explanation)
        passCodeError = findViewById(R.id.error)

        // Allow or disallow touches with other visible windows
        passcodeLockLayout.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        passCodeHdrExplanation.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        inflatePasscodeTxtLine()

        if (ACTION_CHECK == intent.action) {
            /// this is a pass code request; the user has to input the right value
            passCodeHdr.text = getString(R.string.pass_code_enter_pass_code)
            passCodeHdrExplanation.visibility = View.INVISIBLE
            setCancelButtonEnabled(false) // no option to cancel
        } else if (ACTION_REQUEST_WITH_RESULT == intent.action) {
            if (savedInstanceState != null) {
                confirmingPassCode = savedInstanceState.getBoolean(KEY_CONFIRMING_PASSCODE)
                passCodeDigits = savedInstanceState.getStringArray(KEY_PASSCODE_DIGITS)!!
            }
            if (confirmingPassCode) {
                //the app was in the passcodeconfirmation
                requestPassCodeConfirmation()
            } else {
                /// pass code preference has just been activated in Preferences;
                // will receive and confirm pass code value
                passCodeHdr.text = getString(R.string.pass_code_configure_your_pass_code)
                //mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
                // TODO choose a header, check iOS
                passCodeHdrExplanation.visibility = View.VISIBLE
                setCancelButtonEnabled(true)
            }
        } else if (ACTION_CHECK_WITH_RESULT == intent.action) {
            /// pass code preference has just been disabled in Preferences;
            // will confirm user knows pass code, then remove it
            passCodeHdr.text = getString(R.string.pass_code_remove_your_pass_code)
            passCodeHdrExplanation.visibility = View.INVISIBLE
            setCancelButtonEnabled(true)
        } else {
            throw IllegalArgumentException(R.string.illegal_argument_exception_message.toString() + " ")
        }

        setTextListeners()
    }

    private fun inflatePasscodeTxtLine() {
        val passcodeTxtLayout = findViewById<LinearLayout>(R.id.passCodeTxtLayout)
        for (i in 0 until numberOfPassInputs) {
            val txt = layoutInflater.inflate(R.layout.passcode_edit_text, passcodeTxtLayout, false) as EditText
            passcodeTxtLayout.addView(txt)
            passCodeEditTexts[i] = txt
        }
        passCodeEditTexts[0]?.requestFocus()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled       'True' makes the cancel button available, 'false' hides it.
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
     * Binds the appropiate listeners to the input boxes receiving each digit of the pass code.
     */
    protected fun setTextListeners() {
        for (i in 0 until numberOfPassInputs) {
            passCodeEditTexts[i]?.addTextChangedListener(PassCodeDigitTextWatcher(i, i == numberOfPassInputs - 1))
            if (i > 0) {
                passCodeEditTexts[i]?.setOnKeyListener { v: View, keyCode: Int, _: KeyEvent? ->
                    if (keyCode == KeyEvent.KEYCODE_DEL && bChange) {  // TODO WIP: event should be used to control what's exactly happening with DEL, not any custom field...
                        passCodeEditTexts[i - 1]?.apply {
                            isEnabled = true
                            setText("")
                            requestFocus()
                        }
                        if (!confirmingPassCode) {
                            passCodeDigits[i - 1] = ""
                        }
                        bChange = false
                    } else if (!bChange) {
                        bChange = true
                    }
                    false
                }
            }
            passCodeEditTexts[i]?.onFocusChangeListener = OnFocusChangeListener { v: View, _: Boolean ->
                /// TODO WIP: should take advantage of hasFocus to reduce processing
                for (j in 0 until i) {
                    if (passCodeEditTexts[j]?.text.toString() == "") {  // TODO WIP validation
                        // could be done in a global way, with a single OnFocusChangeListener for all the
                        // input fields
                        passCodeEditTexts[j]?.requestFocus()
                        break
                    }
                }
            }
        }
    }

    /**
     * Processes the pass code entered by the user just after the last digit was in.
     *
     * Takes into account the action requested to the activity, the currently saved pass code and
     * the previously typed pass code, if any.
     */
    private fun processFullPassCode() {
        if (ACTION_CHECK == intent.action) {
            if (passCodeViewModel.checkPassCodeIsValid(passCodeDigits)) {
                /// pass code accepted in request, user is allowed to access the app
                passCodeError.visibility = View.INVISIBLE
                val preferencesProvider = SharedPreferencesProviderImpl(applicationContext)
                preferencesProvider.putLong(LAST_UNLOCK_TIMESTAMP, System.currentTimeMillis())
                hideSoftKeyboard()
                finish()
            } else {
                showErrorAndRestart(
                    R.string.pass_code_wrong, R.string.pass_code_enter_pass_code,
                    View.INVISIBLE
                )
            }
        } else if (ACTION_CHECK_WITH_RESULT == intent.action) {
            if (passCodeViewModel.checkPassCodeIsValid(passCodeDigits)) {
                passCodeViewModel.removePassCode()
                val resultIntent = Intent()
                setResult(RESULT_OK, resultIntent)
                passCodeError.visibility = View.INVISIBLE
                hideSoftKeyboard()
                notifyDocumentProviderRoots(applicationContext)
                finish()
            } else {
                showErrorAndRestart(
                    R.string.pass_code_wrong, R.string.pass_code_enter_pass_code,
                    View.INVISIBLE
                )
            }
        } else if (ACTION_REQUEST_WITH_RESULT == intent.action) {
            /// enabling pass code
            if (!confirmingPassCode) {
                passCodeError.visibility = View.INVISIBLE
                requestPassCodeConfirmation()
            } else if (confirmPassCode()) {
                /// confirmed: user typed the same pass code twice
                savePassCodeAndExit()
            } else {
                showErrorAndRestart(
                    R.string.pass_code_mismatch, R.string.pass_code_configure_your_pass_code, View.VISIBLE
                )
            }
        }
    }

    private fun showErrorAndRestart(
        errorMessage: Int, headerMessage: Int,
        explanationVisibility: Int
    ) {
        Arrays.fill(passCodeDigits, null)
        passCodeError.setText(errorMessage)
        passCodeError.visibility = View.VISIBLE
        passCodeHdr.setText(headerMessage) // TODO check if really needed
        passCodeHdrExplanation.visibility = explanationVisibility // TODO check if really needed
        clearBoxes()
    }

    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass
     * code.
     */
    protected fun requestPassCodeConfirmation() {
        clearBoxes()
        passCodeHdr.setText(R.string.pass_code_reenter_your_pass_code)
        passCodeHdrExplanation.visibility = View.INVISIBLE
        confirmingPassCode = true
    }

    /**
     * Compares pass code retyped by the user in the input fields with the value entered just
     * before.
     *
     * @return     'True' if retyped pass code equals to the entered before.
     */
    protected fun confirmPassCode(): Boolean {
        confirmingPassCode = false
        var isValid = true
        var i = 0
        while (i < passCodeEditTexts.size && isValid) {
            isValid = passCodeEditTexts[i]?.text.toString() == passCodeDigits[i]
            i++
        }
        return isValid
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected fun clearBoxes() {
        for (passCodeEditText in passCodeEditTexts) {
            passCodeEditText?.apply {
                isEnabled = true
                setText("")
            }
        }
        passCodeEditTexts[0]?.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(passCodeEditTexts[0], InputMethodManager.SHOW_IMPLICIT)
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

    /**
     * Saves the pass code input by the user as the current pass code.
     */
    protected fun savePassCodeAndExit() {
        val resultIntent = Intent()
        val passCodeString = StringBuilder()
        for (i in 0 until numberOfPassInputs) {
            passCodeString.append(passCodeDigits[i])
        }
        passCodeViewModel.setPassCode(passCodeString.toString())
        setResult(RESULT_OK, resultIntent)
        notifyDocumentProviderRoots(applicationContext)
        finish()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CONFIRMING_PASSCODE, confirmingPassCode)
        outState.putStringArray(KEY_PASSCODE_DIGITS, passCodeDigits)
    }

    /**
     * Constructor
     *
     * @param index         Position in the pass code of the input field that will be bound to
     * this watcher.
     * @param lastOne       'True' means that watcher corresponds to the last position of the
     * pass code.
     */
    private inner class PassCodeDigitTextWatcher(private val index: Int, private val lastOne: Boolean) : TextWatcher {
        private operator fun next(): Int {
            return if (lastOne) 0 else index.plus(1)
        }

        /**
         * Performs several actions when the user types a digit in an input field:
         * - saves the input digit to the state of the activity; this will allow retyping the
         * pass code to confirm it.
         * - moves the focus automatically to the next field
         * - for the last field, triggers the processing of the full pass code
         *
         * @param changedText     Changed text
         */
        override fun afterTextChanged(changedText: Editable) {
            if (changedText.isNotEmpty()) {
                if (!confirmingPassCode) {
                    passCodeDigits[index] = passCodeEditTexts[index]?.text.toString()
                }
                passCodeEditTexts[next()]?.requestFocus()
                passCodeEditTexts[index]?.isEnabled = false
                if (lastOne) {
                    processFullPassCode()
                }
            } else {
                Timber.d("Text box $index was cleaned")
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // nothing to do
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // nothing to do
        }

        init {
            require(index >= 0) {
                "Invalid index in " + PassCodeDigitTextWatcher::class.java.simpleName +
                        " constructor"
            }
        }
    }

    companion object {
        const val ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT"
        const val ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT"
        const val ACTION_CHECK = "ACTION_CHECK"

        // NOTE: PREFERENCE_SET_PASSCODE must have the same value as settings_security.xml-->android:key for passcode preference
        const val PREFERENCE_SET_PASSCODE = "set_pincode"
        const val PREFERENCE_PASSCODE = "PrefPinCode"

        // NOTE: This is required to read the legacy pin code format
        const val PREFERENCE_PASSCODE_D = "PrefPinCode"

        const val numberOfPassInputs = 4
        private const val KEY_PASSCODE_DIGITS = "PASSCODE_DIGITS"
        private const val KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE"
    }
}
