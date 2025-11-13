package com.owncloud.android.data.mdnsdiscovery.repository

import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.data.mdnsdiscovery.datasources.LocalMdnsDiscoveryDataSource
import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePath
import com.owncloud.android.domain.device.model.DevicePathType
import com.owncloud.android.domain.mdnsdiscovery.MdnsDiscoveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber
import kotlin.time.Duration

/**
 * Implementation of MdnsDiscoveryRepository
 *
 * This repository discovers devices via mDNS and verifies each discovered device
 * by calling the proper endpoint. Only verified devices are emitted.
 */
class HCMdnsDiscoveryRepository(
    private val localMdnsDiscoveryDataSource: LocalMdnsDiscoveryDataSource,
    private val deviceVerificationClient: HCDeviceVerificationClient,
) : MdnsDiscoveryRepository {

    override fun discoverAndVerifyDevices(
        serviceType: String,
        serviceName: String,
        duration: Duration
    ): Flow<Device> {
        Timber.d("Starting mDNS discovery with verification - serviceType: $serviceType, serviceName: $serviceName, duration: $duration")

        return localMdnsDiscoveryDataSource.discoverDevices(
            serviceType = serviceType,
            serviceName = serviceName,
            duration = duration
        ).mapNotNull { deviceUrl ->
            // Verify each discovered device independently
            Timber.d("Device discovered via mDNS: $deviceUrl - verifying...")

            val isVerified = deviceVerificationClient.verifyDevice(deviceUrl)

            if (isVerified) {
                Timber.d("Device verified: $deviceUrl")

                // Get certificate common name
                val certificateCommonName = deviceVerificationClient.getCertificateCommonName(deviceUrl).orEmpty()
                Timber.d("Device certificate common name: $certificateCommonName")

                val devicePath = DevicePath(
                    hostName = deviceUrl,
                    hostUrl = deviceUrl,
                    devicePathType = DevicePathType.LOCAL
                )

                Device(
                    id = deviceUrl,
                    availablePaths = mapOf(
                        DevicePathType.LOCAL to devicePath
                    ),
                    preferredPath = devicePath,
                    certificateCommonName = certificateCommonName
                )
            } else {
                Timber.d("Device verification failed, skipping: $deviceUrl")
                null
            }
        }
    }
}

