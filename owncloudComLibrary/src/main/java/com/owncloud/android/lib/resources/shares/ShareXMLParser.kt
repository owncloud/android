/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.shares

import android.util.Xml

import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.resources.files.FileUtils

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

/**
 * Parser for Share API Response
 *
 * @author masensio
 * @author David González Verdugo
 */

class ShareXMLParser {
    // Getters and Setters
    var status: String? = null
    var statusCode: Int = 0
    var message: String? = null

    val isSuccess: Boolean
        get() = statusCode == SUCCESS

    val isForbidden: Boolean
        get() = statusCode == ERROR_FORBIDDEN

    val isNotFound: Boolean
        get() = statusCode == ERROR_NOT_FOUND

    val isWrongParameter: Boolean
        get() = statusCode == ERROR_WRONG_PARAMETER

    // Constructor
    init {
        statusCode = INIT
    }

    /**
     * Parse is as response of Share API
     * @param inputStream
     * @return List of ShareRemoteFiles
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseXMLResponse(inputStream: InputStream): ArrayList<RemoteShare> {

        try {
            // XMLPullParser
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readOCS(parser)

        } finally {
            inputStream.close()
        }
    }

    /**
     * Parse OCS node
     * @param parser
     * @return List of ShareRemoteFiles
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readOCS(parser: XmlPullParser): ArrayList<RemoteShare> {
        var shares = ArrayList<RemoteShare>()
        parser.require(XmlPullParser.START_TAG, ns, NODE_OCS)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            // read NODE_META and NODE_DATA
            if (name.equals(NODE_META, ignoreCase = true)) {
                readMeta(parser)
            } else if (name.equals(NODE_DATA, ignoreCase = true)) {
                shares = readData(parser)
            } else {
                skip(parser)
            }
        }
        return shares
    }

    /**
     * Parse Meta node
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readMeta(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns, NODE_META)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name

            if (name.equals(NODE_STATUS, ignoreCase = true)) {
                status = readNode(parser, NODE_STATUS)

            } else if (name.equals(NODE_STATUS_CODE, ignoreCase = true)) {
                statusCode = Integer.parseInt(readNode(parser, NODE_STATUS_CODE))

            } else if (name.equals(NODE_MESSAGE, ignoreCase = true)) {
                message = readNode(parser, NODE_MESSAGE)

            } else {
                skip(parser)
            }
        }
    }

    /**
     * Parse Data node
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readData(parser: XmlPullParser): ArrayList<RemoteShare> {
        val shares = ArrayList<RemoteShare>()
        var share: RemoteShare? = null

        parser.require(XmlPullParser.START_TAG, ns, NODE_DATA)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name.equals(NODE_ELEMENT, ignoreCase = true)) {
                readElement(parser, shares)

            } else if (name.equals(NODE_ID, ignoreCase = true)) {// Parse Create XML Response
                share = RemoteShare()
                val value = readNode(parser, NODE_ID)
                share.id = Integer.parseInt(value).toLong()

            } else if (name.equals(NODE_URL, ignoreCase = true)) {
                // NOTE: this field is received in all the public shares from OC 9.0.0
                // in previous versions, it's received in the result of POST requests, but not
                // in GET requests
                share!!.shareType = ShareType.PUBLIC_LINK
                val value = readNode(parser, NODE_URL)
                share.shareLink = value

            } else if (name.equals(NODE_TOKEN, ignoreCase = true)) {
                share!!.token = readNode(parser, NODE_TOKEN)

            } else {
                skip(parser)
            }
        }

        if (share != null) {
            // this is the response of a request for creation; don't pass to isValidShare()
            shares.add(share)
        }

        return shares

    }

    /**
     * Parse Element node
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readElement(parser: XmlPullParser, shares: ArrayList<RemoteShare>) {
        parser.require(XmlPullParser.START_TAG, ns, NODE_ELEMENT)

        val remoteShare = RemoteShare()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            val name = parser.name

            when {
                name.equals(NODE_ELEMENT, ignoreCase = true) -> {
                    // patch to work around servers responding with extra <element> surrounding all
                    // the shares on the same file before
                    // https://github.com/owncloud/core/issues/6992 was fixed
                    readElement(parser, shares)
                }

                name.equals(NODE_ID, ignoreCase = true) -> {
                    remoteShare.id = Integer.parseInt(readNode(parser, NODE_ID)).toLong()
                }

                name.equals(NODE_ITEM_TYPE, ignoreCase = true) -> {
                    remoteShare.isFolder = readNode(parser, NODE_ITEM_TYPE).equals(TYPE_FOLDER, ignoreCase = true)
                    fixPathForFolder(remoteShare)
                }

                name.equals(NODE_ITEM_SOURCE, ignoreCase = true) -> {
                    remoteShare.itemSource = java.lang.Long.parseLong(readNode(parser, NODE_ITEM_SOURCE))
                }

                name.equals(NODE_PARENT, ignoreCase = true) -> {
                    readNode(parser, NODE_PARENT)
                }

                name.equals(NODE_SHARE_TYPE, ignoreCase = true) -> {
                    val value = Integer.parseInt(readNode(parser, NODE_SHARE_TYPE))
                    remoteShare.shareType = ShareType.fromValue(value)
                }

                name.equals(NODE_SHARE_WITH, ignoreCase = true) -> {
                    remoteShare.shareWith = readNode(parser, NODE_SHARE_WITH)
                }

                name.equals(NODE_FILE_SOURCE, ignoreCase = true) -> {
                    remoteShare.fileSource = java.lang.Long.parseLong(readNode(parser, NODE_FILE_SOURCE))
                }

                name.equals(NODE_PATH, ignoreCase = true) -> {
                    remoteShare.path = readNode(parser, NODE_PATH)
                    fixPathForFolder(remoteShare)
                }

                name.equals(NODE_PERMISSIONS, ignoreCase = true) -> {
                    remoteShare.permissions = Integer.parseInt(readNode(parser, NODE_PERMISSIONS))
                }

                name.equals(NODE_STIME, ignoreCase = true) -> {
                    remoteShare.sharedDate = java.lang.Long.parseLong(readNode(parser, NODE_STIME))
                }

                name.equals(NODE_EXPIRATION, ignoreCase = true) -> {
                    val value = readNode(parser, NODE_EXPIRATION)
                    if (value.isNotEmpty()) {
                        remoteShare.expirationDate = WebdavUtils.parseResponseDate(value)!!.time
                    }
                }

                name.equals(NODE_TOKEN, ignoreCase = true) -> {
                    remoteShare.token = readNode(parser, NODE_TOKEN)
                }

                name.equals(NODE_STORAGE, ignoreCase = true) -> {
                    readNode(parser, NODE_STORAGE)
                }

                name.equals(NODE_MAIL_SEND, ignoreCase = true) -> {
                    readNode(parser, NODE_MAIL_SEND)
                }

                name.equals(NODE_SHARE_WITH_DISPLAY_NAME, ignoreCase = true) -> {
                    remoteShare.sharedWithDisplayName = readNode(parser, NODE_SHARE_WITH_DISPLAY_NAME)
                }

                name.equals(NODE_SHARE_WITH_ADDITIONAL_INFO, ignoreCase = true) -> {
                    remoteShare.sharedWithAdditionalInfo = readNode(parser, NODE_SHARE_WITH_ADDITIONAL_INFO)
                }

                name.equals(NODE_URL, ignoreCase = true) -> {
                    val value = readNode(parser, NODE_URL)
                    remoteShare.shareLink = value
                }

                name.equals(NODE_NAME, ignoreCase = true) -> {
                    remoteShare.name = readNode(parser, NODE_NAME)
                }

                else -> {
                    skip(parser)
                }
            }
        }

        if (remoteShare.isValid) {
            shares.add(remoteShare)
        }
    }

    private fun fixPathForFolder(share: RemoteShare) {
        if (share.isFolder && share.path.isNotEmpty() &&
            !share.path.endsWith(FileUtils.PATH_SEPARATOR)
        ) {
            share.path = share.path + FileUtils.PATH_SEPARATOR
        }
    }

    /**
     * Parse a node, to obtain its text. Needs readText method
     * @param parser
     * @param node
     * @return Text of the node
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readNode(parser: XmlPullParser, node: String): String {
        parser.require(XmlPullParser.START_TAG, ns, node)
        val value = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, node)
        return value
    }

    /**
     * Read the text from a node
     * @param parser
     * @return Text of the node
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    /**
     * Skip tags in parser procedure
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    companion object {

        // No namespaces
        private val ns: String? = null

        // NODES for XML Parser
        private const val NODE_OCS = "ocs"

        private const val NODE_META = "meta"
        private const val NODE_STATUS = "status"
        private const val NODE_STATUS_CODE = "statuscode"
        private const val NODE_MESSAGE = "message"

        private const val NODE_DATA = "data"
        private const val NODE_ELEMENT = "element"
        private const val NODE_ID = "id"
        private const val NODE_ITEM_TYPE = "item_type"
        private const val NODE_ITEM_SOURCE = "item_source"
        private const val NODE_PARENT = "parent"
        private const val NODE_SHARE_TYPE = "share_type"
        private const val NODE_SHARE_WITH = "share_with"
        private const val NODE_FILE_SOURCE = "file_source"
        private const val NODE_PATH = "path"
        private const val NODE_PERMISSIONS = "permissions"
        private const val NODE_STIME = "stime"
        private const val NODE_EXPIRATION = "expiration"
        private const val NODE_TOKEN = "token"
        private const val NODE_STORAGE = "storage"
        private const val NODE_MAIL_SEND = "mail_send"
        private const val NODE_SHARE_WITH_DISPLAY_NAME = "share_with_displayname"
        private const val NODE_SHARE_WITH_ADDITIONAL_INFO = "share_with_additional_info"
        private const val NODE_NAME = "name"

        private const val NODE_URL = "url"

        private const val TYPE_FOLDER = "folder"

        private const val SUCCESS = 200
        private const val ERROR_WRONG_PARAMETER = 400
        private const val ERROR_FORBIDDEN = 403
        private const val ERROR_NOT_FOUND = 404
        private const val INIT = -1
    }
}
