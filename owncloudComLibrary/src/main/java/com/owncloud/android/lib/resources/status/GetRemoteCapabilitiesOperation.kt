/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author Semih Serhat Karakaya <karakayasemi@itu.edu.tr>
 *   @author David González Verdugo
 *   @author Abel García de Prada
 *
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.status

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK
import com.owncloud.android.lib.resources.response.CapabilityResponse
import com.owncloud.android.lib.resources.status.RemoteCapability.CapabilityBooleanType.Companion.fromBooleanValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.json.JSONObject
import timber.log.Timber
import java.net.URL

/**
 * Get the Capabilities from the server
 * Save in Result.getData in a RemoteCapability object
 *
 * @author masensio
 * @author David González Verdugo
 */
/**
 * Constructor
 */
class GetRemoteCapabilitiesOperation : RemoteOperation<RemoteCapability>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteCapability> {
        var result: RemoteOperationResult<RemoteCapability>

        try {
            val requestUri = client.baseUri
            val uriBuilder = requestUri.buildUpon()
            uriBuilder.appendEncodedPath(OCS_ROUTE)    // avoid starting "/" in this method
            uriBuilder.appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)

            val getMethod = GetMethod(URL(uriBuilder.build().toString()))

            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)

            val status = client.executeHttpMethod(getMethod)

            val response = getMethod.responseBodyAsString

            if (!isSuccess(status)) {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting capabilities from the server status code: $status; response message: $response")
                return result
            }

            Timber.d("Successful response: $response")

            // Parse the response
            val respJSON = JSONObject(response)
            val respOCS = respJSON.getJSONObject(NODE_OCS)
            val respMeta = respOCS.getJSONObject(NODE_META)
            val respData = respOCS.getJSONObject(NODE_DATA)

            // Read meta
            val statusProp = respMeta.getString(PROPERTY_STATUS).equals(PROPERTY_STATUS_OK, ignoreCase = true)
            val statuscode = respMeta.getInt(PROPERTY_STATUSCODE)
            val message = respMeta.getString(PROPERTY_MESSAGE)

            if (statusProp) {
                val moshi: Moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<CapabilityResponse> = moshi.adapter(CapabilityResponse::class.java)
                val capabilityResponse: CapabilityResponse? = adapter.fromJson(respData.toString())

                val remoteCapability = capabilityResponse?.let { mapToModel(it) } ?: RemoteCapability()
                // Result
                result = RemoteOperationResult(OK)
                result.data = remoteCapability

                Timber.d("*** Get Capabilities completed $remoteCapability")
            } else {
                result = RemoteOperationResult(statuscode, message, null)
                Timber.e("Failed response while getting capabilities from the server ")
                Timber.e("*** status: $statusProp; message: $message")
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting capabilities")
        }

        return result
    }

    private fun isSuccess(status: Int): Boolean {
        return status == HttpConstants.HTTP_OK
    }

    private fun mapToModel(capabilityResponse: CapabilityResponse): RemoteCapability =
        with(capabilityResponse) {
            RemoteCapability(
                versionMayor = serverVersion.versionMayor,
                versionMinor = serverVersion.versionMinor,
                versionMicro = serverVersion.versionMicro,
                versionString = serverVersion.versionString,
                versionEdition = serverVersion.versionEdition,
                corePollinterval = capabilities.coreCapabilities.pollInterval,
                filesSharingApiEnabled = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingApiEnabled),
                filesSharingResharing = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingReSharing),
                filesSharingPublicEnabled = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.enabled),
                filesSharingPublicUpload = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicUpload),
                filesSharingPublicSupportsUploadOnly = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicUploadOnly),
                filesSharingPublicMultiple = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicMultiple),
                filesSharingPublicPasswordEnforced = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicPassword.enforced),
                filesSharingPublicPasswordEnforcedReadOnly = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicPassword.enforcedFor.enforcedReadOnly),
                filesSharingPublicPasswordEnforcedReadWrite = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicPassword.enforcedFor.enforcedReadWrite),
                filesSharingPublicPasswordEnforcedUploadOnly = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicPassword.enforcedFor.enforcedUploadOnly),
                filesSharingPublicExpireDateEnabled = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicExpireDate.enabled),
                filesSharingPublicExpireDateDays = capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicExpireDate.days ?: 0,
                filesSharingPublicExpireDateEnforced = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingPublic.fileSharingPublicExpireDate.enforced?:false),
                filesBigFileChunking = fromBooleanValue(capabilities.filesCapabilities.filesBigFileChunking),
                filesUndelete = fromBooleanValue(capabilities.filesCapabilities.filesUnDelete),
                filesVersioning = fromBooleanValue(capabilities.filesCapabilities.filesVersioning),
                filesSharingFederationIncoming = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingFederation.incoming),
                filesSharingFederationOutgoing = fromBooleanValue(capabilities.fileSharingCapabilities.fileSharingFederation.outgoing)
            )
        }

    companion object {

        // OCS Routes
        private const val OCS_ROUTE = "ocs/v2.php/cloud/capabilities"

        // Arguments - names
        private const val PARAM_FORMAT = "format"

        // Arguments - constant values
        private const val VALUE_FORMAT = "json"

        // JSON Node names
        private const val NODE_OCS = "ocs"

        private const val NODE_META = "meta"

        private const val NODE_DATA = "data"

        private const val PROPERTY_STATUS = "status"
        private const val PROPERTY_STATUS_OK = "ok"
        private const val PROPERTY_STATUSCODE = "statuscode"
        private const val PROPERTY_MESSAGE = "message"
    }
}
