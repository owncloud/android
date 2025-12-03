package com.owncloud.android.domain.mdnsdiscovery.usecases

import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.mdnsdiscovery.MdnsDiscoveryRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Use case for discovering and verifying devices via mDNS
 */
class DiscoverLocalNetworkDevicesUseCase(
    private val mdnsDiscoveryRepository: MdnsDiscoveryRepository
) {
    
    fun execute(params: Params = DEFAULT_MDNS_PARAMS): Flow<Device> =
        mdnsDiscoveryRepository.discoverAndVerifyDevices(
            serviceType = params.serviceType,
            serviceName = params.serviceName,
            duration = params.duration
        )

    suspend fun oneShot(params: Params = DEFAULT_MDNS_PARAMS): Device? =
        mdnsDiscoveryRepository.discoverAndVerifyDevice(
            serviceType = params.serviceType,
            serviceName = params.serviceName,
            duration = params.duration
        )

    data class Params(
        val serviceType: String,
        val serviceName: String,
        val duration: Duration
    )

    companion object {

        val DEFAULT_MDNS_PARAMS = Params(
            serviceType = "_https._tcp",
            serviceName = "HomeCloud",
            duration = 10.seconds
        )
    }
}
