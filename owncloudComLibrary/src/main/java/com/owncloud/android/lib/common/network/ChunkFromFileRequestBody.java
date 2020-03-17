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

package com.owncloud.android.lib.common.network;

import okhttp3.MediaType;
import okio.BufferedSink;
import timber.log.Timber;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

/**
 * A Request body that represents a file chunk and include information about the progress when uploading it
 *
 * @author David González Verdugo
 */
public class ChunkFromFileRequestBody extends FileRequestBody {

    private final FileChannel mChannel;
    private final long mChunkSize;
    private long mOffset;
    private long mTransferred;
    private ByteBuffer mBuffer = ByteBuffer.allocate(4096);

    public ChunkFromFileRequestBody(File file, MediaType contentType, FileChannel channel, long chunkSize) {
        super(file, contentType);
        if (channel == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
        this.mChannel = channel;
        this.mChunkSize = chunkSize;
        mOffset = 0;
        mTransferred = 0;
    }

    @Override
    public long contentLength() {
        try {
            return Math.min(mChunkSize, mChannel.size() - mChannel.position());
        } catch (IOException e) {
            return mChunkSize;
        }
    }

    @Override
    public void writeTo(BufferedSink sink) {
        int readCount;
        Iterator<OnDatatransferProgressListener> it;

        try {
            mChannel.position(mOffset);
            long size = mFile.length();
            if (size == 0) {
                size = -1;
            }
            long maxCount = Math.min(mOffset + mChunkSize, mChannel.size());
            while (mChannel.position() < maxCount) {

                Timber.v("Sink buffer size: %s", sink.buffer().size());

                readCount = mChannel.read(mBuffer);

                Timber.v("Read " + readCount + " bytes from file channel to " + mBuffer.toString());

                sink.buffer().write(mBuffer.array(), 0, readCount);

                sink.flush();

                Timber.v("Write " + readCount + " bytes to sink buffer with size " + sink.buffer().size());

                mBuffer.clear();
                if (mTransferred < maxCount) {  // condition to avoid accumulate progress for repeated chunks
                    mTransferred += readCount;
                }
                synchronized (mDataTransferListeners) {
                    it = mDataTransferListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onTransferProgress(readCount, mTransferred, size, mFile.getAbsolutePath());
                    }
                }
            }

            Timber.v("Chunk with size " + mChunkSize + " written in request body");

        } catch (Exception exception) {
            Timber.e(exception);
        }
    }

    public void setOffset(long offset) {
        this.mOffset = offset;
    }
}