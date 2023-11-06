/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2023 ownCloud GmbH.
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
package com.owncloud.android.lib.common.http.logging

import com.owncloud.android.lib.common.http.HttpConstants.AUTHORIZATION_HEADER
import com.owncloud.android.lib.common.http.HttpConstants.OC_X_REQUEST_ID
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import timber.log.Timber
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class LogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        if (!httpLogsEnabled) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val response = chain.proceed(request)
        logRequest(moshi, request)

        return response.also {
            logResponse(moshi, it, request)
        }
    }

    private fun logRequest(moshi: Moshi, request: Request) {
        val requestJsonAdapter = moshi.adapter(LogRequest::class.java)
        Timber.d(
            "HTTP REQUEST: ${
                requestJsonAdapter.toJson(
                    LogRequest(
                        requestId = request.headers[OC_X_REQUEST_ID] ?: "",
                        method = request.method,
                        url = request.url.toString(),
                        headers = logHeaders(request.headers),
                        bodyLength = request.body?.contentLength() ?: 0L,
                        bodyContentType = request.body?.contentType()?.toString() ?: "",
                        body = getRequestBodyString(request.body),
                    )
                )
            }"

        )
    }

    private fun logHeaders(headers: Headers): Map<String, String> {
        val auxHeaders = headers.toMap().toMutableMap()
        if (auxHeaders.contains(AUTHORIZATION_HEADER)) {
            auxHeaders[AUTHORIZATION_HEADER] = "[redacted]"
        }
        return auxHeaders
    }

    private fun getRequestBodyString(requestBodyParam: RequestBody?): String {
        requestBodyParam?.let { requestBody ->
            if (requestBody.isOneShot()) {
                return "One shot body --   Omitted"
            }

            if (requestBody.isDuplex()) {
                return "Duplex body -- Omitted"
            }

            val buffer = Buffer()
            requestBody.writeTo(buffer)

            val contentType = requestBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            if (contentType.isLoggable()) {
                return buffer.readString(charset)
            } else if (requestBody.contentLength() > 0) {
                return "$BINARY_OMITTED ${requestBody.contentLength()} $BYTES"
            }
        }
        return EMPTY_BODY
    }

    private fun logResponse(moshi: Moshi, it: Response, request: Request) {
        val responseJsonAdapter = moshi.adapter(LogResponse::class.java)
        val contentType = it.body?.contentType()
        val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
        val source = it.body?.source()
        source?.request(LIMIT_BODY_LOG)
        val buffer = source?.buffer
        val rawResponseBody = buffer?.clone()?.readString(charset)
        val bodyLength = rawResponseBody?.toByteArray(charset)?.size ?: 0
        val responseBody = getResponseBodyString(contentType, bodyLength, rawResponseBody ?: "")
        Timber.d(
            "HTTP RESPONSE: ${
                responseJsonAdapter.toJson(
                    LogResponse(
                        requestId = request.headers[OC_X_REQUEST_ID] ?: "",
                        method = request.method,
                        url = request.url.toString(),
                        code = it.code,
                        headers = logHeaders(it.headers),
                        bodyLength = bodyLength,
                        bodyContentType = contentType?.toString() ?: "",
                        body = responseBody,
                    )
                )
            }"
        )
    }

    private fun getResponseBodyString(contentType: MediaType?, contentLength: Int, responseBody: String): String {
        return if (contentType?.isLoggable() == true) {
            responseBody
        } else if (contentLength > 0) {
            "$BINARY_OMITTED $contentLength $BYTES"
        } else {
            EMPTY_BODY
        }
    }

    companion object {
        var httpLogsEnabled: Boolean = false
        private const val LIMIT_BODY_LOG: Long = 1000000
        private const val EMPTY_BODY = "Empty body"
        private const val BINARY_OMITTED = "<-- Body end for response -- Binary -- Omitted:"
        private const val BYTES = "bytes -->"
    }
}
