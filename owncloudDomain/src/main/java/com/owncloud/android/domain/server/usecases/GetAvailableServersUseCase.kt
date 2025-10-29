package com.owncloud.android.domain.server.usecases

import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAccessDeviceByIdUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAccessDevicesUseCase
import com.owncloud.android.domain.server.model.Server
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

class GetAvailableServersUseCase(
    private val getRemoteAccessDevicesUseCase: GetRemoteAccessDevicesUseCase,
    private val getRemoteAccessDeviceByIdUseCase: GetRemoteAccessDeviceByIdUseCase,
    private val discoverLocalNetworkDevicesUseCase: DiscoverLocalNetworkDevicesUseCase,
) {

    private val remoteAccessDevicesFlow = MutableStateFlow(emptyList<Server>())

    suspend fun refreshRemoteAccessDevices() {
        try {
            val remoteAccessDevices = getRemoteAccessDevicesUseCase.execute().map {
                val devicePath = getRemoteAccessDeviceByIdUseCase.execute(it.seagateDeviceId)
                val remoteAccessPath = devicePath.firstOrNull()
                val baseUrl = "${remoteAccessPath?.address.orEmpty()}:${remoteAccessPath?.port ?: ""}"
                Server(
                    hostName = it.friendlyName, hostUrl = baseUrl
                )
            }
            remoteAccessDevicesFlow.update { remoteAccessDevices }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        }
    }

    suspend fun getServersUpdates(
        scope: CoroutineScope,
        discoverLocalNetworkDevicesParams: DiscoverLocalNetworkDevicesUseCase.Params
    ): StateFlow<List<Server>> {
        refreshRemoteAccessDevices()
        val localNetworkDevicesFlow = discoverLocalNetworkDevicesUseCase.execute(discoverLocalNetworkDevicesParams)
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), "")

        return combine(remoteAccessDevicesFlow, localNetworkDevicesFlow) { remote, local ->
            Timber.d("Remote access devices: $remote, Local network devices: $local")
            val mutableServers = remote.toMutableList()
            if (local.isNotEmpty()) {
                mutableServers.add(
                    Server(hostName = local, hostUrl = local)
                )
            }
            mutableServers.distinctBy { it.hostUrl }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
}