package com.owncloud.android.dependecyinjection

import com.owncloud.android.BuildConfig
import com.owncloud.android.data.mdnsdiscovery.LocalMdnsDiscoveryDataSource
import com.owncloud.android.data.mdnsdiscovery.datasources.HCDeviceVerificationClient
import com.owncloud.android.data.mdnsdiscovery.implementation.HCLocalMdnsDiscoveryDataSource
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.interceptor.RemoteAccessAuthInterceptor
import com.owncloud.android.data.remoteaccess.interceptor.RemoteAccessTokenRefreshInterceptor
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Constants for Koin named qualifiers
 */
object RemoteAccessQualifiers {
    const val BASE_URL = "remoteAccessBaseUrl"
    const val LOGGING_INTERCEPTOR = "remoteAccessLoggingInterceptor"
    const val AUTH_INTERCEPTOR = "remoteAccessAuthInterceptor"
    const val TOKEN_REFRESH_INTERCEPTOR = "remoteAccessTokenRefreshInterceptor"
    const val OKHTTP_CLIENT = "remoteAccessOkHttpClient"
    const val RETROFIT = "remoteAccessRetrofit"
}

/**
 * Dependency injection module for Remote Access API (Device Resolver)
 */
val remoteAccessModule = module {

    // Base URL for Remote Access API
    single(named(RemoteAccessQualifiers.BASE_URL)) {
        BuildConfig.REMOTE_ACCESS_BASE_URL
    }

    // Token Storage
    single {
        RemoteAccessTokenStorage(get())
    }

    // Moshi instance for JSON serialization
    single { Moshi.Builder().build() }

    // Logging interceptor for debugging
    single(named(RemoteAccessQualifiers.LOGGING_INTERCEPTOR)) {
        HttpLoggingInterceptor { message ->
            Timber.tag("RemoteAccessAPI").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    // Auth interceptor for Bearer token
    single(named(RemoteAccessQualifiers.AUTH_INTERCEPTOR)) {
        RemoteAccessAuthInterceptor(get())
    }

    // Token refresh interceptor for automatic token refresh on 401/403
    // Uses lazy injection to break circular dependency with RemoteAccessService
    single(named(RemoteAccessQualifiers.TOKEN_REFRESH_INTERCEPTOR)) {
        RemoteAccessTokenRefreshInterceptor(
            tokenStorage = get(),
            remoteAccessServiceLazy = inject()
        )
    }

    // OkHttpClient for Remote Access API
    single(named(RemoteAccessQualifiers.OKHTTP_CLIENT)) {
        // TODO: This is a TEMPORARY solution - trusting all certificates is insecure!
        // Replace with proper certificate pinning or trusted certificate validation in production
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        OkHttpClient.Builder()
            .addInterceptor(get<RemoteAccessAuthInterceptor>(named(RemoteAccessQualifiers.AUTH_INTERCEPTOR)))
            .addInterceptor(get<RemoteAccessTokenRefreshInterceptor>(named(RemoteAccessQualifiers.TOKEN_REFRESH_INTERCEPTOR)))
            .addInterceptor(get<HttpLoggingInterceptor>(named(RemoteAccessQualifiers.LOGGING_INTERCEPTOR)))
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit instance for Remote Access API
    single(named(RemoteAccessQualifiers.RETROFIT)) {
        Retrofit.Builder()
            .baseUrl(get<String>(named(RemoteAccessQualifiers.BASE_URL)))
            .client(get(named(RemoteAccessQualifiers.OKHTTP_CLIENT)))
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    // Remote Access Service
    single {
        get<Retrofit>(named(RemoteAccessQualifiers.RETROFIT)).create(RemoteAccessService::class.java)
    }

    // mDNS Discovery Data Source
    singleOf(::HCLocalMdnsDiscoveryDataSource) bind LocalMdnsDiscoveryDataSource::class
    
    // Device Verification Client for mDNS
    single {
        HCDeviceVerificationClient(get())
    }

}
