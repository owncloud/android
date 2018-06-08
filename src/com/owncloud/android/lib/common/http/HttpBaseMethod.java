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

package com.owncloud.android.lib.common.http;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http2.Header;

/**
 * Wrapper to perform http calls transparently by using:
 *  - OkHttp for non webdav methods
 *  - Dav4Android for webdav methods
 *
 * @author David González Verdugo
 */
public abstract class HttpBaseMethod {
    public abstract int execute() throws Exception;
    protected Response mResponse;
    private static final String TAG = HttpBaseMethod.class.getSimpleName();

    // Status
    public int getStatusCode() {
        return mResponse.code();
    }

    public String getStatusMessage() {
        return mResponse.message();
    }

    // Response
    public String getResponseBodyAsString() throws IOException {
        return mResponse.body().string();
    }

    public InputStream getResponseAsStream() {
        return mResponse.body().byteStream();
    }

    public Headers getResponseHeaders() {
        return mResponse.headers();
    }

    public String getResponseHeader(String headerName) {
        return mResponse.header(headerName);
    }
}