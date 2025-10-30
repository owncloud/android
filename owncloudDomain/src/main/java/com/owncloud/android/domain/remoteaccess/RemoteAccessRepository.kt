package com.owncloud.android.domain.remoteaccess

import com.owncloud.android.domain.remoteaccess.model.RemoteAccessDevice
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPath

interface RemoteAccessRepository {
    /**
     * Initiate client authentication
     * @param email User's email address
     * @param clientId Unique client identifier
     * @param clientFriendlyName Human-readable client name
     * @return Reference string to be used in token request
     */
    suspend fun initiateAuthentication(
        email: String,
        clientId: String,
        clientFriendlyName: String
    ): String

    /**
     * Obtain a JWT access token
     * @param reference Reference from initiate response
     * @param code Validation code received by email
     */
    suspend fun getToken(
        reference: String,
        code: String,
        userName: String
    )

    fun getUserName(): String?

    /**
     * Retrieve the list of devices a user has access to
     * @return List of devices
     */
    suspend fun getDevices(): List<RemoteAccessDevice>

    /**
     * Get information about a specific device including its connection paths
     * @param deviceId Device identifier
     * @return List of paths for partivular device
     */
    suspend fun getDeviceById(deviceId: String): List<RemoteAccessPath>
}

