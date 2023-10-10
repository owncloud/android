/* ownCloud Android Library is available under MIT license
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
package com.owncloud.android.lib.common.http

import com.owncloud.android.lib.common.http.HttpConstants.AUTHORIZATION_HEADER
import com.owncloud.android.lib.common.http.HttpConstants.OC_X_REQUEST_ID
import com.owncloud.android.lib.common.http.LogBuilder.logHttp
import com.owncloud.android.lib.common.http.NetworkNode.BODY
import com.owncloud.android.lib.common.http.NetworkNode.HEADER
import com.owncloud.android.lib.common.http.NetworkNode.INFO
import com.owncloud.android.lib.common.http.NetworkPetition.REQUEST
import com.owncloud.android.lib.common.http.NetworkPetition.RESPONSE
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.math.max

class LogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        if (!httpLogsEnabled) {
            return chain.proceed(chain.request())
        }

        val request = chain.request().also {
            val requestId = it.headers[OC_X_REQUEST_ID]
            logHttp(REQUEST, INFO, requestId, "Method: ${it.method} URL: ${it.url}")
            logHeaders(requestId, it.headers, REQUEST)
            logRequestBody(requestId, it.body)
        }

        val response = chain.proceed(request)

        return response.also {
            val requestId = it.request.headers[OC_X_REQUEST_ID]
            logHttp(
                RESPONSE,
                INFO,
                requestId,
                "Method: ${request.method} URL: ${request.url} Code: ${it.code} Message: ${it.message}"
            )
            logHeaders(requestId, it.headers, RESPONSE)
            logResponseBody(requestId, it.body)
        }
    }

    private fun logHeaders(requestId: String?, headers: Headers, networkPetition: NetworkPetition) {
        headers.forEach { header ->
            val headerValue: String = if (header.first.equals(AUTHORIZATION_HEADER, true)) {
                "[redacted]"
            } else {
                header.second
            }
            logHttp(networkPetition, HEADER, requestId, "${header.first}: $headerValue")
        }
    }

    private fun logRequestBody(requestId: String?, requestBodyParam: RequestBody?) {
        requestBodyParam?.let { requestBody ->

            if (requestBody.isOneShot()) {
                logHttp(REQUEST, BODY, requestId, "One shot body -- Omitted")
                return@let
            }

            if (requestBody.isDuplex()) {
                logHttp(REQUEST, BODY, requestId, "Duplex body -- Omitted")
                return@let
            }

            val buffer = Buffer()
            requestBody.writeTo(buffer)

            val contentType = requestBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            logHttp(REQUEST, BODY, requestId, "Length: ${requestBody.contentLength()} byte body")
            logHttp(REQUEST, BODY, requestId, "Type: ${requestBody.contentType()}")
            logHttp(REQUEST, BODY, requestId, "--> Body start for request")

            if (contentType.isLoggable()) {
                if (requestBody.contentLength() < LIMIT_BODY_LOG) {
                    logHttp(REQUEST, BODY, requestId, buffer.readString(charset))
                } else {
                    logHttp(REQUEST, BODY, requestId, buffer.readString(LIMIT_BODY_LOG, charset))
                }
                logHttp(
                    REQUEST,
                    BODY,
                    requestId,
                    "<-- Body end for request -- Omitted: ${max(0, requestBody.contentLength() - LIMIT_BODY_LOG)} bytes"
                )
            } else {
                logHttp(
                    REQUEST,
                    BODY,
                    requestId,
                    "<-- Body end for request -- Binary -- Omitted: ${requestBody.contentLength()} bytes"
                )
            }

        } ?: logHttp(REQUEST, BODY, requestId, "Empty body")
    }

    private fun logResponseBody(requestId: String?, responseBodyParam: ResponseBody?) {
        responseBodyParam?.let { responseBody ->

            val contentType = responseBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            logHttp(RESPONSE, BODY, requestId, "Length: ${responseBody.contentLength()} byte body")
            logHttp(RESPONSE, BODY, requestId, "Type: ${responseBody.contentType()}")
            logHttp(RESPONSE, BODY, requestId, "--> Body start for response")

            val source = responseBody.source()
            source.request(LIMIT_BODY_LOG)
            val buffer = source.buffer

            if (contentType.isLoggable()) {

                if (responseBody.contentLength() < LIMIT_BODY_LOG) {
                    logHttp(RESPONSE, BODY, requestId, buffer.clone().readString(charset))
                } else {
                    logHttp(RESPONSE, BODY, requestId, buffer.clone().readString(LIMIT_BODY_LOG, charset))
                }
                logHttp(
                    RESPONSE,
                    BODY,
                    requestId,
                    "<-- Body end for response -- Omitted: ${
                        max(
                            0,
                            responseBody.contentLength() - LIMIT_BODY_LOG
                        )
                    } bytes"
                )
            } else {
                logHttp(
                    RESPONSE,
                    BODY,
                    requestId,
                    "<-- Body end for response -- Binary -- Omitted: ${responseBody.contentLength()} bytes"
                )
            }
        } ?: logHttp(RESPONSE, BODY, requestId, "Empty body")
    }

    companion object {
        var httpLogsEnabled: Boolean = false
        private const val LIMIT_BODY_LOG: Long = 1024
    }
}
