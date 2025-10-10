package com.owncloud.android.data.remoteaccess.repository

import com.owncloud.android.data.remoteaccess.RemoteAccessModelMapper
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessInitiateRequest
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessTokenRequest
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessDevice
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPath
import kotlinx.coroutines.runBlocking

class HCRemoteAccessRepository(
    private val remoteAccessService: RemoteAccessService,
    private val tokenStorage: RemoteAccessTokenStorage
) : RemoteAccessRepository {

    override fun initiateAuthentication(
        email: String,
        clientId: String,
        clientFriendlyName: String
    ): String {
        val request = RemoteAccessInitiateRequest(
            email = email,
            clientId = clientId,
            clientFriendlyName = clientFriendlyName
        )
        val response = wrapNetworkCall {
            remoteAccessService.initiateAuthentication(request = request)
        }

        return response.reference
    }

    override fun getToken(reference: String, code: String) {
        val request = RemoteAccessTokenRequest(
            reference = reference,
            code = code
        )
        val response = wrapNetworkCall { remoteAccessService.getToken(request = request) }

        tokenStorage.saveToken(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
    }

    override fun getDevices(): List<RemoteAccessDevice> {
        val response = wrapNetworkCall { remoteAccessService.getDevices() }

        return response.map { RemoteAccessModelMapper.toModel(it) }
    }

    override fun getDeviceById(deviceId: String): List<RemoteAccessPath> {
        val response = wrapNetworkCall { remoteAccessService.getDeviceById(deviceId) }

        return response.paths.map { RemoteAccessModelMapper.toModel(it) }
    }

    private fun <T> wrapNetworkCall(networkCall: suspend () -> T) : T {
        return runBlocking {
            networkCall()
        }
    }
}

