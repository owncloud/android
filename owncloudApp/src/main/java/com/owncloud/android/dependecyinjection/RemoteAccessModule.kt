package com.owncloud.android.dependecyinjection

import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.interceptor.RemoteAccessAuthInterceptor
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Dependency injection module for Remote Access API (Device Resolver)
 */
val remoteAccessModule = module {

    // Base URL for Remote Access API
    single(named("remoteAccessBaseUrl")) {
        "https://hc-remote-access-env-https.eba-a2nvhpbm.us-west-2.elasticbeanstalk.com/api/"
    }

    // Moshi instance for JSON serialization
    single(named("remoteAccessMoshi")) {
        Moshi.Builder().build()
    }

    // Logging interceptor for debugging
    single(named("remoteAccessLoggingInterceptor")) {
        HttpLoggingInterceptor { message ->
            Timber.tag("RemoteAccessAPI").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    // Auth interceptor for Bearer token
    single(named("remoteAccessAuthInterceptor")) {
        RemoteAccessAuthInterceptor()
    }

    // OkHttpClient for Remote Access API
    single(named("remoteAccessOkHttpClient")) {
        OkHttpClient.Builder()
            .addInterceptor(get<RemoteAccessAuthInterceptor>(named("remoteAccessAuthInterceptor")))
            .addInterceptor(get<HttpLoggingInterceptor>(named("remoteAccessLoggingInterceptor")))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit instance for Remote Access API
    single(named("remoteAccessRetrofit")) {
        Retrofit.Builder()
            .baseUrl(get<String>(named("remoteAccessBaseUrl")))
            .client(get(named("remoteAccessOkHttpClient")))
            .addConverterFactory(MoshiConverterFactory.create(get(named("remoteAccessMoshi"))))
            .build()
    }

    // Remote Access Service
    single {
        get<Retrofit>(named("remoteAccessRetrofit")).create(RemoteAccessService::class.java)
    }
}

