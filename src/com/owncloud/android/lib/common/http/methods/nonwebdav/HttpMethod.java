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

package com.owncloud.android.lib.common.http.methods.nonwebdav;

import com.owncloud.android.lib.common.http.HttpBaseMethod;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Request;

/**
 * Wrapper to perform OkHttp calls
 *
 * @author David González Verdugo
 */
public abstract class HttpMethod extends HttpBaseMethod {

    public HttpMethod(String httpUrl) {
        super();
        mRequest = new Request.Builder()
                .url(httpUrl)
                .build();
    }

    public HttpMethod(HttpUrl httpUrl) {
        super();
        mRequest = new Request.Builder()
                .url(httpUrl)
                .build();
    }

    // Request headers
    public void addRequestHeader(String name, String value) {
        mRequest.newBuilder()
                .addHeader(name, value)
                .build();
    }

    public void setRequestHeader(String name, String value){
        mRequest.newBuilder()
                .header(name, value);
    }

    public int executeRequest() throws IOException {
        mResponse = mOkHttpClient.newCall(mRequest).execute();
        return super.getStatusCode();
    }
}