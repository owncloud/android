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

package com.owncloud.android.lib.common.network;

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val contentUri: Uri
) : RequestBody() {

    override fun contentType(): MediaType? {
        val contentType = contentResolver.getType(contentUri) ?: return null
        return contentType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        contentResolver.query(contentUri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            return cursor.getLong(sizeIndex)
        } ?: return -1
    }

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(contentUri)
            ?: throw IOException("Couldn't open content URI for reading: $contentUri")

        inputStream.source().use { source ->
            sink.writeAll(source)
        }
    }
}
