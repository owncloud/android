package com.owncloud.android.data.mdnsdiscovery.implementation

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.owncloud.android.data.mdnsdiscovery.datasources.LocalMdnsDiscoveryDataSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import kotlin.time.Duration

class HCLocalMdnsDiscoveryDataSource(
    private val context: Context
) : LocalMdnsDiscoveryDataSource {

    override fun discoverDevices(
        serviceType: String,
        serviceName: String,
        duration: Duration
    ): Flow<String> = callbackFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

        if (nsdManager == null) {
            Timber.w("NsdManager is not available")
            close()
            return@callbackFlow
        }

        val discoveryListener = getDiscoveryListener(
            doOnServiceFound = { service ->
                // Filter by service name if provided
                if (serviceName.isNotEmpty() && !service.serviceName.contains(serviceName, ignoreCase = true)) {
                    Timber.d("Service name doesn't match filter, skipping: ${service.serviceName}")
                    return@getDiscoveryListener
                }

                nsdManager.resolveService(
                    service = service,
                    onServiceResolved = { serviceUrl ->
                        trySend(serviceUrl)
                    }
                )
            },
            doOnFailed = {
                close()
            }
        )

        try {
            // Start discovery
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

            // Wait for the specified duration
            delay(duration)

            // Stop discovery
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: IllegalArgumentException) {
                Timber.w("Discovery listener was not registered or already stopped: ${e.message}")
            }
        } catch (e: Exception) {
            Timber.w(e, "Error during mDNS discovery")
            close(e)
        }

        awaitClose {
            // Ensure discovery is stopped when flow is cancelled
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Timber.w("Error stopping discovery in awaitClose: ${e.message}")
            }
        }
    }

    private fun getDiscoveryListener(
        doOnServiceFound: (NsdServiceInfo) -> Unit,
        doOnFailed: () -> Unit,
    ): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("mDNS Discovery started for type: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.d("Service found: ${service.serviceName}, type: ${service.serviceType}")
                doOnServiceFound(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Timber.d("Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("mDNS Discovery stopped for type: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Failed to start discovery for type: $serviceType, error code: $errorCode")
                doOnFailed()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Failed to stop discovery for type: $serviceType, error code: $errorCode")
            }
        }
    }

    private fun NsdManager.resolveService(service: NsdServiceInfo, onServiceResolved: (String) -> Unit) {
        // Resolve the service to get host and port
        resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.w("Failed to resolve service: ${serviceInfo.serviceName}, error code: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress
                val port = serviceInfo.port

                if (host != null && port > 0) {
                    // Determine protocol based on service type or port
                    val protocol = when {
                        serviceInfo.serviceType.contains("https", ignoreCase = true) -> "https"
                        port == 443 -> "https"
                        else -> "http"
                    }

                    val deviceUrl = "$protocol://$host:$port"
                    Timber.d("Resolved device URL: $deviceUrl")
                    onServiceResolved(deviceUrl)
                } else {
                    Timber.w("Service resolved but host or port is invalid: host=$host, port=$port")
                }
            }
        })
    }
}