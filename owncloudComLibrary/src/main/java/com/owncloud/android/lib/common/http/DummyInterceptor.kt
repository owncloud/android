package com.owncloud.android.lib.common.http

import okhttp3.Interceptor
import okhttp3.Response

class DummyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}