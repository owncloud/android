package com.owncloud.android.data.remoteaccess

import com.owncloud.android.data.providers.SharedPreferencesProvider

class RemoteAccessTokenStorage(
    private val sharedPreferencesProvider: SharedPreferencesProvider
) {

    fun saveToken(accessToken: String, refreshToken: String) {
        sharedPreferencesProvider.putString(KEY_ACCESS_TOKEN, accessToken)
        sharedPreferencesProvider.putString(KEY_REFRESH_TOKEN, refreshToken)
    }

    fun getAccessToken(): String? {
        return sharedPreferencesProvider.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferencesProvider.getString(KEY_REFRESH_TOKEN, null)
    }

    fun clearTokens() {
        sharedPreferencesProvider.removePreference(KEY_ACCESS_TOKEN)
        sharedPreferencesProvider.removePreference(KEY_REFRESH_TOKEN)
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "remote_access_access_token"
        private const val KEY_REFRESH_TOKEN = "remote_access_refresh_token"
    }
}

