package com.owncloud.android.data.remoteaccess.repository

import com.owncloud.android.data.remoteaccess.RemoteAccessModelMapper
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.domain.exceptions.WrongCodeException
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessInitiateRequest
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessTokenRequest
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessDevice
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPath
import com.owncloud.android.lib.common.http.HttpConstants
import retrofit2.HttpException

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

    override suspend fun getDevices(): List<RemoteAccessDevice> {
        return remoteAccessService.getDevices().map { RemoteAccessModelMapper.toModel(it) }
    }

    override suspend fun getDeviceById(deviceId: String): List<RemoteAccessPath> {
        return remoteAccessService.getDeviceById(deviceId).paths.map { RemoteAccessModelMapper.toModel(it) }
    }

}

