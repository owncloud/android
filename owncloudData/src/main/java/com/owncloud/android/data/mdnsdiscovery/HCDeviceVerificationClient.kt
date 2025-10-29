package com.owncloud.android.data.mdnsdiscovery

import com.owncloud.android.data.mdnsdiscovery.remote.HCDeviceStatusResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * HTTP client for verifying discovered devices
 */
class HCDeviceVerificationClient(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {

    private val statusAdapter by lazy {
        moshi.adapter(HCDeviceStatusResponse::class.java)
    }

    /**
     * Verifies if a device is alive by checking the /api/v1/status endpoint
     *
     * @param deviceUrl The base URL of the device (e.g., "https://192.168.1.100:8080")
     * @return true if the device responds with a valid status indicating it's ready, false otherwise
     */
    suspend fun verifyDevice(deviceUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val statusUrl = "$deviceUrl/api/v1/status"
                Timber.d("Verifying device at: $statusUrl")

                val request = Request.Builder()
                    .url(statusUrl)
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Timber.w("Device verification failed with HTTP ${response.code} for: $deviceUrl")
                    return@withContext false
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Timber.w("Device verification failed: empty response body for: $deviceUrl")
                    return@withContext false
                }

                val statusResponse = statusAdapter.fromJson(responseBody)

                if (statusResponse == null) {
                    Timber.w("Device verification failed: unable to parse response for: $deviceUrl")
                    return@withContext false
                }

                // Verify that the device is in ready state with OOBE completed
                val isReady = statusResponse.state == "ready" &&
                        statusResponse.oobe.done &&
                        statusResponse.apps.files == "ready" &&
                        statusResponse.apps.photos == "ready"

                if (isReady) {
                    Timber.d("Device verified successfully: $deviceUrl")
                } else {
                    Timber.w("Device not ready: $deviceUrl - $statusResponse")
                }

                isReady
            } catch (e: Exception) {
                Timber.w(e, "Device verification failed for: $deviceUrl")
                false
            }
        }
    }
}