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

package com.owncloud.android.lib.common.http.methods;

import com.owncloud.android.lib.common.http.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Wrapper to perform http calls transparently by using:
 *  - OkHttp for non webdav methods
 *  - Dav4Android for webdav methods
 *
 * @author David González Verdugo
 */
public abstract class HttpBaseMethod {
    protected OkHttpClient mOkHttpClient;
    protected Request mRequest;
    protected RequestBody mRequestBody;
    protected Response mResponse;
    protected Call mCall;

    protected HttpBaseMethod (HttpUrl httpUrl) {
        mOkHttpClient = HttpClient.getOkHttpClient();
        mRequest = new Request.Builder()
                .url(httpUrl)
                .build();
    }

    public int execute() throws Exception {
        return onExecute();
    }

    public void abort() {
        mCall.cancel();
    }

    public boolean isAborted() {
        return mCall.isCanceled();
    }


    //////////////////////////////
    //         For override
    //////////////////////////////

    protected abstract int onExecute() throws Exception;

    //////////////////////////////
    //         Getter
    //////////////////////////////

    // Response
    public int getStatusCode() {
        return mResponse.code();
    }

    public String getStatusMessage() {
        return mResponse.message();
    }

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

    public HttpUrl getUrl() {
        return mRequest.url();
    }

    public boolean getRetryOnConnectionFailure() {
        return mOkHttpClient.retryOnConnectionFailure();
    }

    // Request
    public String getRequestHeader(String name) {
        return mRequest.header(name);
    }

    public Headers getRequestHeaders() {
        return mRequest.headers();
    }

    //////////////////////////////
    //         Setter
    //////////////////////////////

    // Connection parameters
    public void setReadTimeout(long readTimeout, TimeUnit timeUnit) {
        mOkHttpClient = mOkHttpClient.newBuilder()
                .readTimeout(readTimeout, timeUnit)
                .build();
    }

    public void setConnectionTimeout(long connectionTimeout, TimeUnit timeUnit) {
        mOkHttpClient = mOkHttpClient.newBuilder()
                .readTimeout(connectionTimeout, timeUnit)
                .build();
    }

    public void setFollowRedirects(boolean followRedirects) {
        mOkHttpClient = mOkHttpClient.newBuilder()
                .followRedirects(followRedirects)
                .build();
    }

    public void setRetryOnConnectionFailure(boolean retryOnConnectionFailure) {
        mOkHttpClient = mOkHttpClient.newBuilder()
                .retryOnConnectionFailure(retryOnConnectionFailure)
                .build();
    }

    public void addRequestHeader(String name, String value) {
        mRequest = mRequest.newBuilder()
                .addHeader(name, value)
                .build();
    }

    public void setRequestHeader(String name, String value) {
        mRequest = mRequest.newBuilder()
                .header(name, value)
                .build();
    }

    public void setRequestBody(RequestBody requestBody) {
        mRequestBody = requestBody;
    }

    public void setUrl(HttpUrl url) {
        mRequest = mRequest.newBuilder()
                .url(url)
                .build();
    }
}