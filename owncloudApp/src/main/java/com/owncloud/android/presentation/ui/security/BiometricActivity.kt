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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.presentation.ui.security.PassCodeManager.isPassCodeEnabled
import com.owncloud.android.presentation.ui.security.PatternManager.isPatternEnabled
import timber.log.Timber
import java.security.KeyStore
import java.security.KeyStoreException
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class BiometricActivity : AppCompatActivity() {

    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private lateinit var cipher: Cipher
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

        generateAndStoreKey()

        if (initCipher()) {
            cryptoObject = BiometricPrompt.CryptoObject(cipher)
        }

        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt()
        } else {
            authError()
        }
    }

    /**
     * Generate encryption key involved in biometric authentication process and store it securely on the device using
     * the Android Keystore system
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun generateAndStoreKey() {
        try {
            // Access Android Keystore container, used to safely store cryptographic keys on Android devices
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        } catch (e: KeyStoreException) {
            Timber.e(e, "Failed while getting KeyStore instance")
        }
        try {
            // Access Android KeyGenerator to create the encryption key involved in biometric authentication process
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        } catch (e: Exception) {
            Timber.e(e, "Failed while getting KeyGenerator instance")
        }
        try {
            // Generate and save the encryption key
            keyStore.load(null)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
            )
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Timber.e(e, "Failed while generating and saving the encryption key")
        }
    }

    /**
     * Init cipher that will be used to create the encrypted [BiometricPrompt.CryptoObject] instance. This
     * CryptoObject will be used during the biometric authentication process
     *
     * @return true if cipher is properly initialized, false otherwise
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun initCipher(): Boolean {
        try {
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: Exception) {
            Timber.e(e, "Error while generating and saving the encryption key")
        }

        try {
            keyStore.load(null)
            // Initialize the cipher with the key stored in the Keystore container
            val key = keyStore.getKey(KEY_NAME, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: KeyPermanentlyInvalidatedException) {
            Timber.e(e, "Key permanently invalidated while initializing the cipher")
            return false
        } catch (e: Exception) {
            Timber.e(e, "Failed while initializing the cipher")
            return false
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
                    val preferencesProvider: SharedPreferencesProvider = SharedPreferencesProviderImpl(applicationContext)
                    // In this line, null is only provisional until the rearchitecture of BiometricActivity
                    val passCode = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, null)
                    var passCodeDigits = applicationContext.resources.getInteger(R.integer.passcode_digits)
                    if (passCodeDigits < PassCodeActivity.PASSCODE_MIN_LENGTH) passCodeDigits = PassCodeActivity.PASSCODE_MIN_LENGTH
                    if (passCode != null && passCode.length < passCodeDigits) {
                        preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
                        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
                        val intent = Intent(baseContext, PassCodeActivity::class.java)
                        intent.action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
                        intent.putExtra(PassCodeActivity.EXTRAS_MIGRATION, true)
                        startActivity(intent)
                    }
                    preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, System.currentTimeMillis())
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
        if (isPassCodeEnabled()) {
            PassCodeManager.onBiometricCancelled(activity)
        } else if (isPatternEnabled()) {
            PatternManager.onBiometricCancelled(activity)
        }

        activity.finish()
    }

    companion object {
        const val PREFERENCE_SET_BIOMETRIC = "set_biometric"

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_NAME = "default_key"
    }
}