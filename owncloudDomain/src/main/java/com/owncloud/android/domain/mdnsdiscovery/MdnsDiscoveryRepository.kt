package com.owncloud.android.domain.mdnsdiscovery

import com.owncloud.android.domain.device.model.Device
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Repository for discovering and verifying devices via mDNS
 */
interface MdnsDiscoveryRepository {
    
    /**
     * Discovers devices using mDNS and verifies they are alive
     *
     * This method discovers devices via mDNS and verifies each device by calling
     * the proper API endpoint. Only devices that respond with a valid status
     * will be emitted in the flow.
     *
     * @param serviceType The service type to discover (e.g., "_https._tcp")
     * @param serviceName The service name to filter by (optional, empty string to discover all)
     * @param duration How long to run discovery
     * @return Flow of verified devices as Server objects with certificate common name (empty string if not available)
     */
    fun discoverAndVerifyDevices(
        serviceType: String,
        serviceName: String,
        duration: Duration
    ): Flow<Device>
}

