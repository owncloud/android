package com.owncloud.android.domain.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.domain.device.usecases.DynamicUrlSwitchingController
import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAvailableDevicesUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.IOException

/**
 * Worker responsible for updating base URLs by combining mDNS discovery
 * and remote access devices, then triggering dynamic URL switching.
 */
class BaseUrlUpdateWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val discoverLocalNetworkDevicesUseCase: DiscoverLocalNetworkDevicesUseCase by inject()
    private val getRemoteAvailableDevicesUseCase: GetRemoteAvailableDevicesUseCase by inject()
    private val saveCurrentDeviceUseCase: SaveCurrentDeviceUseCase by inject()
    private val dynamicUrlSwitchingController: DynamicUrlSwitchingController by inject()

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting base URL update worker")

            val localDevice = discoverLocalNetworkDevicesUseCase.oneShot(
                DiscoverLocalNetworkDevicesUseCase.DEFAULT_MDNS_PARAMS
            )
            Timber.d("Local mDNS device discovered: $localDevice")

            if (localDevice != null) {
                saveCurrentDeviceUseCase(localDevice)
            } else {
                val remoteCurrentDevice = getRemoteAvailableDevicesUseCase.currentDevice()
                Timber.d("Remote devices received: $remoteCurrentDevice")
                if (remoteCurrentDevice != null) {
                    saveCurrentDeviceUseCase(remoteCurrentDevice)
                } else {
                    Timber.d("No device to update, skipping URL switching")
                }
            }

            dynamicUrlSwitchingController.oneShotDynamicUrlSwitching()
            Timber.d("Base URL update completed successfully")
            Result.success()
        } catch (e: IOException) {
            Timber.e(e, "Base URL update worker failed with ${e.message}")
            Result.retry()
        } catch (e: Exception) {
            Timber.e(e, "Base URL update worker failed ${e.message}")
            Result.failure()
        }
    }

    companion object {
        const val BASE_URL_UPDATE_WORKER = "BASE_URL_UPDATE_WORKER"
    }
}