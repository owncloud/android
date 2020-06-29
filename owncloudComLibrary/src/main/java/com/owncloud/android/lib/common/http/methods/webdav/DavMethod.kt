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

import at.bitfire.dav4android.Constants.log
import at.bitfire.dav4android.exception.HttpException
import at.bitfire.dav4android.exception.RedirectException
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod
import okhttp3.HttpUrl
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Wrapper to perform WebDAV (dav4android) calls
 *
 * @author David González Verdugo
 */
abstract class DavMethod protected constructor(url: URL) : HttpBaseMethod(url) {
    protected var mDavResource: OCDavResource

    init {
        val httpUrl = HttpUrl.parse(url.toString()) ?: throw MalformedURLException()
        mDavResource = OCDavResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    override fun abort() {
        mDavResource.cancelCall()
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
                if (response.body()?.contentType() != null) {
                    val responseBody = ResponseBody.create(
                        response.body()?.contentType(),
                        httpException.responseBody?:""
                    )
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
        mDavResource = OCDavResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    override fun setConnectionTimeout(
        connectionTimeout: Long,
        timeUnit: TimeUnit
    ) {
        super.setConnectionTimeout(connectionTimeout, timeUnit)
        mDavResource = OCDavResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    override fun setFollowRedirects(followRedirects: Boolean) {
        super.setFollowRedirects(followRedirects)
        mDavResource = OCDavResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    override fun setUrl(url: HttpUrl) {
        super.setUrl(url)
        mDavResource = OCDavResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    override fun setRequestHeader(name: String, value: String) {
        super.setRequestHeader(name, value)
        mDavResource = OCDavResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    fun getRetryOnConnectionFailure(): Boolean {
        return false //TODO: implement me
    }

    //////////////////////////////
    //         Getter
    //////////////////////////////
    override fun setRetryOnConnectionFailure(retryOnConnectionFailure: Boolean) {
        super.setRetryOnConnectionFailure(retryOnConnectionFailure)
        mDavResource = OCDavResource(
            okHttpClient,
            httpUrl,
            log
        )
    }

    override val isAborted: Boolean
        get() = mDavResource.isCallAborted()

}