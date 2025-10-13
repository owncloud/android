package com.owncloud.android.data.mdnsdiscovery.datasources

import com.owncloud.android.data.mdnsdiscovery.remote.HCDeviceStatusResponse
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * HTTP client for verifying discovered devices
 */
class HCDeviceVerificationClient(
    private val moshi: Moshi
) {
    
    private val okHttpClient: OkHttpClient by lazy {
        createTrustAllOkHttpClient()
    }
    
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
        return try {
            val statusUrl = "$deviceUrl/api/v1/status"
            Timber.d("Verifying device at: $statusUrl")
            
            val request = Request.Builder()
                .url(statusUrl)
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Timber.w("Device verification failed with HTTP ${response.code} for: $deviceUrl")
                return false
            }
            
            val responseBody = response.body?.string()
            if (responseBody == null) {
                Timber.w("Device verification failed: empty response body for: $deviceUrl")
                return false
            }
            
            val statusResponse = statusAdapter.fromJson(responseBody)
            
            if (statusResponse == null) {
                Timber.w("Device verification failed: unable to parse response for: $deviceUrl")
                return false
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
    
    /**
     * Creates an OkHttpClient that trusts all SSL certificates
     * This is needed for local device discovery where devices may use self-signed certificates
     */
    private fun createTrustAllOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })
        
        val sslContext = javax.net.ssl.SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }
        
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }
}

