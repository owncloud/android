package com.owncloud.android.lib.resources.status

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

internal class StatusRequestor {

    companion object {
        /**
         * Maximum time to wait for a response from the server when the connection is being tested,
         * in MILLISECONDs.
         */
        private const val TRY_CONNECTION_TIMEOUT: Long = 5000
        private const val NODE_INSTALLED = "installed"
        private const val HTTPS_SCHEME = "https"
        private const val HTTP_SCHEME = "http"
        private const val NODE_VERSION = "version"
    }

    private fun checkIfConnectionIsRedirectedToNoneSecure(
        isConnectionSecure: Boolean,
        baseUrl: String,
        redirectedUrl: String
    ): Boolean {
        return isConnectionSecure ||
                (baseUrl.startsWith(HTTPS_SCHEME) && redirectedUrl.startsWith(
                    HTTP_SCHEME
                ))
    }

    fun updateLocationWithRedirectPath(oldLocation: String, redirectedLocation: String): String {
        if (!redirectedLocation.startsWith("/"))
            return redirectedLocation
        val oldLocation = URL(oldLocation)
        return URL(oldLocation.protocol, oldLocation.host, oldLocation.port, redirectedLocation).toString()
    }

    private fun getGetMethod(url: String): GetMethod {
        return GetMethod(URL(url + OwnCloudClient.STATUS_PATH)).apply {
            setReadTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            setConnectionTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
        }
    }

    data class RequestResult(
        val getMethod: GetMethod,
        val status: Int,
        val result: RemoteOperationResult<OwnCloudVersion>,
        val redirectedToUnsecureLocation: Boolean
    )

    fun requestAndFollowRedirects(baseLocation: String, client: OwnCloudClient): RequestResult {
        var currentLocation = baseLocation
        var redirectedToUnsecureLocation = false
        var status: Int

        while (true) {
            val getMethod = getGetMethod(currentLocation)

            status = client.executeHttpMethod(getMethod)
            val result =
                if (isSuccess(status)) RemoteOperationResult<OwnCloudVersion>(RemoteOperationResult.ResultCode.OK)
                else RemoteOperationResult(getMethod)

            if (result.redirectedLocation.isNullOrEmpty() || result.isSuccess) {
                return RequestResult(getMethod, status, result, redirectedToUnsecureLocation)
            } else {
                val nextLocation = updateLocationWithRedirectPath(currentLocation, result.redirectedLocation)
                redirectedToUnsecureLocation =
                    checkIfConnectionIsRedirectedToNoneSecure(
                        redirectedToUnsecureLocation,
                        currentLocation,
                        nextLocation
                    )
                currentLocation = nextLocation
            }
        }
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    fun handleRequestResult(
        requestResult: RequestResult,
        baseUrl: String
    ): RemoteOperationResult<OwnCloudVersion> {
        if (!isSuccess(requestResult.status))
            return RemoteOperationResult(requestResult.getMethod)

        val respJSON = JSONObject(requestResult.getMethod.getResponseBodyAsString() ?: "")
        if (!respJSON.getBoolean(NODE_INSTALLED))
            return RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED)

        val ocVersion = OwnCloudVersion(respJSON.getString(NODE_VERSION))
        // the version object will be returned even if the version is invalid, no error code;
        // every app will decide how to act if (ocVersion.isVersionValid() == false)
        val result =
            if (requestResult.redirectedToUnsecureLocation) {
                RemoteOperationResult<OwnCloudVersion>(RemoteOperationResult.ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION)
            } else {
                if (baseUrl.startsWith(HTTPS_SCHEME)) RemoteOperationResult(
                    RemoteOperationResult.ResultCode.OK_SSL
                )
                else RemoteOperationResult(RemoteOperationResult.ResultCode.OK_NO_SSL)
            }
        result.data = ocVersion
        return result
    }
}