package com.owncloud.android.dependecyinjection

import com.owncloud.android.data.connectivity.NetworkStateObserver
import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.lib.common.network.PinnedCertificateTrustManager
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal object NetworkModuleQualifiers {
    const val OKHTTP_CLIENT_TRUST_ALL = "okHttpClient_trust_all_certs"
    const val OKHTTP_CLIENT_PINNED_CERTS = "okHttpClient_pinned_certs"
}

val networkModule = module {

    factoryOf(::PinnedCertificateTrustManager)

    // Moshi instance for JSON serialization
    factory { Moshi.Builder().build() }

    // OkHttpClient for Remote Access API
    single(named(NetworkModuleQualifiers.OKHTTP_CLIENT_TRUST_ALL)) {
        /* TODO: This is a TEMPORARY solution - trusting all certificates is insecure!
           Replace with proper certificate pinning or trusted certificate validation in production
           This should be done as part of https://jira.seagate.com/jira/browse/HCNOVEO-867
         */
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    single(named(NetworkModuleQualifiers.OKHTTP_CLIENT_PINNED_CERTS)) {
        val certificateTrustManager = get<PinnedCertificateTrustManager>()
        val sslContext = certificateTrustManager.createSSLContext()
        val trustManager = certificateTrustManager.trustManager

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true } // Disabled - devices accessed via IP
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Device Verification Client for mDNS
    single {
        HCDeviceVerificationClient(
            okHttpClient = get<OkHttpClient>(named(NetworkModuleQualifiers.OKHTTP_CLIENT_TRUST_ALL)),
            moshi = get()
        )
    }

    single {
        NetworkStateObserver(androidContext())
    }
}