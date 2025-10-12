package com.owncloud.android.data.remoteaccess.interceptor

import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.REMOTE_ACCESS_PATH_INITIATE
import com.owncloud.android.data.remoteaccess.datasources.REMOTE_ACCESS_PATH_TOKEN
import com.owncloud.android.data.remoteaccess.datasources.REMOTE_ACCESS_PATH_TOKEN_REFRESH
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

/**
 * OkHttp Interceptor for handling token refresh on 401/403 errors.
 * 
 * This interceptor:
 * - Intercepts 401 and 403 responses
 * - Automatically refreshes the access token using the refresh token
 * - Retries the original request with the new token
 * - Prevents race conditions by synchronizing token refresh
 * - Avoids infinite retry loops
 * 
 * Uses lazy injection to break circular dependency:
 * OkHttpClient → Interceptor → Service (lazy) → Retrofit → OkHttpClient
 */
class RemoteAccessTokenRefreshInterceptor(
    private val tokenStorage: RemoteAccessTokenStorage,
    private val remoteAccessServiceLazy: Lazy<RemoteAccessService>
) : Interceptor {

    // Lock object for synchronizing token refresh across multiple threads
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Proceed with the original request
        val response = chain.proceed(originalRequest)

        // Check if we need to handle token refresh (401 or 403)
        if ((response.code == 401 || response.code == 403) && shouldAttemptRefresh(originalRequest)) {
            Timber.d("Received ${response.code} error, attempting token refresh")
            
            // Close the original response body
            response.close()
            
            val refreshedRequest = attemptTokenRefresh(originalRequest)
            
            if (refreshedRequest != null) {
                // Retry with the new token
                Timber.d("Retrying request with refreshed token")
                return chain.proceed(refreshedRequest)
            } else {
                Timber.w("Token refresh failed, returning original error")
                // Return the original error response (recreate it)
                return chain.proceed(originalRequest)
            }
        }

        return response
    }

    /**
     * Checks if we should attempt to refresh the token for this request
     */
    private fun shouldAttemptRefresh(request: Request): Boolean {
        val path = request.url.encodedPath
        
        // Don't refresh for auth endpoints
        if (path.contains(REMOTE_ACCESS_PATH_INITIATE) ||
            path.contains(REMOTE_ACCESS_PATH_TOKEN) ||
            path.contains(REMOTE_ACCESS_PATH_TOKEN_REFRESH)) {
            return false
        }
        
        // Check if this request already has the retry marker (prevent infinite loops)
        if (request.header(RETRY_HEADER) != null) {
            Timber.w("Request already retried once, not attempting another refresh")
            return false
        }
        
        return true
    }

    /**
     * Attempts to refresh the token and returns a new request with the updated token.
     * Returns null if refresh fails.
     */
    private fun attemptTokenRefresh(originalRequest: Request): Request? {
        val currentToken = tokenStorage.getAccessToken()
        val refreshToken = tokenStorage.getRefreshToken()

        if (refreshToken.isNullOrEmpty()) {
            Timber.w("No refresh token available, cannot refresh")
            return null
        }

        // Synchronized block to prevent multiple simultaneous refresh attempts
        synchronized(refreshLock) {
            val newToken = tokenStorage.getAccessToken()
            
            // Check if token was already refreshed by another thread
            if (newToken != currentToken && !newToken.isNullOrEmpty()) {
                Timber.d("Token was already refreshed by another thread, using new token")
                return buildRequestWithToken(originalRequest, newToken)
            }

            // Attempt to refresh the token
            return try {
                Timber.d("Attempting to refresh access token")
                val tokenResponse = runBlocking {
                    remoteAccessServiceLazy.value.refreshToken(refreshToken)
                }

                // Save the new tokens
                tokenStorage.saveToken(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken
                )

                Timber.d("Token refreshed successfully")

                // Build and return the request with the new token
                buildRequestWithToken(originalRequest, tokenResponse.accessToken)
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh token")
                
                // If refresh failed with auth error, clear tokens
                if (e is retrofit2.HttpException && (e.code() == 401 || e.code() == 403)) {
                    Timber.w("Refresh token is invalid, clearing tokens")
                    tokenStorage.clearTokens()
                }
                
                // Return null to indicate refresh failure
                null
            }
        }
    }

    /**
     * Builds a new request with the updated access token and a retry marker
     */
    private fun buildRequestWithToken(originalRequest: Request, token: String): Request {
        return originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header(RETRY_HEADER, "true") // Mark as retried to prevent infinite loops
            .build()
    }

    companion object {
        private const val RETRY_HEADER = "X-Token-Retry"
    }
}

