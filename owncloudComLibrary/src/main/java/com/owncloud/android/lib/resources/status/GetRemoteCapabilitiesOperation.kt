/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author Semih Serhat Karakaya <karakayasemi@itu.edu.tr>
 *   @author David González Verdugo
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
import org.json.JSONObject
import java.net.URL
import com.owncloud.android.lib.resources.status.RemoteCapability.CapabilityBooleanType
import timber.log.Timber

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
                Timber.e("Failed response while getting capabilities from the server ")
                if (response != null) {
                    Timber.e("*** status code: $status; response message: $response")
                } else {
                    Timber.e("*** status code: $status")
                }

                return result
            }

            Timber.d("Successful response: " + response!!)

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
                val capability = RemoteCapability()
                // Add Version
                if (respData.has(NODE_VERSION)) {
                    val respVersion = respData.getJSONObject(NODE_VERSION)
                    capability.versionMayor = respVersion.getInt(PROPERTY_MAJOR)
                    capability.versionMinor = respVersion.getInt(PROPERTY_MINOR)
                    capability.versionMicro = respVersion.getInt(PROPERTY_MICRO)
                    capability.versionString = respVersion.getString(PROPERTY_STRING)
                    capability.versionEdition = respVersion.getString(PROPERTY_EDITION)
                    Timber.d("*** Added $NODE_VERSION")
                }

                // Capabilities Object
                if (respData.has(NODE_CAPABILITIES)) {
                    val respCapabilities = respData.getJSONObject(NODE_CAPABILITIES)

                    // Add Core: pollinterval
                    if (respCapabilities.has(NODE_CORE)) {
                        val respCore = respCapabilities.getJSONObject(NODE_CORE)
                        capability.corePollinterval = respCore.getInt(PROPERTY_POLLINTERVAL)
                        Timber.d("*** Added $NODE_CORE")
                    }

                    // Add files_sharing: public, user, resharing
                    if (respCapabilities.has(NODE_FILES_SHARING)) {
                        val respFilesSharing = respCapabilities.getJSONObject(NODE_FILES_SHARING)
                        if (respFilesSharing.has(PROPERTY_API_ENABLED)) {
                            capability.filesSharingApiEnabled = CapabilityBooleanType.fromBooleanValue(
                                respFilesSharing.getBoolean(PROPERTY_API_ENABLED)
                            )
                        }
                        if (respFilesSharing.has(PROPERTY_SEARCH_MIN_LENGTH)) {
                            capability.filesSharingSearchMinLength = respFilesSharing.getInt(
                                PROPERTY_SEARCH_MIN_LENGTH
                            )
                        }

                        if (respFilesSharing.has(NODE_PUBLIC)) {
                            val respPublic = respFilesSharing.getJSONObject(NODE_PUBLIC)
                            capability.filesSharingPublicEnabled = CapabilityBooleanType.fromBooleanValue(
                                respPublic.getBoolean(PROPERTY_ENABLED)
                            )

                            if (respPublic.has(NODE_PASSWORD)) {
                                val respPassword = respPublic.getJSONObject(NODE_PASSWORD)
                                capability.filesSharingPublicPasswordEnforced =
                                    CapabilityBooleanType.fromBooleanValue(
                                        respPublic.getJSONObject(NODE_PASSWORD).getBoolean(PROPERTY_ENFORCED)
                                    )

                                if (respPassword.has(NODE_ENFORCED_FOR)) {
                                    capability.filesSharingPublicPasswordEnforcedReadOnly =
                                        CapabilityBooleanType.fromBooleanValue(
                                            respPassword.getJSONObject(NODE_ENFORCED_FOR).getBoolean(
                                                PROPERTY_ENFORCED_READ_ONLY
                                            )
                                        )

                                    capability.filesSharingPublicPasswordEnforcedReadWrite =
                                        CapabilityBooleanType.fromBooleanValue(
                                            respPassword.getJSONObject(NODE_ENFORCED_FOR).getBoolean(
                                                PROPERTY_ENFORCED_READ_WRITE
                                            )
                                        )

                                    capability.filesSharingPublicPasswordEnforcedUploadOnly =
                                        CapabilityBooleanType.fromBooleanValue(
                                            respPassword.getJSONObject(NODE_ENFORCED_FOR).getBoolean(
                                                PROPERTY_ENFORCED_UPLOAD_ONLY
                                            )
                                        )
                                }
                            }
                            if (respPublic.has(NODE_EXPIRE_DATE)) {
                                val respExpireDate = respPublic.getJSONObject(NODE_EXPIRE_DATE)
                                capability.filesSharingPublicExpireDateEnabled =
                                    CapabilityBooleanType.fromBooleanValue(
                                        respExpireDate.getBoolean(PROPERTY_ENABLED)
                                    )
                                if (respExpireDate.has(PROPERTY_DAYS)) {
                                    capability.filesSharingPublicExpireDateDays =
                                        respExpireDate.getInt(PROPERTY_DAYS)
                                }
                                if (respExpireDate.has(PROPERTY_ENFORCED)) {
                                    capability.filesSharingPublicExpireDateEnforced =
                                        CapabilityBooleanType.fromBooleanValue(
                                            respExpireDate.getBoolean(PROPERTY_ENFORCED)
                                        )
                                }
                            }
                            if (respPublic.has(PROPERTY_UPLOAD)) {
                                capability.filesSharingPublicUpload = CapabilityBooleanType.fromBooleanValue(
                                    respPublic.getBoolean(PROPERTY_UPLOAD)
                                )
                            }
                            if (respPublic.has(PROPERTY_UPLOAD_ONLY)) {
                                capability.filesSharingPublicSupportsUploadOnly =
                                    CapabilityBooleanType.fromBooleanValue(
                                        respPublic.getBoolean(PROPERTY_UPLOAD_ONLY)
                                    )
                            }
                            if (respPublic.has(PROPERTY_MULTIPLE)) {
                                capability.filesSharingPublicMultiple = CapabilityBooleanType.fromBooleanValue(
                                    respPublic.getBoolean(PROPERTY_MULTIPLE)
                                )
                            }
                        }

                        if (respFilesSharing.has(NODE_USER)) {
                            val respUser = respFilesSharing.getJSONObject(NODE_USER)
                            capability.filesSharingUserSendMail = CapabilityBooleanType.fromBooleanValue(
                                respUser.getBoolean(PROPERTY_SEND_MAIL)
                            )
                        }

                        capability.filesSharingResharing = CapabilityBooleanType.fromBooleanValue(
                            respFilesSharing.getBoolean(PROPERTY_RESHARING)
                        )
                        if (respFilesSharing.has(NODE_FEDERATION)) {
                            val respFederation = respFilesSharing.getJSONObject(NODE_FEDERATION)
                            capability.filesSharingFederationOutgoing =
                                CapabilityBooleanType.fromBooleanValue(respFederation.getBoolean(PROPERTY_OUTGOING))
                            capability.filesSharingFederationIncoming = CapabilityBooleanType.fromBooleanValue(
                                respFederation.getBoolean(PROPERTY_INCOMING)
                            )
                        }
                        Timber.d("*** Added $NODE_FILES_SHARING")
                    }

                    if (respCapabilities.has(NODE_FILES)) {
                        val respFiles = respCapabilities.getJSONObject(NODE_FILES)
                        // Add files
                        capability.filesBigFileChunking = CapabilityBooleanType.fromBooleanValue(
                            respFiles.getBoolean(PROPERTY_BIGFILECHUNKING)
                        )
                        if (respFiles.has(PROPERTY_UNDELETE)) {
                            capability.filesUndelete = CapabilityBooleanType.fromBooleanValue(
                                respFiles.getBoolean(PROPERTY_UNDELETE)
                            )
                        }
                        if (respFiles.has(PROPERTY_VERSIONING)) {
                            capability.filesVersioning = CapabilityBooleanType.fromBooleanValue(
                                respFiles.getBoolean(PROPERTY_VERSIONING)
                            )
                        }
                        Timber.d("*** Added $NODE_FILES")
                    }
                }
                // Result
                result = RemoteOperationResult(OK)
                result.data = capability

                Timber.d("*** Get Capabilities completed ")
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
        private const val NODE_VERSION = "version"

        private const val NODE_CAPABILITIES = "capabilities"
        private const val NODE_CORE = "core"

        private const val NODE_FILES_SHARING = "files_sharing"
        private const val NODE_PUBLIC = "public"
        private const val NODE_PASSWORD = "password"
        private const val NODE_ENFORCED_FOR = "enforced_for"
        private const val NODE_EXPIRE_DATE = "expire_date"
        private const val NODE_USER = "user"
        private const val NODE_FEDERATION = "federation"
        private const val NODE_FILES = "files"

        private const val PROPERTY_STATUS = "status"
        private const val PROPERTY_STATUS_OK = "ok"
        private const val PROPERTY_STATUSCODE = "statuscode"
        private const val PROPERTY_MESSAGE = "message"

        private const val PROPERTY_POLLINTERVAL = "pollinterval"

        private const val PROPERTY_MAJOR = "major"
        private const val PROPERTY_MINOR = "minor"
        private const val PROPERTY_MICRO = "micro"
        private const val PROPERTY_STRING = "string"
        private const val PROPERTY_EDITION = "edition"

        private const val PROPERTY_API_ENABLED = "api_enabled"
        private const val PROPERTY_ENABLED = "enabled"
        private const val PROPERTY_ENFORCED = "enforced"
        private const val PROPERTY_ENFORCED_READ_ONLY = "read_only"
        private const val PROPERTY_ENFORCED_READ_WRITE = "read_write"
        private const val PROPERTY_ENFORCED_UPLOAD_ONLY = "upload_only"
        private const val PROPERTY_DAYS = "days"
        private const val PROPERTY_SEARCH_MIN_LENGTH = "search_min_length"
        private const val PROPERTY_SEND_MAIL = "send_mail"
        private const val PROPERTY_UPLOAD = "upload"
        private const val PROPERTY_UPLOAD_ONLY = "supports_upload_only"
        private const val PROPERTY_MULTIPLE = "multiple"
        private const val PROPERTY_RESHARING = "resharing"
        private const val PROPERTY_OUTGOING = "outgoing"
        private const val PROPERTY_INCOMING = "incoming"

        private const val PROPERTY_BIGFILECHUNKING = "bigfilechunking"
        private const val PROPERTY_UNDELETE = "undelete"
        private const val PROPERTY_VERSIONING = "versioning"
    }
}
