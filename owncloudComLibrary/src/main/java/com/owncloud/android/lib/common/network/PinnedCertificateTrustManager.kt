package com.owncloud.android.lib.common.network

import android.content.Context
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Certificate trust manager that loads and validates certificates from the assets/cert folder.
 * Only certificates stored in this folder will be trusted for SSL connections.
 */
class PinnedCertificateTrustManager(private val context: Context) {

    companion object {
        private const val CERT_FOLDER = "cert"
        private val CERT_EXTENSIONS = listOf(".cer", ".pem", ".crt")
    }

    val trustManager: X509TrustManager by lazy {
        createTrustManager()
    }

    /**
     * Creates an SSLContext configured with the trusted certificates
     */
    fun createSSLContext(): SSLContext {
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
    }

    /**
     * Creates a trust manager by loading all certificates from the assets/cert folder
     */
    private fun createTrustManager(): X509TrustManager {
        val keyStore = loadCertificatesFromAssets()

        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(keyStore)

        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Unexpected default trust managers: ${trustManagers.contentToString()}"
        }

        return trustManagers[0] as X509TrustManager
    }

    /**
     * Loads all certificate files from the assets/cert folder into a KeyStore
     */
    private fun loadCertificatesFromAssets(): KeyStore {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        try {
            val certFiles = context.assets.list(CERT_FOLDER)

            if (certFiles.isNullOrEmpty()) {
                Timber.w("No certificate files found in assets/$CERT_FOLDER folder")
                return keyStore
            }

            var certCount = 0
            certFiles.forEach { fileName ->
                // Only process files with certificate extensions
                if (CERT_EXTENSIONS.any { fileName.endsWith(it, ignoreCase = true) }) {
                    try {
                        loadCertificateFromAsset(
                            certificateFactory,
                            keyStore,
                            fileName,
                            certCount
                        )
                        certCount++
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load certificate: $fileName")
                    }
                }
            }

            Timber.d("Loaded $certCount certificate(s) from assets/$CERT_FOLDER")

            if (certCount == 0) {
                Timber.w("No valid certificates were loaded from assets/$CERT_FOLDER")
            }

        } catch (e: IOException) {
            Timber.e(e, "Error accessing assets/$CERT_FOLDER folder")
        }

        return keyStore
    }

    /**
     * Loads a single certificate file from assets into the KeyStore
     */
    private fun loadCertificateFromAsset(
        certificateFactory: CertificateFactory,
        keyStore: KeyStore,
        fileName: String,
        index: Int
    ) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open("$CERT_FOLDER/$fileName")
            val certificate = certificateFactory.generateCertificate(inputStream) as X509Certificate

            // Use the certificate's subject as the alias, fallback to index-based alias
            val alias = try {
                certificate.subjectX500Principal.name.take(50)
            } catch (e: Exception) {
                "cert_$index"
            }

            keyStore.setCertificateEntry(alias, certificate)

            Timber.d("Loaded certificate: $fileName (Subject: ${certificate.subjectX500Principal.name})")
        } catch (e: CertificateException) {
            Timber.e(e, "Invalid certificate format: $fileName")
            throw e
        } catch (e: IOException) {
            Timber.e(e, "Error reading certificate file: $fileName")
            throw e
        } finally {
            inputStream?.close()
        }
    }
}