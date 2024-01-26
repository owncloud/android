/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.security.biometric

import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import com.owncloud.android.R
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.presentation.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.security.passcode.PassCodeActivity
import com.owncloud.android.providers.ContextProvider
import timber.log.Timber
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class BiometricViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider
) : ViewModel() {

    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private lateinit var cipher: Cipher

    /**
     * Init cipher that will be used to create the encrypted [BiometricPrompt.CryptoObject] instance. This
     * CryptoObject will be used during the biometric authentication process
     *
     * This won't be used to encrypt data, just to rely on its exclusiveness and safe access
     *
     * @return the cipher if it is properly initialized, null otherwise
     */
    fun initCipher(): Cipher? {
        generateAndStoreKey()

        try {
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/" +
                        KeyProperties.BLOCK_MODE_CBC + "/" +
                        KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
        } catch (e: Exception) {
            Timber.e(e, "Error while generating and saving the encryption key")
        }

        return try {
            keyStore.load(null)
            // Initialize the cipher with the key stored in the Keystore container
            val key = keyStore.getKey(BiometricActivity.KEY_NAME, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            Timber.e(e, "Key permanently invalidated while initializing the cipher")
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed while initializing the cipher")
            null
        }
    }

    fun setLastUnlockTimestamp() {
        preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, SystemClock.elapsedRealtime())
    }

    fun shouldAskForNewPassCode(): Boolean {
        val passCode = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, loadPinFromOldFormatIfPossible())
        val passCodeDigits = maxOf(contextProvider.getInt(R.integer.passcode_digits), PassCodeActivity.PASSCODE_MIN_LENGTH)
        return (passCode != null && passCode.length < passCodeDigits)
    }

    fun removePassCode() {
        preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
    }

    /**
     * Generate encryption key involved in biometric authentication process and store it securely on the device using
     * the Android Keystore system
     */
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
                KeyGenParameterSpec.Builder(
                    BiometricActivity.KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7
                    )
                    .build()
            )
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Timber.e(e, "Failed while generating and saving the encryption key")
        }
    }

    private fun loadPinFromOldFormatIfPossible(): String? {
        var pinString = ""
        for (i in 1..4) {
            val pinChar = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE_D + i, null)
            pinChar?.let { pinString += pinChar }
        }
        return if (pinString.isEmpty()) null else pinString
    }

    fun isBiometricLockAvailable(): Boolean {
        return if (!BiometricManager.isHardwareDetected()) { // Biometric not supported
            false
        } else BiometricManager.hasEnrolledBiometric() // Biometric not enrolled
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}
