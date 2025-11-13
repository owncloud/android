package com.owncloud.android.data.remoteaccess.repository

import com.owncloud.android.data.device.CurrentDeviceStorage
import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessInitiateRequest
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessPath
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessTokenRequest
import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePath
import com.owncloud.android.domain.device.model.DevicePathType
import com.owncloud.android.domain.exceptions.WrongCodeException
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.lib.common.http.HttpConstants
import retrofit2.HttpException

class HCRemoteAccessRepository(
    private val remoteAccessService: RemoteAccessService,
    private val tokenStorage: RemoteAccessTokenStorage,
    private val deviceVerificationClient: HCDeviceVerificationClient,
    private val currentDeviceStorage: CurrentDeviceStorage
) : RemoteAccessRepository {

    override suspend fun initiateAuthentication(
        email: String,
        clientId: String,
        clientFriendlyName: String
    ): String {
        val request = RemoteAccessInitiateRequest(
            email = email,
            clientId = clientId,
            clientFriendlyName = clientFriendlyName
        )

        return remoteAccessService.initiateAuthentication(request = request).reference
    }

    override suspend fun getToken(reference: String, code: String, userName: String) {
        try {
            val request = RemoteAccessTokenRequest(
                reference = reference,
                code = code
            )
            val (accessToken, refreshToken) = remoteAccessService.getToken(request = request)

            tokenStorage.saveToken(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )

            tokenStorage.saveUserName(userName = userName)
        } catch (e: HttpException) {
            if (e.code() == HttpConstants.HTTP_BAD_REQUEST) {
                throw WrongCodeException(e)
            } else {
                throw e
            }
        }
    }

    override fun getUserName(): String? {
        return tokenStorage.getUserName()
    }

    override suspend fun getAvailableDevices(): List<Device> {
        return remoteAccessService.getDevices().mapNotNull { deviceResponse ->
            val remoteDevicePaths = remoteAccessService.getDeviceById(deviceResponse.seagateDeviceId).paths

            // Build all available servers for this device
            val availablePaths = mutableMapOf<DevicePathType, DevicePath>()
            var preferredDevicePath: DevicePath? = null
            var certificateCommonName = ""

            for (remoteDevicePath in remoteDevicePaths) {
                val baseUrl = getDeviceBaseUrl(remoteDevicePath)
                val baseFilesUrl = "$baseUrl/files"
                val devicePathType = remoteDevicePath.type.mapToDomain()

                // Verify device and get certificate
                val isVerified = deviceVerificationClient.verifyDevice(baseUrl)
                if (isVerified) {
                    certificateCommonName = deviceVerificationClient.getCertificateCommonName(baseUrl).orEmpty()
                }

                val devicePath = DevicePath(
                    hostName = deviceResponse.friendlyName,
                    hostUrl = baseFilesUrl,
                    devicePathType = devicePathType
                )

                availablePaths[devicePathType] = devicePath

                // Set preferred server to the first verified one
                if (preferredDevicePath == null && isVerified) {
                    preferredDevicePath = devicePath
                }
            }

            preferredDevicePath = preferredDevicePath ?: availablePaths.values.firstOrNull()

            if (availablePaths.isNotEmpty() && preferredDevicePath != null) {
                Device(
                    id = deviceResponse.seagateDeviceId,
                    availablePaths = availablePaths,
                    preferredPath = preferredDevicePath,
                    certificateCommonName = certificateCommonName
                )
            } else {
                null
            }
        }
    }

    override fun clearDevicePaths() {
        currentDeviceStorage.clearDevicePaths()
    }

    private fun getDeviceBaseUrl(remoteAccessPath: RemoteAccessPath): String {
        val address = remoteAccessPath.address
        val port = if (remoteAccessPath.port == null) "" else ":${remoteAccessPath.port}"
        return "https://${address}${port}"
    }
}

