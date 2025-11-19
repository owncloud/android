package com.owncloud.android.domain.server.usecases

import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.exceptions.UnknownErrorException
import com.owncloud.android.domain.server.model.ServerInfo

class GetAvailableServerInfoUseCase(
    private val getServerInfoAsyncUseCase: GetServerInfoAsyncUseCase,
    private val deviceUrlResolver: DeviceUrlResolver
) {

    fun getAvailableServerInfo(
        manualUrl: String,
        enforceOIDC: Boolean,
        secureConnectionEnforced: Boolean
    ): UseCaseResult<ServerInfo> {
        return callServerInfo(manualUrl, enforceOIDC = enforceOIDC, secureConnectionEnforced = secureConnectionEnforced)
    }

    suspend fun getAvailableServerInfo(
        device: Device,
        enforceOIDC: Boolean,
        secureConnectionEnforced: Boolean
    ): UseCaseResult<ServerInfo> {
        val availableDeviceUrl = deviceUrlResolver.resolveAvailableBaseUrl(device.availablePaths)
        return if (availableDeviceUrl == null) {
            UseCaseResult.Error(UnknownErrorException())
        } else {
            callServerInfo(availableDeviceUrl, enforceOIDC = enforceOIDC, secureConnectionEnforced = secureConnectionEnforced)
        }
    }

    private fun callServerInfo(
        hostUrl: String,
        enforceOIDC: Boolean,
        secureConnectionEnforced: Boolean
    ): UseCaseResult<ServerInfo> {
        val params = GetServerInfoAsyncUseCase.Params(
            serverPath = hostUrl,
            creatingAccount = false,
            enforceOIDC = enforceOIDC,
            secureConnectionEnforced = secureConnectionEnforced,
        )
        return getServerInfoAsyncUseCase(params)
    }
}