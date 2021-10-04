package com.owncloud.android.lib.common.http

import com.facebook.stetho.okhttp3.StethoInterceptor

class DebugInterceptorFactory {
    companion object {
        fun getInterceptor() = StethoInterceptor()
    }
}
