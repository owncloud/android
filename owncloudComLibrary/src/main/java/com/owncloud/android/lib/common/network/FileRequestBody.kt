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

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Source
import okio.source
import timber.log.Timber
import java.io.File
import java.util.HashSet

/**
 * A Request body that represents a file and include information about the progress when uploading it
 *
 * @author David González Verdugo
 */
open class FileRequestBody(
    val file: File,
    private val contentType: MediaType?,
) : RequestBody(), ProgressiveDataTransferer {

    val dataTransferListeners: MutableSet<OnDatatransferProgressListener> = HashSet()

    override fun isOneShot(): Boolean = true

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val source: Source
        var it: Iterator<OnDatatransferProgressListener>
        try {
            source = file.source()
            var transferred: Long = 0
            var read: Long
            while (source.read(sink.buffer, BYTES_TO_READ).also { read = it } != -1L) {
                transferred += read
                sink.flush()
                synchronized(dataTransferListeners) {
                    it = dataTransferListeners.iterator()
                    while (it.hasNext()) {
                        it.next().onTransferProgress(read, transferred, file.length(), file.absolutePath)
                    }
                }
            }
            Timber.d("File with name ${file.name} and size ${file.length()} written in request body")
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun addDatatransferProgressListener(listener: OnDatatransferProgressListener) {
        synchronized(dataTransferListeners) {
            dataTransferListeners.add(listener)
        }
    }

    override fun addDatatransferProgressListeners(listeners: Collection<OnDatatransferProgressListener>) {
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
