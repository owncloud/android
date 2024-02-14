/* ownCloud Android Library is available under MIT license
 *
 *   @author Abel García de Prada
 *
 *   Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.lib.resources.oauth.params

import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_JSON
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ClientRegistrationParams(
    val registrationEndpoint: String,
    val clientName: String,
    val redirectUris: List<String>,
    val tokenEndpointAuthMethod: String,
    val applicationType: String
) {
    fun toRequestBody(): RequestBody =
        JSONObject().apply {
            put(PARAM_APPLICATION_TYPE, applicationType)
            put(PARAM_CLIENT_NAME, clientName)
            put(PARAM_REDIRECT_URIS, JSONArray(redirectUris))
            put(PARAM_TOKEN_ENDPOINT_AUTH_METHOD, tokenEndpointAuthMethod)
        }.toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType())

    companion object {
        private const val PARAM_APPLICATION_TYPE = "application_type"
        private const val PARAM_CLIENT_NAME = "client_name"
        private const val PARAM_TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method"
        private const val PARAM_REDIRECT_URIS = "redirect_uris"
    }
}
