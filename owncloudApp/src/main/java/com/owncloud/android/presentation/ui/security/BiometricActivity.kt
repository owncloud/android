/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.presentation.viewmodels.security.BiometricViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.concurrent.Executor

class BiometricActivity : AppCompatActivity() {

    // ViewModel
    private val biometricViewModel by viewModel<BiometricViewModel>()

    private lateinit var cryptoObject: BiometricPrompt.CryptoObject
    private lateinit var activity: BiometricActivity
    private val handler = Handler()
    private val executor = Executor { command -> handler.post(command) }

    /**
     * Initializes the activity.
     * <p>
     * An intent with a valid ACTION is expected; if none is found, an
     * [IllegalArgumentException] will be thrown.
     *
     * @param savedInstanceState Previously saved state - irrelevant in this case
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = this

        biometricViewModel.initCipher()?.let {
            cryptoObject = BiometricPrompt.CryptoObject(it)
        }

        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt()
        } else {
            authError()
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(getString(android.R.string.cancel))
            .setConfirmationRequired(true)
            .build()
        val biometricPrompt = BiometricPrompt(this@BiometricActivity,
            executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("onAuthenticationError ($errorCode): $errString")
                    authError()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (biometricViewModel.shouldAskForNewPassCode()) {
                        biometricViewModel.removePassCode()
                        val intent = Intent(baseContext, PassCodeActivity::class.java)
                        intent.action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
                        intent.putExtra(PassCodeActivity.EXTRAS_MIGRATION, true)
                        startActivity(intent)
                    }
                    biometricViewModel.setLastUnlockTimestamp()
                    activity.finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.e("onAuthenticationFailed")
                }
            })

        // Displays the "log in" prompt.
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    private fun authError() {
        if (PassCodeManager.isPassCodeEnabled()) {
            PassCodeManager.onBiometricCancelled(activity)
        } else if (PatternManager.isPatternEnabled()) {
            PatternManager.onBiometricCancelled(activity)
        }

        activity.finish()
    }

    companion object {
        const val PREFERENCE_SET_BIOMETRIC = "set_biometric"

        const val KEY_NAME = "default_key"
    }
}
