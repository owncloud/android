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
import com.owncloud.android.presentation.security.biometric.BiometricActivity.Companion.KEY_NAME
import com.owncloud.android.presentation.security.passcode.PassCodeActivity
import com.owncloud.android.providers.ContextProvider
import timber.log.Timber
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class BiometricViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider
) : ViewModel() {

    private lateinit var keyStore: KeyStore
    private lateinit var secretKey: SecretKey
    private lateinit var keyGenerator: KeyGenerator
    private lateinit var cipher: Cipher

    /**
     * Init cipher that will be used to create the encrypted [BiometricPrompt.CryptoObject] instance. This
     * CryptoObject will be used during the biometric authentication process
     *
     * @return the cipher if it is properly initialized, null otherwise
     */
    fun initCipher(): Cipher? {

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
            // Initialize the cipher with the key stored in the Keystore container
            val secretKey = getOrCreateSecretKey()
            val initializationVector = preferencesProvider.getByteArray(PassCodeActivity.PREFERENCE_IV_ENCRYPTED, null)

            if (initializationVector == null) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(initializationVector))
            }
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

    fun encryptedOrDecryptedPassCode(cipher: Cipher?): Boolean {

        val passCode = preferencesProvider.getString(PassCodeActivity.PREFERENCE_PASSCODE, loadPinFromOldFormatIfPossible())
        val passCodeEncrypted = preferencesProvider.getByteArray(PassCodeActivity.PREFERENCE_PASSCODE_ENCRYPTED, null)

        return if (passCodeEncrypted == null) {
            preferencesProvider.putByteArray(PassCodeActivity.PREFERENCE_IV_ENCRYPTED, cipher?.iv)
            val encryptedInfo = cipher?.doFinal(passCode?.toByteArray(Charset.defaultCharset()))
            preferencesProvider.putByteArray(PassCodeActivity.PREFERENCE_PASSCODE_ENCRYPTED, encryptedInfo)
            true
        } else {
            val decryptedInfo = cipher?.doFinal(passCodeEncrypted)
            if (decryptedInfo != null) {
                String(decryptedInfo, Charset.defaultCharset()) == passCode
            } else {
                false
            }
        }
    }

    fun removePassCode() {
        preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
        preferencesProvider.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_PASSCODE_ENCRYPTED)
        preferencesProvider.removePreference(PassCodeActivity.PREFERENCE_IV_ENCRYPTED)
    }

    /**
     * Generate encryption key involved in biometric authentication process and store it securely on the device using
     * the Android Keystore system
     */
    private fun getOrCreateSecretKey(): SecretKey {

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
            keyStore.load(null) // Keystore must be loaded before it can be accessed
            keyStore.getKey(KEY_NAME, null)?.let { return it as SecretKey }

            // if you reach here, then a new SecretKey must be generated for that keyName
            val paramsBuilder = KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            paramsBuilder.apply {
                setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                setUserAuthenticationRequired(true)
            }

            val keyGenParams = paramsBuilder.build()

            keyGenerator.init(keyGenParams)
            secretKey = keyGenerator.generateKey()

        } catch (e: Exception) {
            Timber.e(e, "Failed while generating and saving the encryption key")
        }

        return secretKey
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
