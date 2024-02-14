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
 */
package com.owncloud.android.lib.common.http.logging

import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_JRD_JSON
import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_JSON
import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_WWW_FORM
import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_XML
import okhttp3.MediaType

/**
 * Check whether a media type is loggable.
 *
 * @return true if its type is text, xml, json, jrd+json or x-www-form-urlencoded.
 */
fun MediaType?.isLoggable(): Boolean =
    this?.let { mediaType ->
        val mediaTypeString = mediaType.toString()
        (mediaType.type == "text" ||
                mediaTypeString.contains(CONTENT_TYPE_XML) ||
                mediaTypeString.contains(CONTENT_TYPE_JSON) ||
                mediaTypeString.contains(CONTENT_TYPE_WWW_FORM) ||
                mediaTypeString.contains(CONTENT_TYPE_JRD_JSON))
    } ?: false
