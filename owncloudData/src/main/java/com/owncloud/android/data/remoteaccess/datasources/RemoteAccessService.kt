package com.owncloud.android.data.remoteaccess.datasources

import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDevicePathsResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDeviceResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessInitiateRequest
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessInitiateResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessTokenRequest
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for Device Resolver (Remote Access) API
 */
interface RemoteAccessService {

    /**
     * Initiate client authentication
     *
     * This request allows a client application to start authenticating with the service.
     * The service will send an email to the user with a short-lived code.
     *
     * @param type Type of user identifier (only "email" is supported)
     * @param request Authentication initiation request containing email, clientId, and clientFriendlyName
     * @return Response containing an opaque reference to be used in the token request
     */
    @POST(REMOTE_ACCESS_PATH_INITIATE)
    suspend fun initiateAuthentication(
        @Query("type") type: String = "email",
        @Body request: RemoteAccessInitiateRequest
    ): RemoteAccessInitiateResponse

    /**
     * Obtain a JWT access token
     *
     * This request allows a client application to obtain a JWT access token
     * by providing the reference from the initiate response and the user code received by email.
     *
     * @param type Type of user identifier (only "email" is supported)
     * @param request Token request containing the reference and validation code
     * @return Token response containing access token, refresh token, and expiration info
     */
    @POST(REMOTE_ACCESS_PATH_TOKEN)
    suspend fun getToken(
        @Query("type") type: String = "email",
        @Body request: RemoteAccessTokenRequest
    ): RemoteAccessTokenResponse

    /**
     * Renew a JWT access token using a refresh token
     *
     * This request allows a client application to renew a JWT access token
     * if it has expired (after 10 minutes).
     *
     * @param refreshToken Refresh token from the previous token response
     * @return New token response with refreshed access token
     */
    @GET(REMOTE_ACCESS_PATH_TOKEN_REFRESH)
    suspend fun refreshToken(
        @Query("refresh_token") refreshToken: String
    ): RemoteAccessTokenResponse

    /**
     * Retrieve the list of devices a user has access to
     *
     * This request allows an authenticated client application to obtain
     * the list of devices a user has access to.
     *
     * @return List of devices with their information
     */
    @GET(REMOTE_ACCESS_PATH_DEVICES)
    suspend fun getDevices(): List<RemoteAccessDeviceResponse>

    /**
     * Get information about a specific device
     *
     * This request allows an authenticated client application to get
     * information about a device including its connection paths.
     *
     * @param deviceId Device identifier
     * @return Device paths information including available connection paths
     */
    @GET(REMOTE_ACCESS_PATH_DEVICE_PATHS)
    suspend fun getDeviceById(
        @Path("deviceID") deviceId: String
    ): RemoteAccessDevicePathsResponse
}

internal const val REMOTE_ACCESS_PATH_INITIATE = "client/v1/auth/initiate"
internal const val REMOTE_ACCESS_PATH_TOKEN = "client/v1/auth/token"
internal const val REMOTE_ACCESS_PATH_TOKEN_REFRESH = "client/v1/auth/refresh"
internal const val REMOTE_ACCESS_PATH_DEVICES = "client/v1/devices"
internal const val REMOTE_ACCESS_PATH_DEVICE_PATHS = "client/v1/devices/{deviceID}"


