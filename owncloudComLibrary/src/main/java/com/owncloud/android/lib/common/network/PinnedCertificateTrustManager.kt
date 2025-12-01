package com.owncloud.android.lib.common.network

import timber.log.Timber
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Certificate trust manager that loads and validates certificates from the assets/cert folder.
 * Only certificates stored in this folder will be trusted for SSL connections.
 */
class PinnedCertificateTrustManager(
    private val assetsCertificateReader: CertificateReader
) {

    val trustManager: X509TrustManager by lazy {
        createTrustManager()
    }

    fun createSSLContext(): SSLContext {
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
    }

    private fun createTrustManager(): X509TrustManager {
        val keyStore = initKeystore()

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

    private fun initKeystore(): KeyStore {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        assetsCertificateReader.readCertificates().forEachIndexed { index, certificate ->
            val alias = try {
                certificate.subjectX500Principal.name.take(50)
            } catch (e: Exception) {
                "cert_$index"
            }

            keyStore.setCertificateEntry(alias, certificate)

            Timber.d("Loaded certificate: (Subject: ${certificate.subjectX500Principal.name})")
        }
        return keyStore
    }

}