package com.owncloud.android.dependecyinjection

import com.owncloud.android.BuildConfig
import com.owncloud.android.data.device.BaseUrlChooser
import com.owncloud.android.data.device.CurrentDeviceStorage
import com.owncloud.android.data.device.DynamicBaseUrlSwitcher
import com.owncloud.android.data.device.HCDeviceUrlResolver
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.interceptor.RemoteAccessAuthInterceptor
import com.owncloud.android.data.remoteaccess.interceptor.RemoteAccessTokenRefreshInterceptor
import com.owncloud.android.domain.server.usecases.DeviceUrlResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

/**
 * Constants for Koin named qualifiers
 */
private object RemoteAccessQualifiers {
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

    // Current Device Storage
    single {
        CurrentDeviceStorage(get())
    }

    // Device URL Resolver - resolves available device URLs by priority
    single<DeviceUrlResolver> {
        HCDeviceUrlResolver(
            deviceVerificationClient = get()
        )
    }

    // Base URL Chooser - dynamically selects best available base URL
    single {
        BaseUrlChooser(
            networkStateObserver = get(),
            currentDeviceStorage = get(),
            deviceUrlResolver = get()
        )
    }

    // Dynamic Base URL Switcher - manages automatic base URL switching for accounts
    single {
        DynamicBaseUrlSwitcher(
            accountManager = get(),
            baseUrlChooser = get(),
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }

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
            currentDeviceStorage = get(),
            remoteAccessServiceLazy = inject()
        )
    }

    single(named(RemoteAccessQualifiers.OKHTTP_CLIENT)) {
        val trustAllClient = get<OkHttpClient>(named(NetworkModuleQualifiers.OKHTTP_CLIENT_TRUST_ALL))

        trustAllClient.newBuilder()
            .addInterceptor(get<RemoteAccessAuthInterceptor>(named(RemoteAccessQualifiers.AUTH_INTERCEPTOR)))
            .addInterceptor(get<RemoteAccessTokenRefreshInterceptor>(named(RemoteAccessQualifiers.TOKEN_REFRESH_INTERCEPTOR)))
            .addInterceptor(get<HttpLoggingInterceptor>(named(RemoteAccessQualifiers.LOGGING_INTERCEPTOR)))
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
}
