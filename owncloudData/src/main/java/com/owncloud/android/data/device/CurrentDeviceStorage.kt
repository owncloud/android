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
     * @param pathType Type of the path (LOCAL, PUBLIC, REMOTE)
     * @param baseUrl Complete base URL (e.g., "https://192.168.1.1:8080")
     */
    fun saveDeviceBaseUrl(pathType: String, baseUrl: String) {
        val key = buildKey(pathType)
        sharedPreferencesProvider.putString(key, baseUrl)
    }

    fun saveCertificateCommonName(commonName: String) {
        sharedPreferencesProvider.putString(KEY_CERTIFICATE_COMMON_NAME, commonName)
    }

    fun getCertificateCommonName(): String? {
        return sharedPreferencesProvider.getString(KEY_CERTIFICATE_COMMON_NAME, null)
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
        sharedPreferencesProvider.removePreference(KEY_CERTIFICATE_COMMON_NAME)
    }

    /**
     * Build SharedPreferences key from path type
     */
    private fun buildKey(pathType: String): String {
        return KEY_PREFIX + pathType
    }

    companion object {
        private const val KEY_PREFIX = "KEY_DEVICE_PATH"
        private const val KEY_CERTIFICATE_COMMON_NAME = "KEY_CERTIFICATE_COMMON_NAME"

    }
}