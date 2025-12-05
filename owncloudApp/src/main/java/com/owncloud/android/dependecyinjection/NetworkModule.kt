package com.owncloud.android.dependecyinjection

import com.owncloud.android.data.connectivity.NetworkStateObserver
import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.lib.common.http.HttpClient
import com.owncloud.android.lib.common.network.AssetsCertificateReader
import com.owncloud.android.lib.common.network.PinnedCertificateTrustManager
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

internal object NetworkModuleQualifiers {
    const val OKHTTP_CLIENT_PINNED_HC_CERT = "okHttpClient_pinned_hc_cert"
    const val OKHTTP_CLIENT_PINNED_DEVICE_CERTS = "okHttpClient_pinned_device_certs"
}

val networkModule = module {

    factory {
        PinnedCertificateTrustManager(AssetsCertificateReader(androidContext().assets))
    }

    factory { Moshi.Builder().build() }

    // OkHttpClient for Remote Access API
    single(named(NetworkModuleQualifiers.OKHTTP_CLIENT_PINNED_HC_CERT)) {
        val certificateTrustManager = get<PinnedCertificateTrustManager>()
        val sslContext = certificateTrustManager.createSSLContext()
        val trustManager = certificateTrustManager.trustManager

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // OkHttpClient for device access
    single(named(NetworkModuleQualifiers.OKHTTP_CLIENT_PINNED_DEVICE_CERTS)) {
        HttpClient(androidContext()).okHttpClient
    }

    // Device Verification Client for mDNS
    single {
        HCDeviceVerificationClient(
            okHttpClient = get<OkHttpClient>(named(NetworkModuleQualifiers.OKHTTP_CLIENT_PINNED_DEVICE_CERTS)),
            moshi = get()
        )
    }

    single {
        NetworkStateObserver(androidContext())
    }
}