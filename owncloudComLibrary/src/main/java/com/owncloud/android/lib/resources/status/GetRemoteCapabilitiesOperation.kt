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
import com.owncloud.android.lib.resources.response.CommonResponse
import com.owncloud.android.lib.resources.status.RemoteCapability.CapabilityBooleanType.Companion.fromBooleanValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber
import java.lang.reflect.Type
import java.net.URL

/**
 * Get the Capabilities from the server
 * Save in Result.getData in a RemoteCapability object
 *
 * @author masensio
 * @author David González Verdugo
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

            if (status != HttpConstants.HTTP_OK) {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting capabilities from the server status code: $status; response message: $response")
                return result
            }

            Timber.d("Successful response: $response")

            // Parse the response
            val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type: Type = Types.newParameterizedType(CommonResponse::class.java, CapabilityResponse::class.java)
            val adapter: JsonAdapter<CommonResponse<CapabilityResponse>> = moshi.adapter(type)
            val commonResponse: CommonResponse<CapabilityResponse>? = adapter.fromJson(response)

            // Read MetaData
            val statusMessage = commonResponse?.ocs?.meta?.status
            val statusCode = commonResponse?.ocs?.meta?.statuscode
            val message = commonResponse?.ocs?.meta?.message

            if (statusMessage.equals(PROPERTY_STATUS_OK, ignoreCase = true)) {
                val remoteCapability = commonResponse?.ocs?.data?.let { mapToModel(it) } ?: RemoteCapability()
                // Result
                result = RemoteOperationResult(OK)
                result.data = remoteCapability

                Timber.d("*** Get Capabilities completed and parsed to: $remoteCapability")
            } else {
                result = RemoteOperationResult(statusCode!!, message, null)
                Timber.e("Failed response while getting capabilities from the server ")
                Timber.e("*** statusCode: $status; status: $statusMessage; message: $message")
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting capabilities")
        }

        return result
    }

    private fun mapToModel(capabilityResponse: CapabilityResponse): RemoteCapability =
        with(capabilityResponse) {
            RemoteCapability(
                versionMayor = serverVersion.major,
                versionMinor = serverVersion.minor,
                versionMicro = serverVersion.micro,
                versionString = serverVersion.string,
                versionEdition = serverVersion.edition,
                corePollinterval = capabilities.coreCapabilities.pollinterval,
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
                filesBigFileChunking = fromBooleanValue(capabilities.filesCapabilities.bigfilechunking),
                filesUndelete = fromBooleanValue(capabilities.filesCapabilities.undelete),
                filesVersioning = fromBooleanValue(capabilities.filesCapabilities.versioning),
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

        private const val PROPERTY_STATUS_OK = "ok"
    }
}
