/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.lib.common.http.methods

import com.owncloud.android.lib.common.http.HttpClient
import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

abstract class HttpBaseMethod constructor(url: URL) {
    var httpUrl: HttpUrl = url.toHttpUrlOrNull() ?: throw MalformedURLException()
    var request: Request
    var followPermanentRedirects = false
    abstract var response: Response
    var call: Call? = null

    var followRedirects: Boolean = true
    var retryOnConnectionFailure: Boolean = true
    var connectionTimeoutVal: Long? = null
    var connectionTimeoutUnit: TimeUnit? = null
    var readTimeoutVal: Long? = null
        private set
    var readTimeoutUnit: TimeUnit? = null
        private set

    init {
        request = Request.Builder()
            .url(httpUrl)
            .build()
    }

    @Throws(Exception::class)
    open fun execute(httpClient: HttpClient): Int {
        val okHttpClient = httpClient.okHttpClient.newBuilder().apply {
            retryOnConnectionFailure(retryOnConnectionFailure)
            followRedirects(followRedirects)
            readTimeoutUnit?.let { unit ->
                readTimeoutVal?.let { readTimeout(it, unit) }
            }
            connectionTimeoutUnit?.let { unit ->
               connectionTimeoutVal?.let { connectTimeout(it, unit) }
            }
        }.build()

        return onExecute(okHttpClient)
    }

    open fun setUrl(url: HttpUrl) {
        request = request.newBuilder()
            .url(url)
            .build()
    }

    /****************
     *** Requests ***
     ****************/

    fun getRequestHeader(name: String): String? {
        return request.header(name)
    }

    fun getRequestHeadersAsHashMap(): HashMap<String, String?> {
        val headers: HashMap<String, String?> = HashMap()
        val superHeaders: Set<String> = request.headers.names()
        superHeaders.forEach {
            headers[it] = getRequestHeader(it)
        }
        return headers
    }

    open fun addRequestHeader(name: String, value: String) {
        request = request.newBuilder()
            .addHeader(name, value)
            .build()
    }

    /**
     * Sets a header and replace it if already exists with that name
     *
     * @param name  header name
     * @param value header value
     */
    open fun setRequestHeader(name: String, value: String) {
        request = request.newBuilder()
            .header(name, value)
            .build()
    }

    /****************
     *** Response ***
     ****************/
    val statusCode: Int
        get() = response.code

    val statusMessage: String
        get() = response.message

    // Headers
    open fun getResponseHeaders(): Headers? {
        return response.headers
    }

    open fun getResponseHeader(headerName: String): String? {
        return response.header(headerName)
    }

    // Body
    fun getResponseBodyAsString(): String = response.peekBody(Long.MAX_VALUE).string()

    open fun getResponseBodyAsStream(): InputStream? {
        return response.body?.byteStream()
    }

    /**
     * returns the final url after following the last redirect.
     */
    open fun getFinalUrl() = response.request.url

    /*************************
     *** Connection Params ***
     *************************/

    //////////////////////////////
    //         Setter
    //////////////////////////////
    // Connection parameters


    open fun setReadTimeout(readTimeout: Long, timeUnit: TimeUnit) {
        readTimeoutVal = readTimeout
        readTimeoutUnit = timeUnit
    }

    open fun setConnectionTimeout(
        connectionTimeout: Long,
        timeUnit: TimeUnit
    ) {
        connectionTimeoutVal = connectionTimeout
        connectionTimeoutUnit = timeUnit
    }

    /************
     *** Call ***
     ************/
    open fun abort() {
        call?.cancel()
    }

    open val isAborted: Boolean
        get() = call?.isCanceled() ?: false

    //////////////////////////////
    //         For override
    //////////////////////////////
    @Throws(Exception::class)
    protected abstract fun onExecute(okHttpClient: OkHttpClient): Int
}
