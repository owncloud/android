package com.owncloud.android.data.remoteaccess.interceptor

import com.owncloud.android.data.remoteaccess.datasources.REMOTE_ACCESS_PATH_INITIATE
import com.owncloud.android.data.remoteaccess.datasources.REMOTE_ACCESS_PATH_TOKEN
import com.owncloud.android.data.remoteaccess.datasources.REMOTE_ACCESS_PATH_TOKEN_REFRESH
import okhttp3.Interceptor
import okhttp3.Response

class RemoteAccessAuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Check if the endpoint requires authentication
        val requiresAuth = !originalRequest.url.encodedPath.contains(REMOTE_ACCESS_PATH_INITIATE) &&
                !originalRequest.url.encodedPath.contains(REMOTE_ACCESS_PATH_TOKEN) &&
                !originalRequest.url.encodedPath.contains(REMOTE_ACCESS_PATH_TOKEN_REFRESH)

        if (requiresAuth) {
            // TODO: Get token from shared preferences or token manager
            val token = "" // Replace with actual token retrieval logic

            val request = if (token.isNotEmpty()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            return chain.proceed(request)
        } else {
            return chain.proceed(originalRequest)
        }
    }
}