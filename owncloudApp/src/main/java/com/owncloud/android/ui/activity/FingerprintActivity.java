/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.ui.activity;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.owncloud.android.ui.dialog.FingerprintAuthDialogFragment;
import timber.log.Timber;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class FingerprintActivity extends AppCompatActivity {

    private static final String TAG_FINGERPRINT_FRAGMENT = "FINGERPRINT_LOCK";

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    public final static String PREFERENCE_SET_FINGERPRINT = "set_fingerprint";

    private static final String KEY_NAME = "default_key";

    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher mCipher;
    private FingerprintManager.CryptoObject mCryptoObject;

    /**
     * Initializes the activity.
     *
     * An intent with a valid ACTION is expected; if none is found, an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param savedInstanceState    Previously saved state - irrelevant in this case
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        generateAndStoreKey();

        if (initCipher()) {
            mCryptoObject = new FingerprintManager.CryptoObject(mCipher);
        }

        FingerprintAuthDialogFragment fingerprintAuthDialogFragment = new FingerprintAuthDialogFragment();

        // Attach crypto object used during the fingerprint authentication process
        fingerprintAuthDialogFragment.setCryptoObject(mCryptoObject);

        fingerprintAuthDialogFragment.show(getFragmentManager(), TAG_FINGERPRINT_FRAGMENT);
    }

    /**
     * Generate encryption key involved in fingerprint authentication process and store it securely on the device using
     * the Android Keystore system
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void generateAndStoreKey() {

        try {
            // Access Android Keystore container, used to safely store cryptographic keys on Android devices
            mKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE);

        } catch (KeyStoreException e) {
            Timber.e(e, "Failed while getting KeyStore instance");
        }

        try {
            // Access Android KeyGenerator to create the encryption key involved in fingerprint authentication process
            mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            Timber.e(e, "Failed while getting KeyGenerator instance");
        }

        try {
            // Generate and save the encryption key
            mKeyStore.load(null);
            mKeyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e) {
            Timber.e(e, "Failed while generating and saving the encryption key");
        }
    }

    /**
     * Init mCipher that will be used to create the encrypted FingerprintManager.CryptoObject instance. This
     * CryptoObject will be used during the fingerprint authentication process
     *
     * @return true if mCipher is properly initialized, false otherwise
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean initCipher() {

        try {
            mCipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Timber.e(e, "Error while generating and saving the encryption key");
        }

        try {
            mKeyStore.load(null);
            // Initialize the cipher with the key stored in the Keystore container
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            Timber.e(e, "Key permanently invalidated while initializing the cipher");
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException |
                NoSuchAlgorithmException | InvalidKeyException e) {
            Timber.e(e, "Failed while initializing the cipher");
            return false;
        }
    }
}