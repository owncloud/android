package com.owncloud.android.domain.server.usecases

import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePathType
import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAvailableDevicesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

class GetAvailableDevicesUseCase(
    private val getRemoteAvailableDevicesUseCase: GetRemoteAvailableDevicesUseCase,
    private val discoverLocalNetworkDevicesUseCase: DiscoverLocalNetworkDevicesUseCase,
) {

    companion object {
        private const val NO_EXIST_INDEX = -1
    }

    private val remoteAccessDevicesFlow = MutableStateFlow(emptyList<Device>())

    suspend fun refreshRemoteAccessDevices() {
        val remoteAccessDevices = getRemoteAvailableDevicesUseCase.execute()
        remoteAccessDevicesFlow.update { remoteAccessDevices }
    }

    fun getServersUpdates(
        scope: CoroutineScope,
        discoverLocalNetworkDevicesParams: DiscoverLocalNetworkDevicesUseCase.Params
    ): StateFlow<List<Device>> {
        val localNetworkDevicesFlow = discoverLocalNetworkDevicesUseCase.execute(discoverLocalNetworkDevicesParams)
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

        return combine(remoteAccessDevicesFlow, localNetworkDevicesFlow) { remoteDevices, localDevice ->
            Timber.d("Remote access devices: $remoteDevices, Local network server: $localDevice")

            val mutableDevices = remoteDevices.toMutableList()

            // If we have a local network discovery server, try to merge it with existing devices
            if (localDevice != null) {
                val localCertificate = localDevice.certificateCommonName

                // Try to find an existing device with the same certificate
                val existingDeviceIndex = if (localCertificate.isNotEmpty()) {
                    mutableDevices.indexOfFirst { device ->
                        device.certificateCommonName == localCertificate
                    }
                } else {
                    NO_EXIST_INDEX
                }

                if (existingDeviceIndex != NO_EXIST_INDEX) {
                    val existingDevice = mutableDevices[existingDeviceIndex]
                    val updatedPaths = existingDevice.availablePaths.toMutableMap()

                    if (!updatedPaths.containsKey(DevicePathType.LOCAL)) {
                        val localDevicePath = localDevice.availablePaths[DevicePathType.LOCAL]
                        if (localDevicePath != null) {
                            updatedPaths[DevicePathType.LOCAL] = localDevicePath

                            mutableDevices[existingDeviceIndex] = Device(
                                id = existingDevice.id,
                                name = localDevice.name,
                                availablePaths = updatedPaths,
                                certificateCommonName = existingDevice.certificateCommonName
                            )
                        }
                    }
                } else {
                    mutableDevices.add(localDevice)
                }
            }
            mutableDevices
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
}