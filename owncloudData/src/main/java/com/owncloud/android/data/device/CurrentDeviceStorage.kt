package com.owncloud.android.data.device

import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.domain.device.model.DevicePathType

/**
 * Storage for current device access paths.
 * Stores LOCAL, PUBLIC, and REMOTE base URLs for the device.
 *
 * @author Alexey Pushkarev
 */
class CurrentDeviceStorage(
    private val sharedPreferencesProvider: SharedPreferencesProvider
) {

    /**
     * Save device base URL by type
     * @param baseUrl Complete base URL (e.g., "https://192.168.1.1:8080")
     * @param pathType Type of the path (LOCAL, PUBLIC, REMOTE)
     */
    fun saveDeviceBaseUrl(baseUrl: String, pathType: String) {
        val key = buildKey(pathType)
        sharedPreferencesProvider.putString(key, baseUrl)
    }

    /**
     * Get device base URL by type
     * @param pathType Type of the path (LOCAL, PUBLIC, REMOTE)
     * @return Base URL or null if not found
     */
    fun getDeviceBaseUrl(pathType: String): String? {
        val key = buildKey(pathType)
        return sharedPreferencesProvider.getString(key, null)
    }

    /**
     * Clear all stored device paths
     */
    fun clearDevicePaths() {
        DevicePathType.entries.forEach { type ->
            sharedPreferencesProvider.removePreference(buildKey(type.name))
        }
    }

    /**
     * Build SharedPreferences key from path type
     */
    private fun buildKey(pathType: String): String {
        return KEY_PREFIX + pathType
    }

    companion object {
        private const val KEY_PREFIX = "KEY_DEVICE_PATH"

    }
}