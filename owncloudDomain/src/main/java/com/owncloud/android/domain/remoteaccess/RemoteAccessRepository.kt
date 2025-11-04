package com.owncloud.android.domain.remoteaccess

import com.owncloud.android.domain.server.model.Server

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
     * Get all available server urls for all devices
     * @return a list of available servers
     */
    suspend fun getAvailableServers(): List<Server>
}

