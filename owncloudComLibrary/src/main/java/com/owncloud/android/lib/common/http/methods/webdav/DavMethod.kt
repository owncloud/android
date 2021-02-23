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
package com.owncloud.android.lib.common.http.methods.webdav

import at.bitfire.dav4jvm.Dav4jvm.log
import at.bitfire.dav4jvm.DavOCResource
import at.bitfire.dav4jvm.exception.HttpException
import at.bitfire.dav4jvm.exception.RedirectException
import com.owncloud.android.lib.common.http.HttpClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Wrapper to perform WebDAV (dav4android) calls
 *
 * @author David González Verdugo
 */
abstract class DavMethod protected constructor(httpClient: HttpClient, url: URL) : HttpBaseMethod(httpClient, url) {
    protected var davResource: DavOCResource

    override lateinit var response: Response

    init {
        val httpUrl = url.toHttpUrlOrNull() ?: throw MalformedURLException()
        davResource = DavOCResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    override fun abort() {
        davResource.cancelCall()
    }

    @Throws(Exception::class)
    override fun execute(): Int {
        return try {
            onExecute()
        } catch (httpException: HttpException) {
            // Modify responses with information gathered from exceptions
            if (httpException is RedirectException) {
                response = Response.Builder()
                    .header(
                        HttpConstants.LOCATION_HEADER, httpException.redirectLocation
                    )
                    .code(httpException.code)
                    .request(request)
                    .message(httpException.message ?: "")
                    .protocol(Protocol.HTTP_1_1)
                    .build()
            } else {
                // The check below should be included in okhttp library, method ResponseBody.create(
                // TODO check most recent versions of okhttp to see if this is already fixed and try to update if so
                if (response.body?.contentType() != null) {
                    val responseBody = (httpException.responseBody ?: "").toResponseBody(response.body?.contentType())
                    response = response.newBuilder()
                        .body(responseBody)
                        .build()
                }
            }
            httpException.code
        }
    }

    //////////////////////////////
    //         Setter
    //////////////////////////////
    // Connection parameters
    override fun setReadTimeout(readTimeout: Long, timeUnit: TimeUnit) {
        super.setReadTimeout(readTimeout, timeUnit)
        davResource = DavOCResource(
            okHttpClient,
            request.url,
            log
        )
    }

    override fun setConnectionTimeout(
        connectionTimeout: Long,
        timeUnit: TimeUnit
    ) {
        super.setConnectionTimeout(connectionTimeout, timeUnit)
        davResource = DavOCResource(
            okHttpClient,
            request.url,
            log
        )
    }

    override fun setFollowRedirects(followRedirects: Boolean) {
        super.setFollowRedirects(followRedirects)
        davResource = DavOCResource(
            okHttpClient,
            request.url,
            log
        )
    }

    override fun setUrl(url: HttpUrl) {
        super.setUrl(url)
        davResource = DavOCResource(
            okHttpClient,
            request.url,
            log
        )
    }

    override fun setRequestHeader(name: String, value: String) {
        super.setRequestHeader(name, value)
        davResource = DavOCResource(
            okHttpClient,
            request.url,
            log
        )
    }

    //////////////////////////////
    //         Getter
    //////////////////////////////
    override fun setRetryOnConnectionFailure(retryOnConnectionFailure: Boolean) {
        super.setRetryOnConnectionFailure(retryOnConnectionFailure)
        davResource = DavOCResource(
            okHttpClient,
            request.url,
            log
        )
    }

    override val isAborted: Boolean
        get() = davResource.isCallAborted()

}
