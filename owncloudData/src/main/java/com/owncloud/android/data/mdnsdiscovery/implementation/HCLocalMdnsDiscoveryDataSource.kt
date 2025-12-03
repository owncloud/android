package com.owncloud.android.data.mdnsdiscovery.implementation

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.owncloud.android.data.mdnsdiscovery.datasources.LocalMdnsDiscoveryDataSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.time.Duration

class HCLocalMdnsDiscoveryDataSource(
    private val nsdManager: NsdManager?
) : LocalMdnsDiscoveryDataSource {

    override fun discoverDevices(
        serviceType: String,
        serviceName: String,
        duration: Duration
    ): Flow<String> = callbackFlow {
        if (nsdManager == null) {
            Timber.d("NsdManager is not available")
            close()
            return@callbackFlow
        }

        val onServiceFound: (NsdServiceInfo) -> Unit = { service ->
            // Filter by service name if provided
            if (serviceName.isNotEmpty() && !service.serviceName.contains(serviceName, ignoreCase = true)) {
                Timber.d("Service name doesn't match filter, skipping: ${service.serviceName}")
            } else {
                nsdManager.resolveService(
                    service = service,
                    onServiceResolved = { serviceUrl ->
                        trySend(serviceUrl)
                    }
                )
            }
        }

        var discoveryListener: NsdManager.DiscoveryListener? = null

        while (isActive) {
            try {
                discoveryListener = getDiscoveryListener(
                    doOnServiceFound = onServiceFound,
                )
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                delay(duration)
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Timber.d(e, "Error during mDNS discovery")
            }
        }

        awaitClose {
            Timber.d("Closing mDNS discovery flow")
            discoveryListener?.let { listener ->
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: Exception) {
                    Timber.d("Error stopping discovery in awaitClose: ${e.message}")
                }
            }
        }
    }

    override suspend fun discoverDevicesOneShot(
        serviceType: String,
        serviceName: String,
        timeout: Duration,
    ): String? {
        return withTimeoutOrNull(timeout) {
            getDeviceBaseUrl(serviceName, serviceType)
        }
    }

    private suspend fun getDeviceBaseUrl(serviceName: String, serviceType: String): String = suspendCancellableCoroutine { continuation ->
        var discoveryListener: NsdManager.DiscoveryListener? = null
        val onServiceFound: (NsdServiceInfo) -> Unit = { service ->
            // Filter by service name if provided
            if (serviceName.isNotEmpty() && !service.serviceName.contains(serviceName, ignoreCase = true)) {
                Timber.d("Service name doesn't match filter, skipping: ${service.serviceName}")
            } else {
                nsdManager?.resolveService(
                    service = service,
                    onServiceResolved = { serviceUrl ->
                        discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
                        continuation.resume(serviceUrl)
                    }
                )
            }
        }

        discoveryListener = getDiscoveryListener(
            doOnServiceFound = onServiceFound,
        )

        continuation.invokeOnCancellation {
            nsdManager?.stopServiceDiscovery(discoveryListener)
        }

        nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun getDiscoveryListener(
        doOnServiceFound: (NsdServiceInfo) -> Unit,
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
                    val deviceUrl = "https://$host:$port"
                    Timber.d("Resolved device URL: $deviceUrl")
                    onServiceResolved(deviceUrl)
                } else {
                    Timber.w("Service resolved but host or port is invalid: host=$host, port=$port")
                }
            }
        })
    }
}