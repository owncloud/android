package com.owncloud.android.lib.common.http.methods

import com.owncloud.android.lib.common.http.HttpClient
import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

abstract class HttpBaseMethod constructor(url: URL) {
    var okHttpClient: OkHttpClient
    var httpUrl: HttpUrl = url.toHttpUrlOrNull() ?: throw MalformedURLException()
    var request: Request
    var requestBody: RequestBody? = null
    abstract var response: Response

    var call: Call? = null

    init {
        okHttpClient = HttpClient.getOkHttpClient()
        request = Request.Builder()
            .url(httpUrl)
            .build()
    }

    @Throws(Exception::class)
    open fun execute(): Int {
        return onExecute()
    }

    open fun setUrl(url: HttpUrl) {
        request = request.newBuilder()
            .url(url)
            .build()
    }

    /****************
     *** Requests ***
     ****************/

    // Headers
    val requestHeaders: Headers
        get() = request.headers

    fun getRequestHeader(name: String): String? {
        return request.header(name)
    }

    fun getRequestHeadersAsHashMap(): HashMap<String, String?> {
        val headers: HashMap<String, String?> = HashMap()
        val superHeaders: Set<String> = requestHeaders.names()
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
    fun getResponseBodyAsString(): String? = response.body?.string()

    open fun getResponseBodyAsStream(): InputStream? {
        return response.body?.byteStream()
    }

    /*************************
     *** Connection Params ***
     *************************/

    //////////////////////////////
    //         Setter
    //////////////////////////////
    // Connection parameters
    open fun setRetryOnConnectionFailure(retryOnConnectionFailure: Boolean) {
        okHttpClient = okHttpClient.newBuilder()
            .retryOnConnectionFailure(retryOnConnectionFailure)
            .build()
    }

    open fun setReadTimeout(readTimeout: Long, timeUnit: TimeUnit) {
        okHttpClient = okHttpClient.newBuilder()
            .readTimeout(readTimeout, timeUnit)
            .build()
    }

    open fun setConnectionTimeout(
        connectionTimeout: Long,
        timeUnit: TimeUnit
    ) {
        okHttpClient = okHttpClient.newBuilder()
            .readTimeout(connectionTimeout, timeUnit)
            .build()
    }

    open fun setFollowRedirects(followRedirects: Boolean) {
        okHttpClient = okHttpClient.newBuilder()
            .followRedirects(followRedirects)
            .build()
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
    protected abstract fun onExecute(): Int
}
