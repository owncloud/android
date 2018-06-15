package com.owncloud.android.lib.common.network;

import android.util.Log;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import static android.content.ContentValues.TAG;

public class FileRequestBody extends RequestBody {

    MediaType mContentType;
    File mFile;

    FileRequestBody(MediaType contentType, File file) {
        mContentType = contentType;
        mFile = file;
    }

    @Override
    public MediaType contentType() {
        return mContentType;
    }

    @Override
    public void writeTo(BufferedSink sink) {
        Source source;
        try {
            source = Okio.source(mFile);
            //sink.writeAll(source);
            Buffer buffer = new Buffer();
            Long remaining = contentLength();

            for (long readCount; (readCount = source.read(buffer, 2048)) != -1;) {
                sink.write(buffer, readCount);
                Log.d(TAG, "source size: " + contentLength() + " remaining bytes: " + (remaining -= readCount));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}