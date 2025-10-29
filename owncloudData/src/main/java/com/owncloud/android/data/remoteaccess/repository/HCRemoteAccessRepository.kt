package com.owncloud.android.data.remoteaccess.repository

import com.owncloud.android.data.remoteaccess.RemoteAccessModelMapper
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessInitiateRequest
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessTokenRequest
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessDevice
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPath

class HCRemoteAccessRepository(
    private val remoteAccessService: RemoteAccessService,
    private val tokenStorage: RemoteAccessTokenStorage
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

    override suspend fun getToken(reference: String, code: String) {
        val request = RemoteAccessTokenRequest(
            reference = reference,
            code = code
        )
        val (accessToken, refreshToken) = remoteAccessService.getToken(request = request)

        tokenStorage.saveToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    override suspend fun getDevices(): List<RemoteAccessDevice> {
        return remoteAccessService.getDevices().map { RemoteAccessModelMapper.toModel(it) }
    }

    override suspend fun getDeviceById(deviceId: String): List<RemoteAccessPath> {
        return remoteAccessService.getDeviceById(deviceId).paths.map { RemoteAccessModelMapper.toModel(it) }
    }

}

