/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.lib.resources.status;

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Get the Capabilities from the server
 *
 * Save in Result.getData in a OCCapability object
 */
public class GetRemoteCapabilitiesOperation extends RemoteOperation {

    private static final String TAG = GetRemoteCapabilitiesOperation.class.getSimpleName();


    // OCS Routes
    private static final String OCS_ROUTE = "ocs/v1.php/cloud/capabilities";

    // Arguments - names
    private static final String PARAM_FORMAT = "format";

    // Arguments - constant values
    private static final String VALUE_FORMAT = "json";

    // JSON Node names
    private static final String NODE_OCS = "ocs";

    private static final String NODE_META = "meta";

    private static final String NODE_DATA = "data";
    private static final String NODE_VERSION = "version";

    private static final String NODE_CAPABILITIES = "capabilities";
    private static final String NODE_CORE = "core";

    private static final String NODE_FILES_SHARING = "files_sharing";
    private static final String NODE_PUBLIC = "public";
    private static final String NODE_PASSWORD = "password";
    private static final String NODE_EXPIRE_DATE = "expire_date";
    private static final String NODE_USER = "user";
    private static final String NODE_FEDERATION = "federation";
    private static final String NODE_FILES = "files";

    private static final String PROPERTY_STATUS = "status";
    private static final String PROPERTY_STATUSCODE = "statuscode";
    private static final String PROPERTY_MESSAGE = "message";

    private static final String PROPERTY_POLLINTERVAL = "pollinterval";

    private static final String PROPERTY_MAJOR = "major";
    private static final String PROPERTY_MINOR = "minor";
    private static final String PROPERTY_MICRO = "micro";
    private static final String PROPERTY_STRING = "string";
    private static final String PROPERTY_EDITION = "edition";

    private static final String PROPERTY_API_ENABLED = "api_enabled";
    private static final String PROPERTY_ENABLED = "enabled";
    private static final String PROPERTY_ENFORCED = "enforced";
    private static final String PROPERTY_DAYS = "days";
    private static final String PROPERTY_SEND_MAIL = "send_mail";
    private static final String PROPERTY_UPLOAD = "upload";
    private static final String PROPERTY_RESHARING = "resharing";
    private static final String PROPERTY_OUTGOING = "outgoing";
    private static final String PROPERTY_INCOMING = "incoming";

    private static final String PROPERTY_BIGFILECHUNKING = "bigfilechunking";
    private static final String PROPERTY_UNDELETE = "undelete";
    private static final String PROPERTY_VERSIONING = "versioning";


    /**
     * Constructor
     *
     */
    public GetRemoteCapabilitiesOperation() {

    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status;
        GetMethod get = null;

        try {
            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(OCS_ROUTE);    // avoid starting "/" in this method
            uriBuilder.appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT);

            // Get Method
            get = new GetMethod(uriBuilder.build().toString());
            get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            status = client.executeMethod(get);

            if(isSuccess(status)) {
                String response = get.getResponseBodyAsString();
                Log_OC.d(TAG, "Successful response: " + response);

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
                JSONObject respMeta = respOCS.getJSONObject(NODE_META);
                JSONObject respData = respOCS.getJSONObject(NODE_DATA);

                // Read meta
                boolean statusProp = respMeta.getString(PROPERTY_STATUS).equalsIgnoreCase("ok");
                int statuscode = respMeta.getInt(PROPERTY_STATUSCODE);
                String message = respMeta.getString(PROPERTY_MESSAGE);

                if (statusProp) {
                    ArrayList<Object> data = new ArrayList<Object>(); // For result data
                    OCCapability capability = new OCCapability();
                    // Add Version
                    if (respData.has(NODE_VERSION)) {
                        JSONObject respVersion = respData.getJSONObject(NODE_VERSION);
                        capability.setVersionMayor(respVersion.getInt(PROPERTY_MAJOR));
                        capability.setVersionMinor(respVersion.getInt(PROPERTY_MINOR));
                        capability.setVersionMicro(respVersion.getInt(PROPERTY_MICRO));
                        capability.setVersionString(respVersion.getString(PROPERTY_STRING));
                        capability.setVersionEdition(respVersion.getString(PROPERTY_EDITION));
                        Log_OC.d(TAG, "*** Added " + NODE_VERSION);
                    }

                    // Capabilities Object
                    if (respData.has(NODE_CAPABILITIES)) {
                        JSONObject respCapabilities = respData.getJSONObject(NODE_CAPABILITIES);

                        // Add Core: pollinterval
                        if (respCapabilities.has(NODE_CORE)) {
                            JSONObject respCore = respCapabilities.getJSONObject(NODE_CORE);
                            capability.setCorePollinterval(respCore.getInt(PROPERTY_POLLINTERVAL));
                            Log_OC.d(TAG, "*** Added " + NODE_CORE);
                        }

                        // Add files_sharing: public, user, resharing
                        if (respCapabilities.has(NODE_FILES_SHARING)) {
                            JSONObject respFilesSharing = respCapabilities.getJSONObject(NODE_FILES_SHARING);
                            if (respFilesSharing.has(PROPERTY_API_ENABLED)) {
                                capability.setFilesSharingApiEnabled(CapabilityBooleanType.fromBooleanValue(
                                        respFilesSharing.getBoolean(PROPERTY_API_ENABLED)));
                            }

                            if (respFilesSharing.has(NODE_PUBLIC)) {
                                JSONObject respPublic = respFilesSharing.getJSONObject(NODE_PUBLIC);
                                capability.setFilesSharingPublicEnabled(CapabilityBooleanType.fromBooleanValue(
                                        respPublic.getBoolean(PROPERTY_ENABLED)));
                                if(respPublic.has(NODE_PASSWORD)) {
                                    capability.setFilesSharingPublicPasswordEnforced(
                                            CapabilityBooleanType.fromBooleanValue(
                                            respPublic.getJSONObject(NODE_PASSWORD).getBoolean(PROPERTY_ENFORCED)));
                                }
                                if(respPublic.has(NODE_EXPIRE_DATE)){
                                    JSONObject respExpireDate = respPublic.getJSONObject(NODE_EXPIRE_DATE);
                                    capability.setFilesSharingPublicExpireDateEnabled(
                                            CapabilityBooleanType.fromBooleanValue(
                                                    respExpireDate.getBoolean(PROPERTY_ENABLED)));
                                    if (respExpireDate.has(PROPERTY_DAYS)) {
                                        capability.setFilesSharingPublicExpireDateDays(
                                                respExpireDate.getInt(PROPERTY_DAYS));
                                    }
                                    if (respExpireDate.has(PROPERTY_ENFORCED)) {
                                        capability.setFilesSharingPublicExpireDateEnforced(
                                                CapabilityBooleanType.fromBooleanValue(
                                                        respExpireDate.getBoolean(PROPERTY_ENFORCED)));
                                    }
                                }
                                if (respPublic.has(PROPERTY_UPLOAD)){
                                    capability.setFilesSharingPublicUpload(CapabilityBooleanType.fromBooleanValue(
                                            respPublic.getBoolean(PROPERTY_UPLOAD)));
                                }
                            }

                            if (respFilesSharing.has(NODE_USER)) {
                                JSONObject respUser = respFilesSharing.getJSONObject(NODE_USER);
                                capability.setFilesSharingUserSendMail(CapabilityBooleanType.fromBooleanValue(
                                        respUser.getBoolean(PROPERTY_SEND_MAIL)));
                            }

                            capability.setFilesSharingResharing(CapabilityBooleanType.fromBooleanValue(
                                    respFilesSharing.getBoolean(PROPERTY_RESHARING)));
                            if (respFilesSharing.has(NODE_FEDERATION)) {
                                JSONObject respFederation = respFilesSharing.getJSONObject(NODE_FEDERATION);
                                capability.setFilesSharingFederationOutgoing(
                                CapabilityBooleanType.fromBooleanValue(respFederation.getBoolean(PROPERTY_OUTGOING)));
                                capability.setFilesSharingFederationIncoming(CapabilityBooleanType.fromBooleanValue(
                                        respFederation.getBoolean(PROPERTY_INCOMING)));
                            }
                            Log_OC.d(TAG, "*** Added " + NODE_FILES_SHARING);
                        }


                        if (respCapabilities.has(NODE_FILES)) {
                            JSONObject respFiles = respCapabilities.getJSONObject(NODE_FILES);
                            // Add files
                            capability.setFilesBigFileChuncking(CapabilityBooleanType.fromBooleanValue(
                                    respFiles.getBoolean(PROPERTY_BIGFILECHUNKING)));
                            if (respFiles.has(PROPERTY_UNDELETE)) {
                                capability.setFilesUndelete(CapabilityBooleanType.fromBooleanValue(
                                        respFiles.getBoolean(PROPERTY_UNDELETE)));
                            }
                            capability.setFilesVersioning(CapabilityBooleanType.fromBooleanValue(
                                    respFiles.getBoolean(PROPERTY_VERSIONING)));
                            Log_OC.d(TAG, "*** Added " + NODE_FILES);
                        }
                    }
                    // Result
                    data.add(capability);
                    result = new RemoteOperationResult(true, status, get.getResponseHeaders());
                    result.setData(data);

                    Log_OC.d(TAG, "*** Get Capabilities completed ");
                } else {
                    result = new RemoteOperationResult(statusProp, statuscode, null);
                    Log_OC.e(TAG, "Failed response while getting capabilities from the server ");
                    Log_OC.e(TAG, "*** status: " + statusProp + "; message: " + message);
                }

            } else {
                result = new RemoteOperationResult(false, status, get.getResponseHeaders());
                String response = get.getResponseBodyAsString();
                Log_OC.e(TAG, "Failed response while getting capabilities from the server ");
                if (response != null) {
                    Log_OC.e(TAG, "*** status code: " + status + "; response message: " + response);
                } else {
                    Log_OC.e(TAG, "*** status code: " + status);
                }
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while getting capabilities", e);

        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
}
