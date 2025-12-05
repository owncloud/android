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

import timber.log.Timber;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * @author David A. Velasco
 */
public class AdvancedX509TrustManager implements X509TrustManager {

    private final X509TrustManager mSystemTrustManager;
    private final X509TrustManager mLocalTrustManager;
    private final KeyStore mKnownServersKeyStore;

    /**
     * Constructor for AdvancedX509TrustManager
     *
     * @param knownServersKeyStore Local certificates store with server certificates explicitly trusted by the user.
     *                             If it contains a Root CA, all certificates signed by that CA will be trusted.
     */
    public AdvancedX509TrustManager(KeyStore knownServersKeyStore) throws NoSuchAlgorithmException, KeyStoreException {
        super();
        
        // System TrustManager for public CAs (Let's Encrypt, etc.)
        TrustManagerFactory systemFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        systemFactory.init((KeyStore) null);
        mSystemTrustManager = findX509TrustManager(systemFactory);
        
        // Local TrustManager for custom CAs (Seagate, etc.)
        TrustManagerFactory localFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        localFactory.init(knownServersKeyStore);
        mLocalTrustManager = findX509TrustManager(localFactory);
        
        mKnownServersKeyStore = knownServersKeyStore;
    }

    /**
     * Locates the first X509TrustManager provided by a given TrustManagerFactory
     *
     * @param factory TrustManagerFactory to inspect in the search for a X509TrustManager
     * @return The first X509TrustManager found in factory.
     */
    private X509TrustManager findX509TrustManager(TrustManagerFactory factory) {
        TrustManager[] tms = factory.getTrustManagers();
        for (TrustManager tm : tms) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        return null;
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],
     * String authType)
     */
    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        // Try system first, then local
        try {
            mSystemTrustManager.checkClientTrusted(certificates, authType);
        } catch (CertificateException e) {
            mLocalTrustManager.checkClientTrusted(certificates, authType);
        }
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],
     * String authType)
     */
    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) {
        if (!isKnownServer(certificates[0])) {
            CertificateCombinedException result = new CertificateCombinedException(certificates[0]);
            
            // Check certificate validity (expiration)
            try {
                certificates[0].checkValidity();
            } catch (CertificateExpiredException c) {
                result.setCertificateExpiredException(c);
            } catch (CertificateNotYetValidException c) {
                result.setCertificateNotYetException(c);
            }

            // 1. Try system TrustManager first (for public CAs like Let's Encrypt)
            try {
                mSystemTrustManager.checkServerTrusted(certificates, authType);
                Timber.d("Certificate validated by system TrustManager (public CA)");
                return; // Success with system CA
            } catch (CertificateException systemEx) {
                Timber.d("System TrustManager failed: %s", systemEx.getMessage());
            }

            // 2. Try local TrustManager (for custom CAs like Seagate)
            try {
                mLocalTrustManager.checkServerTrusted(certificates, authType);
                Timber.d("Certificate validated by local TrustManager (custom CA)");
                return; // Success with local CA
            } catch (CertificateException c) {
                // Check if it's an Extended Key Usage error
                if (isExtendedKeyUsageError(c)) {
                    Timber.d("Local validation failed due to EKU, trying manual chain validation");
                    // Try manual chain validation without EKU check
                    try {
                        validateCertificateChainWithoutEKU(certificates);
                        Timber.d("Manual chain validation successful (EKU bypassed)");
                        // Chain is valid, EKU is the only issue - we accept this
                        return;
                    } catch (CertificateException manualEx) {
                        Timber.e(manualEx, "Manual chain validation also failed");
                        result.setCertPathValidatorException(
                            new CertPathValidatorException("Certificate chain validation failed", manualEx)
                        );
                    }
                } else {
                    // Not an EKU error - handle normally
                    Throwable cause = c.getCause();
                    Throwable previousCause = null;
                    while (cause != null && cause != previousCause && !(cause instanceof CertPathValidatorException)) {
                        previousCause = cause;
                        cause = cause.getCause();
                    }
                    if (cause instanceof CertPathValidatorException) {
                        result.setCertPathValidatorException((CertPathValidatorException) cause);
                    } else {
                        result.setOtherCertificateException(c);
                    }
                }
            }

            if (result.isException()) {
                throw result;
            }
        }
    }

    /**
     * Check if the exception is related to Extended Key Usage validation
     */
    private boolean isExtendedKeyUsageError(CertificateException e) {
        String message = e.getMessage();
        if (message != null && message.toLowerCase().contains("extendedkeyusage")) {
            return true;
        }
        
        Throwable cause = e.getCause();
        while (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && causeMessage.toLowerCase().contains("extendedkeyusage")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Validate certificate chain manually without Extended Key Usage check.
     * This validates chain signatures and ensures the chain terminates at a trusted CA.
     */
    private void validateCertificateChainWithoutEKU(X509Certificate[] certificates) throws CertificateException {
        if (certificates == null || certificates.length == 0) {
            throw new CertificateException("Empty certificate chain");
        }

        try {
            // First try PKIX validation
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(Arrays.asList(certificates));
            PKIXParameters params = new PKIXParameters(mKnownServersKeyStore);
            params.setRevocationEnabled(false);
            
            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXCertPathValidatorResult validatorResult = (PKIXCertPathValidatorResult) validator.validate(certPath, params);
            
            Timber.d("Certificate chain validated successfully. Trust anchor: %s", 
                validatorResult.getTrustAnchor().getTrustedCert().getSubjectX500Principal());
                
        } catch (java.security.cert.CertPathValidatorException e) {
            // If PKIX fails due to EKU, fall back to manual signature chain validation
            if (isExtendedKeyUsageError(new CertificateException(e))) {
                Timber.d("PKIX failed due to EKU, performing manual chain validation");
                validateChainSignaturesManually(certificates);
            } else {
                throw new CertificateException("Certificate path validation failed: " + e.getMessage(), e);
            }
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new CertificateException("Failed to initialize certificate validator", e);
        }
    }

    /**
     * Manually validate certificate chain by checking signatures.
     * Ensures each certificate is signed by the next one in the chain,
     * and the chain ends with a certificate signed by a trusted CA.
     */
    private void validateChainSignaturesManually(X509Certificate[] certificates) throws CertificateException {
        // Check validity of each certificate
        for (X509Certificate cert : certificates) {
            cert.checkValidity();
        }

        // Verify signatures in chain: each cert should be signed by the next
        for (int i = 0; i < certificates.length - 1; i++) {
            X509Certificate cert = certificates[i];
            X509Certificate issuer = certificates[i + 1];
            try {
                cert.verify(issuer.getPublicKey());
            } catch (Exception e) {
                throw new CertificateException("Certificate chain signature verification failed at index " + i, e);
            }
        }

        // The last certificate in chain should be signed by a trusted CA from our keystore
        X509Certificate lastCert = certificates[certificates.length - 1];
        boolean trustedIssuerFound = false;
        
        try {
            Enumeration<String> aliases = mKnownServersKeyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (mKnownServersKeyStore.isCertificateEntry(alias)) {
                    X509Certificate trustedCert = (X509Certificate) mKnownServersKeyStore.getCertificate(alias);
                    try {
                        // Check if last cert is signed by this trusted CA
                        lastCert.verify(trustedCert.getPublicKey());
                        // Also verify the issuer DN matches
                        if (lastCert.getIssuerX500Principal().equals(trustedCert.getSubjectX500Principal())) {
                            trustedIssuerFound = true;
                            Timber.d("Chain validated manually. Trusted CA: %s", trustedCert.getSubjectX500Principal());
                            break;
                        }
                    } catch (Exception ignored) {
                        // This trusted cert is not the issuer, try next
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new CertificateException("Failed to access keystore", e);
        }

        if (!trustedIssuerFound) {
            // Maybe the last cert itself is in the keystore (self-signed or directly trusted)
            try {
                if (mKnownServersKeyStore.getCertificateAlias(lastCert) != null) {
                    trustedIssuerFound = true;
                    Timber.d("Last certificate in chain is directly trusted");
                }
            } catch (KeyStoreException e) {
                throw new CertificateException("Failed to check keystore", e);
            }
        }

        if (!trustedIssuerFound) {
            throw new CertificateException("Certificate chain does not terminate at a trusted CA");
        }
    }

    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // Combine issuers from both trust managers
        X509Certificate[] systemIssuers = mSystemTrustManager.getAcceptedIssuers();
        X509Certificate[] localIssuers = mLocalTrustManager.getAcceptedIssuers();
        
        X509Certificate[] combined = new X509Certificate[systemIssuers.length + localIssuers.length];
        System.arraycopy(systemIssuers, 0, combined, 0, systemIssuers.length);
        System.arraycopy(localIssuers, 0, combined, systemIssuers.length, localIssuers.length);
        
        return combined;
    }

    public boolean isKnownServer(X509Certificate cert) {
        try {
            return (mKnownServersKeyStore.getCertificateAlias(cert) != null);
        } catch (KeyStoreException e) {
            Timber.e(e, "Fail while checking certificate in the known-servers store");
            return false;
        }
    }
}
