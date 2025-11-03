package com.owncloud.android.data.mdnsdiscovery

import com.owncloud.android.data.mdnsdiscovery.remote.HCDeviceAboutResponse
import com.owncloud.android.data.mdnsdiscovery.remote.HCDeviceStatusResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
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

    private val aboutAdapter by lazy {
        moshi.adapter(HCDeviceAboutResponse::class.java)
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

                val responseBody = makeRequest(statusUrl)
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Device verification failed for: $deviceUrl")
                false
            }
        }
    }

    private fun makeRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            Timber.w("Failed to fetch about info with HTTP ${response.code} for: $url")
            return null
        }

        return response.body?.string()
    }

    /**
     * Fetches the certificate common name from the /api/v1/about endpoint.
     *
     * @param deviceUrl The base URL of the device (e.g., "https://192.168.1.100:8080")
     * @return The certificate_common_name string, or null if it cannot be fetched or parsed.
     */
    suspend fun getCertificateCommonName(deviceUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val aboutUrl = "$deviceUrl/api/v1/about"
                Timber.d("Fetching about info from: $aboutUrl")

                val responseBody = makeRequest(aboutUrl)
                if (responseBody == null) {
                    Timber.w("Failed to fetch about info: empty response body for: $deviceUrl")
                    return@withContext null
                }

                val aboutResponse = aboutAdapter.fromJson(responseBody)

                if (aboutResponse == null) {
                    Timber.w("Failed to fetch about info: unable to parse response for: $deviceUrl")
                    return@withContext null
                }

                aboutResponse.certificateCommonName
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch about info for: $deviceUrl")
                null
            }
        }
    }
}