/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.common.network;

import android.content.Context;

import timber.log.Timber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class NetworkUtils {

    private static String LOCAL_TRUSTSTORE_FILENAME = "knownServers.bks";

    private static String LOCAL_TRUSTSTORE_PASSWORD = "password";

    private static KeyStore mKnownServersStore = null;

    /**
     * Returns the local store of reliable server certificates, explicitly accepted by the user.
     * <p>
     * Returns a KeyStore instance with empty content if the local store was never created.
     * <p>
     * Loads the store from the storage environment if needed.
     *
     * @param context Android context where the operation is being performed.
     * @return KeyStore instance with explicitly-accepted server certificates.
     * @throws KeyStoreException        When the KeyStore instance could not be created.
     * @throws IOException              When an existing local trust store could not be loaded.
     * @throws NoSuchAlgorithmException When the existing local trust store was saved with an unsupported algorithm.
     * @throws CertificateException     When an exception occurred while loading the certificates from the local
     *                                  trust store.
     */
    public static KeyStore getKnownServersStore(Context context)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (mKnownServersStore == null) {
            //mKnownServersStore = KeyStore.getInstance("BKS");
            mKnownServersStore = KeyStore.getInstance(KeyStore.getDefaultType());
            File localTrustStoreFile = new File(context.getFilesDir(), LOCAL_TRUSTSTORE_FILENAME);
            Timber.d("Searching known-servers store at %s", localTrustStoreFile.getAbsolutePath());
            if (localTrustStoreFile.exists()) {
                InputStream in = new FileInputStream(localTrustStoreFile);
                try {
                    mKnownServersStore.load(in, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
                } finally {
                    in.close();
                }
            } else {
                // next is necessary to initialize an empty KeyStore instance
                mKnownServersStore.load(null, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
            }
        }
        return mKnownServersStore;
    }

    public static void addCertToKnownServersStore(Certificate cert, Context context)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        KeyStore knownServers = getKnownServersStore(context);
        knownServers.setCertificateEntry(Integer.toString(cert.hashCode()), cert);
        try (FileOutputStream fos = context.openFileOutput(LOCAL_TRUSTSTORE_FILENAME, Context.MODE_PRIVATE)) {
            knownServers.store(fos, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
        }
    }

}
