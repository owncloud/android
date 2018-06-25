/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import okhttp3.MediaType;
import okio.BufferedSink;

public class ChunkFromFileRequestBody extends FileRequestBody {

    private static final String TAG = ChunkFromFileChannelRequestEntity.class.getSimpleName();

    //private final File mFile;
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
    public void writeTo(BufferedSink sink) {
        int readCount;
        Iterator<OnDatatransferProgressListener> it;

        try {
            mChannel.position(mOffset);
            long size = mFile.length();
            if (size == 0) size = -1;
            long maxCount = Math.min(mOffset + mChunkSize, mChannel.size());
            while (mChannel.position() < maxCount) {
                readCount = mChannel.read(mBuffer);
                sink.write(mBuffer.array(), 0 ,readCount);
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

            sink.flush();

        } catch (IOException io) {
//            // any read problem will be handled as if the file is not there
//            if (io instanceof FileNotFoundException) {
//                throw io;
//            } else {
//                FileNotFoundException fnf = new FileNotFoundException("Exception reading source file");
//                fnf.initCause(io);
//                throw fnf;
//            }
        }
    }
}