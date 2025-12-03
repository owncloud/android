package com.owncloud.android.data.remoteaccess.repository

import com.owncloud.android.data.device.CurrentDeviceStorage
import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDeviceResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessInitiateRequest
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessTokenRequest
import com.owncloud.android.domain.device.model.Device
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
            getVerifiedDevice(deviceResponse)
        }
    }

    private suspend fun getVerifiedDevice(deviceResponse: RemoteAccessDeviceResponse): Device? {
        val remoteDevicePaths = remoteAccessService.getDeviceById(deviceResponse.seagateDeviceId).paths

        val availablePaths = mutableMapOf<DevicePathType, String>()
        var certificateCommonName = ""

        for (remoteDevicePath in remoteDevicePaths) {
            val devicePathType = remoteDevicePath.type.mapToDomain()
            val deviceFilesUrl = remoteDevicePath.getDeviceBaseUrl()
            val baseUrl = deviceFilesUrl.removeSuffix("/files")
            if (certificateCommonName.isEmpty()) {
                certificateCommonName = deviceVerificationClient.getCertificateCommonName(baseUrl).orEmpty()
            }

            availablePaths[devicePathType] = deviceFilesUrl
        }

        return if (availablePaths.isNotEmpty()) {
            Device(
                id = deviceResponse.seagateDeviceId,
                name = deviceResponse.friendlyName,
                availablePaths = availablePaths,
                certificateCommonName = certificateCommonName
            )
        } else {
            null
        }
    }

    override suspend fun getCurrentDevice(): Device? {
        val deviceResponse = remoteAccessService.getDevices().firstOrNull { deviceResponse ->
            deviceResponse.certificateCommonName == currentDeviceStorage.getCertificateCommonName()
        }
        return deviceResponse?.let { getVerifiedDevice(deviceResponse = it) }
    }

    override fun clearDevicePaths() {
        currentDeviceStorage.clearDevicePaths()
    }
}

