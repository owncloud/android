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

import com.owncloud.android.lib.common.http.LogBuilder.logHttp
import com.owncloud.android.lib.common.http.NetworkNode.*
import com.owncloud.android.lib.common.http.NetworkPetition.*
import okhttp3.Interceptor
import okhttp3.Response

class LogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val response = chain.proceed(chain.request())

        return response.also {
            if (httpLogsEnabled) {
                // Log request
                logHttp(REQUEST, INFO, "Type: ${it.request.method} URL: ${it.request.url}")
                it.headers.forEach { header -> logHttp(REQUEST, HEADER, header.toString()) }
                logHttp(REQUEST, BODY, it.body.toString())

                // Log response
                logHttp(RESPONSE, INFO, "Code: ${it.code}  Message: ${it.message} IsSuccessful: ${it.isSuccessful}")
                it.headers.forEach { header -> logHttp(RESPONSE, HEADER, header.toString()) }
                logHttp(RESPONSE, BODY, it.body.toString())
            }
        }
    }

    companion object {
        var httpLogsEnabled: Boolean = false
    }
}
