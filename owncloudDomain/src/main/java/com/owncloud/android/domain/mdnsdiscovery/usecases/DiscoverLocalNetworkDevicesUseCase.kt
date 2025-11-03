package com.owncloud.android.domain.mdnsdiscovery.usecases

import com.owncloud.android.domain.mdnsdiscovery.MdnsDiscoveryRepository
import com.owncloud.android.domain.server.model.Server
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Use case for discovering and verifying devices via mDNS
 */
class DiscoverLocalNetworkDevicesUseCase(
    private val mdnsDiscoveryRepository: MdnsDiscoveryRepository
) {
    
    fun execute(params: Params): Flow<Server> =
        mdnsDiscoveryRepository.discoverAndVerifyDevices(
            serviceType = params.serviceType,
            serviceName = params.serviceName,
            duration = params.duration
        )
    
    data class Params(
        val serviceType: String,
        val serviceName: String,
        val duration: Duration
    )
}
