package com.owncloud.android.data.device

import com.owncloud.android.data.connectivity.NetworkStateObserver
import com.owncloud.android.domain.device.model.DevicePathType
import com.owncloud.android.domain.device.usecases.UpdateBaseUrlUseCase
import com.owncloud.android.domain.server.usecases.DeviceUrlResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Dynamically chooses the best available base URL based on network state.
 *
 * Priority order: LOCAL > PUBLIC > REMOTE
 *
 * When network state changes, attempts to find the first available base URL
 * by verifying device availability through DeviceUrlResolver.
 *
 */
class BaseUrlChooser(
    private val networkStateObserver: NetworkStateObserver,
    private val currentDeviceStorage: CurrentDeviceStorage,
    private val deviceUrlResolver: DeviceUrlResolver,
    private val updateBaseUrlUseCase: UpdateBaseUrlUseCase
) {

    /**
     * Observe the best available base URL based on network state.
     *
     * Emits a new URL whenever:
     * - Network state changes
     * - A different URL becomes available
     *
     * @return Flow emitting the currently available base URL, or null if none are available
     */
    fun observeAvailableBaseUrl(): Flow<String?> {
        return networkStateObserver.observeNetworkState()
            .map { connectivity ->
                Timber.d("BaseUrlChooser: Network state changed: $connectivity, resolving available base URL")

                if (!connectivity.hasAnyNetwork() || updateBaseUrlUseCase.hasScheduled()) {
                    Timber.d("BaseUrlChooser: No network available or base url update scheduled, returning null")
                    return@map null
                }

                chooseBestAvailableBaseUrl()
            }
            .distinctUntilChanged()
    }

    suspend fun chooseBestAvailableBaseUrl(): String? {
        val devicePaths = buildDevicePathsList()
        return deviceUrlResolver.resolveAvailableBaseUrl(devicePaths)
    }

    /**
     * Builds a list of device paths ordered by priority (LOCAL > PUBLIC > REMOTE).
     * Only includes paths that are stored in CurrentDeviceStorage.
     *
     * @return List of device path type and URL pairs
     */
    private fun buildDevicePathsList(): Map<DevicePathType, String> {
        val priorityOrder = listOf(
            DevicePathType.LOCAL,
            DevicePathType.PUBLIC,
            DevicePathType.REMOTE
        )

        return priorityOrder.mapNotNull { pathType ->
            currentDeviceStorage.getDeviceBaseUrl(pathType.name)?.let {
                pathType to it
            }
        }.associate { it.first to it.second }
    }
}

