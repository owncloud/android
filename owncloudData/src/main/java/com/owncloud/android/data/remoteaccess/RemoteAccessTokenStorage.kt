package com.owncloud.android.data.remoteaccess

import com.owncloud.android.data.providers.SharedPreferencesProvider

class RemoteAccessTokenStorage(
    private val sharedPreferencesProvider: SharedPreferencesProvider
) {

    // Lock object for thread-safe token operations
    private val tokenLock = Any()

    fun saveToken(accessToken: String, refreshToken: String) {
        synchronized(tokenLock) {
            sharedPreferencesProvider.putString(KEY_ACCESS_TOKEN, accessToken)
            sharedPreferencesProvider.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    fun getAccessToken(): String? {
        synchronized(tokenLock) {
            return sharedPreferencesProvider.getString(KEY_ACCESS_TOKEN, null)
        }
    }

    fun getRefreshToken(): String? {
        synchronized(tokenLock) {
            return sharedPreferencesProvider.getString(KEY_REFRESH_TOKEN, null)
        }
    }

    fun clearTokens() {
        synchronized(tokenLock) {
            sharedPreferencesProvider.removePreference(KEY_ACCESS_TOKEN)
            sharedPreferencesProvider.removePreference(KEY_REFRESH_TOKEN)
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "remote_access_access_token"
        private const val KEY_REFRESH_TOKEN = "remote_access_refresh_token"
    }
}

