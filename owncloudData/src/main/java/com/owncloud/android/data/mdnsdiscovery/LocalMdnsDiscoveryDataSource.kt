package com.owncloud.android.data.mdnsdiscovery

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Interface for mDNS device discovery
 */
interface LocalMdnsDiscoveryDataSource {
    /**
     * Discovers devices using mDNS/Bonjour service discovery
     *
     * @param serviceType The service type to discover (e.g., "_https._tcp")
     * @param serviceName The service name to filter by (optional, empty string to discover all)
     * @param duration How long to run discovery
     * @return Flow of device URLs as they are discovered
     */
    fun discoverDevices(
        serviceType: String,
        serviceName: String,
        duration: Duration,
    ): Flow<String>
}

