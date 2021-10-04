package com.owncloud.android.lib.common.http

class DebugInterceptorFactory {
    companion object {
        fun getInterceptor() = DummyInterceptor()
    }
}