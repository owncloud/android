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

package com.owncloud.android.lib.common.http.interceptors;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Http interceptor to use multiple interceptors in the same {@link okhttp3.OkHttpClient} instance
 *
 * @author David González Verdugo
 */
public class HttpInterceptor implements Interceptor {

    private final ArrayList<RequestInterceptor> mRequestInterceptors = new ArrayList<>();
    private final ArrayList<ResponseInterceptor> mResponseInterceptors = new ArrayList<>();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        ListIterator<RequestInterceptor> requestInterceptorIterator = mRequestInterceptors.listIterator();

        while (requestInterceptorIterator.hasNext()) {
            RequestInterceptor currentRequestInterceptor = requestInterceptorIterator.next();
            request = currentRequestInterceptor.intercept(request);
        }

        Response response = chain.proceed(request);

        ListIterator<ResponseInterceptor> responseInterceptorIterator = mResponseInterceptors.listIterator();

        while (responseInterceptorIterator.hasNext()) {
            ResponseInterceptor currentResponseInterceptor = responseInterceptorIterator.next();
            response = currentResponseInterceptor.intercept(response);
        }

        return response;
    }

    public HttpInterceptor addRequestInterceptor(RequestInterceptor requestInterceptor) {
        mRequestInterceptors.listIterator().add(requestInterceptor);
        return this;
    }

    public HttpInterceptor addResponseInterceptor(ResponseInterceptor responseInterceptor) {
        mResponseInterceptors.listIterator().add(responseInterceptor);
        return this;
    }

    public ArrayList<RequestInterceptor> getRequestInterceptors() {
        return mRequestInterceptors;
    }

    private ArrayList<RequestHeaderInterceptor> getRequestHeaderInterceptors() {
        ArrayList<RequestHeaderInterceptor> requestHeaderInterceptors = new ArrayList<>();

        for (RequestInterceptor requestInterceptor : mRequestInterceptors) {
            if (requestInterceptor instanceof RequestHeaderInterceptor) {
                requestHeaderInterceptors.add((RequestHeaderInterceptor) requestInterceptor);
            }
        }

        return requestHeaderInterceptors;
    }

    public void deleteRequestHeaderInterceptor(String headerName) {
        ListIterator<RequestInterceptor> requestInterceptorIterator = mRequestInterceptors.listIterator();
        while (requestInterceptorIterator.hasNext()) {
            RequestInterceptor currentRequestInterceptor = requestInterceptorIterator.next();
            if (currentRequestInterceptor instanceof RequestHeaderInterceptor &&
                    ((RequestHeaderInterceptor) currentRequestInterceptor).getHeaderName().equals(headerName)) {
                requestInterceptorIterator.remove();
            }
        }
    }

    public ArrayList<ResponseInterceptor> getResponseInterceptors() {
        return mResponseInterceptors;
    }

    public interface RequestInterceptor {
        Request intercept(Request request) throws IOException;
    }

    public interface ResponseInterceptor {
        Response intercept(Response response) throws IOException;
    }
}
