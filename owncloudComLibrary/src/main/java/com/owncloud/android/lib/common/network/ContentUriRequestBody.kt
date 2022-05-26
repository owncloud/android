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

package com.owncloud.android.lib.common.network

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Source
import okio.source
import timber.log.Timber
import java.io.IOException

class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val contentUri: Uri
) : RequestBody(), ProgressiveDataTransferer {

    private val dataTransferListeners: MutableSet<OnDatatransferProgressListener> = HashSet()

    val fileSize: Long = contentResolver.query(contentUri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()
        cursor.getLong(sizeIndex)
    } ?: -1

    override fun contentType(): MediaType? {
        val contentType = contentResolver.getType(contentUri) ?: return null
        return contentType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return fileSize
    }

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(contentUri)
            ?: throw IOException("Couldn't open content URI for reading: $contentUri")

        val previousTime = System.currentTimeMillis()

        sink.writeAndUpdateProgress(inputStream.source())
        inputStream.source().close()

        val laterTime = System.currentTimeMillis()

        Timber.d("Difference - ${laterTime - previousTime} milliseconds")
    }

    private fun BufferedSink.writeAndUpdateProgress(source: Source) {
        var iterator: Iterator<OnDatatransferProgressListener>

        try {
            var totalBytesRead = 0L
            var read: Long
            while (source.read(this.buffer, BYTES_TO_READ).also { read = it } != -1L) {
                totalBytesRead += read
                this.flush()
                synchronized(dataTransferListeners) {
                    iterator = dataTransferListeners.iterator()
                    while (iterator.hasNext()) {
                        iterator.next().onTransferProgress(read, totalBytesRead, fileSize, contentUri.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun addDatatransferProgressListener(listener: OnDatatransferProgressListener) {
        synchronized(dataTransferListeners) {
            dataTransferListeners.add(listener)
        }
    }

    override fun addDatatransferProgressListeners(listeners: MutableCollection<OnDatatransferProgressListener>) {
        synchronized(dataTransferListeners) {
            dataTransferListeners.addAll(listeners)
        }
    }

    override fun removeDatatransferProgressListener(listener: OnDatatransferProgressListener) {
        synchronized(dataTransferListeners) {
            dataTransferListeners.remove(listener)
        }
    }

    companion object {
        private const val BYTES_TO_READ = 4_096L
    }
}
