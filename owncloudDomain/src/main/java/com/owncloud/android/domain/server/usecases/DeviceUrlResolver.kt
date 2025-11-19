package com.owncloud.android.domain.server.usecases

import com.owncloud.android.domain.device.model.DevicePathType

interface DeviceUrlResolver {

    suspend fun resolveAvailableBaseUrl(devicePaths: Map<DevicePathType, String>): String?
}