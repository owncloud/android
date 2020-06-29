package com.owncloud.android.lib.common.http.methods.webdav

import at.bitfire.dav4android.DavResource
import at.bitfire.dav4android.DavResponseCallback
import at.bitfire.dav4android.IF_MATCH_HEADER
import at.bitfire.dav4android.Property
import at.bitfire.dav4android.QuotedStringUtils
import at.bitfire.dav4android.XmlUtils
import at.bitfire.dav4android.exception.DavException
import at.bitfire.dav4android.exception.HttpException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.io.StringWriter
import java.util.logging.Logger

class OCDavResource(
    httpClient: OkHttpClient,
    location: HttpUrl,
    log: Logger
) : DavResource(httpClient, location, log) {

    /**
     * Sends a PUT request to the resource.
     * @param body              new resource body to upload
     * @param ifMatchETag       value of "If-Match" header to set, or null to omit
     * @param ifNoneMatch       indicates whether "If-None-Match: *" ("don't overwrite anything existing") header shall be sent
     * @param contentType
     * @param ocTotalLength     total length of resource body
     * @param ocXOcMtimeHeader  modification time
     * @return                  true if the request was redirected successfully, i.e. #{@link #location} and maybe resource name may have changed
     * @throws IOException on I/O error
     * @throws HttpException on HTTP error
     */
    @Throws(IOException::class, HttpException::class)
    fun put(
        body: RequestBody,
        ifMatchETag: String?,
        listOfHeaders: HashMap<String, String?>?,
        callback: (response: Response) -> Unit
    ) {
        val requestBuilder = Request.Builder()
            .put(body)

        listOfHeaders?.forEach { (name, value) ->
            value?.let {
                requestBuilder.header(name, value)
            }
        }

        if (ifMatchETag != null)
        // only overwrite specific version
            requestBuilder.header(IF_MATCH_HEADER, QuotedStringUtils.asQuotedString(ifMatchETag))
//        if (contentType != null) {
//            requestBuilder.header(CONTENT_TYPE_HEADER, contentType)
//        }
//        if (ocTotalLength != null) {
//            requestBuilder.header(OC_TOTAL_LENGTH_HEADER, ocTotalLength)
//        }
//        if (ocXOcMtimeHeader != null) {
//            requestBuilder.header(OC_X_OC_MTIME_HEADER, ocXOcMtimeHeader)
//        }

        followRedirects {
            requestBuilder
                .url(location)
            val call = httpClient.newCall(requestBuilder.build())

            this.call = call
            call.execute()
        }.use { response ->
            callback(response)
            checkStatus(response)
        }
    }

    /**
     * Sends a MOVE request to the resource
     * @param ocTotalLength     total length of resource body
     * @param ocXOcMtimeHeader  modification time
     */
    @Throws(IOException::class, HttpException::class, DavException::class)
    fun move(
        destination: String,
        forceOverride: Boolean,
        listOfHeaders: HashMap<String, String?>?,
        callback: (response: Response) -> Unit
    ) {
        val requestBuilder = Request.Builder()
            .method("MOVE", null)
            .header("Content-Length", "0")
            .header("Destination", destination)

        if (forceOverride)
            requestBuilder.header("Overwrite", "F")

        listOfHeaders?.forEach { (name, value) ->
            value?.let {
                requestBuilder.header(name, value)
            }
        }
//        if (ocTotalLength != null)
//            requestBuilder.header(OC_TOTAL_LENGTH_HEADER, ocTotalLength)
//        if (ocXOcMtimeHeader != null)
//            requestBuilder.header(OC_X_OC_MTIME_HEADER, ocXOcMtimeHeader)

        followRedirects {
            requestBuilder.url(location)
            val call = httpClient.newCall(requestBuilder.build())
            this.call = call
            call.execute()
        }.use { response ->
            callback(response)
            checkStatus(response)
        }
    }

    @Throws(IOException::class, HttpException::class, DavException::class)
    fun copy(
        destination: String,
        forceOverride: Boolean,
        listOfHeaders: HashMap<String, String?>?,
        callback: (response: Response) -> Unit
    ) {
        val requestBuilder = Request.Builder()
            .method("COPY", null)
            .header("Content-Length", "0")
            .header("Destination", destination)
        if (forceOverride)
            requestBuilder.header("Overwrite", "F")

        listOfHeaders?.forEach { (name, value) ->
            value?.let {
                requestBuilder.header(name, value)
            }
        }

        followRedirects {
            requestBuilder.url(location)
            val call = httpClient.newCall(requestBuilder.build())

            this.call = call
            call.execute()
        }.use { response ->
            callback(response)
            checkStatus(response)
        }
    }

    /**
     * Sends a MKCOL request to this resource. Follows up to [MAX_REDIRECTS] redirects.
     *
     * @throws IOException on I/O error
     * @throws HttpException on HTTP error
     */
    @Throws(IOException::class, HttpException::class)
    fun mkCol(
        xmlBody: String?,
        listOfHeaders: HashMap<String, String?>?,
        callback: (response: Response) -> Unit
    ) {
        val rqBody = if (xmlBody != null) RequestBody.create(MIME_XML, xmlBody) else null
        val requestBuilder = Request.Builder()
            .method("MKCOL", rqBody)
            .url(location)
        listOfHeaders?.forEach { (name, value) ->
            value?.let {
                requestBuilder.header(name, value)
            }
        }
        followRedirects {
            val call = httpClient.newCall(
                requestBuilder.build()
            )
            this.call = call
            call.execute()
        }.use { response ->
            callback(response)
            checkStatus(response)
        }
    }

    /**
     * Sends a GET request to the resource. Sends `Accept-Encoding: identity` to disable
     * compression, because compression might change the ETag.
     *
     * Follows up to [MAX_REDIRECTS] redirects.
     *
     * @param accept   value of Accept header (must not be null, but may be *&#47;*)
     * @param callback called with server response unless an exception is thrown
     *
     * @throws IOException on I/O error
     * @throws HttpException on HTTP error
     */
    @Throws(IOException::class, HttpException::class)
    fun get(
        accept: String,
        listOfHeaders: HashMap<String, String?>?,
        callback: (response: Response) -> Unit
    ) {
        val requestBuilder = Request.Builder()
            .get()
            .url(location)
            .header("Accept", accept)
            .header("Accept-Encoding", "identity")    // disable compression because it can change the ETag

        listOfHeaders?.forEach { (name, value) ->
            value?.let {
                requestBuilder.header(name, value)
            }
        }

        followRedirects {
            val call = httpClient.newCall(
                requestBuilder
                    .build()
            )
            this.call = call
            call.execute()
        }.use { response ->
            callback(response)
            checkStatus(response)
        }
    }

    /**
     * Sends a DELETE request to the resource. Warning: Sending this request to a collection will
     * delete the collection with all its contents!
     *
     * Follows up to [MAX_REDIRECTS] redirects.
     *
     * @param ifMatchETag  value of `If-Match` header to set, or null to omit
     * @param callback     called with server response unless an exception is thrown
     *
     * @throws IOException on I/O error
     * @throws HttpException on HTTP errors, or when 207 Multi-Status is returned
     *         (because then there was probably a problem with a member resource)
     */
    @Throws(IOException::class, HttpException::class)
    fun delete(
        ifMatchETag: String?,
        listOfHeaders: HashMap<String, String?>?,
        callback: (Response) -> Unit
    ) {
        followRedirects {
            val requestBuilder = Request.Builder()
                .delete()
                .url(location)
            if (ifMatchETag != null)
                requestBuilder.header("If-Match", QuotedStringUtils.asQuotedString(ifMatchETag))
            listOfHeaders?.forEach { (name, value) ->
                value?.let {
                    requestBuilder.header(name, value)
                }
            }

            val call = httpClient.newCall(requestBuilder.build())

            this.call = call
            call.execute()
        }.use { response ->
            callback(response)
            checkStatus(response)
            if (response.code() == 207)
            /* If an error occurs deleting a member resource (a resource other than
               the resource identified in the Request-URI), then the response can be
               a 207 (Multi-Status). […] (RFC 4918 9.6.1. DELETE for Collections) */
                throw HttpException(response)
        }
    }

    /**
     * Sends a PROPFIND request to the resource. Expects and processes a 207 Multi-Status response.
     *
     * Follows up to [MAX_REDIRECTS] redirects.
     *
     * @param depth    "Depth" header to send (-1 for `infinity`)
     * @param reqProp  properties to request
     * @param callback called for every XML response element in the Multi-Status response
     *
     * @throws IOException on I/O error
     * @throws HttpException on HTTP error
     * @throws DavException on WebDAV error (like no 207 Multi-Status response)
     */
    @Throws(IOException::class, HttpException::class, DavException::class)
    fun propfind(
        depth: Int,
        vararg reqProp:
        Property.Name,
        listOfHeaders: HashMap<String, String?>?,
        callback: DavResponseCallback,
        rawCallback: (response: Response) -> Unit
    ) {
        // build XML request body
        val serializer = XmlUtils.newSerializer()
        val writer = StringWriter()
        serializer.setOutput(writer)
        serializer.setPrefix("", XmlUtils.NS_WEBDAV)
        serializer.setPrefix("CAL", XmlUtils.NS_CALDAV)
        serializer.setPrefix("CARD", XmlUtils.NS_CARDDAV)
        serializer.setPrefix("SABRE", XmlUtils.NS_SABREDAV)
        serializer.setPrefix("OC", XmlUtils.NS_OWNCLOUD)
        serializer.startDocument("UTF-8", null)
        serializer.startTag(XmlUtils.NS_WEBDAV, "propfind")
        serializer.startTag(XmlUtils.NS_WEBDAV, "prop")
        for (prop in reqProp) {
            serializer.startTag(prop.namespace, prop.name)
            serializer.endTag(prop.namespace, prop.name)
        }
        serializer.endTag(XmlUtils.NS_WEBDAV, "prop")
        serializer.endTag(XmlUtils.NS_WEBDAV, "propfind")
        serializer.endDocument()

        val requestBuilder = Request.Builder()
            .url(location)
            .method("PROPFIND", RequestBody.create(MIME_XML, writer.toString()))
            .header("Depth", if (depth >= 0) depth.toString() else "infinity")

        listOfHeaders?.forEach { (name, value) ->
            value?.let {
                requestBuilder.header(name, value)
            }
        }

        followRedirects {
            val call = httpClient.newCall(
                requestBuilder.build()
            )
            this.call = call
            call.execute()
        }.use { response ->
            rawCallback(response)
            processMultiStatus(response, callback)
        }
    }

}
