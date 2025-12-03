package com.owncloud.android.data.device

import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.domain.device.model.DevicePathType
import com.owncloud.android.domain.server.usecases.DeviceUrlResolver
import timber.log.Timber

/**
 * Resolves the best available device base URL by checking URLs in priority order.
 *
 * This class encapsulates the logic for:
 * - Verifying device availability via HCDeviceVerificationClient
 * - Selecting the first available URL based on priority (LOCAL > PUBLIC > REMOTE)
 *
 * Can be reused in any context where device URL resolution is needed.
 * Does not depend on any storage - receives URLs as input parameters.
 *
 */
class HCDeviceUrlResolver(
    private val deviceVerificationClient: HCDeviceVerificationClient,
): DeviceUrlResolver {

    /**
     * Resolves the best available base URL by checking each URL in priority order.
     *
     * The input list should be ordered by priority (e.g., LOCAL > PUBLIC > REMOTE).
     * The method will return the first URL that passes verification.
     *
     * @param devicePaths List of device path type and URL pairs, ordered by priority
     * @return The first available base URL, or null if none are available
     */
    override suspend fun resolveAvailableBaseUrl(devicePaths: Map<DevicePathType, String>): String? {
        if (devicePaths.isEmpty()) {
            Timber.d("DeviceUrlResolver: No device paths provided")
            return null
        }

        val priorityTypeOrder = listOf(
            DevicePathType.LOCAL,
            DevicePathType.PUBLIC,
            DevicePathType.REMOTE
        )

        for (priorityType in priorityTypeOrder) {
            val baseUrl = devicePaths[priorityType] ?: continue
            Timber.d("DeviceUrlResolver: Checking availability of $priorityType: $baseUrl")

            // Remove /files suffix if present for verification
            val verificationUrl = baseUrl.removeSuffix("/files")
            val isAvailable = deviceVerificationClient.verifyDevice(verificationUrl)

            if (isAvailable) {
                Timber.d("DeviceUrlResolver: Found available base URL: $baseUrl ($priorityType)")
                return baseUrl
            } else {
                Timber.d("DeviceUrlResolver: Base URL $baseUrl ($priorityType) is not available")
            }
        }

        Timber.d("DeviceUrlResolver: No available base URLs found")
        return null
    }
}

