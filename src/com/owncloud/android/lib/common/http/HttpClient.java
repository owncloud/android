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

import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.http.interceptors.HttpInterceptor;
import com.owncloud.android.lib.common.http.interceptors.UserAgentInterceptor;

import java.util.Arrays;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * Client used to perform network operations
 * @author David González Verdugo
 */
public class HttpClient {

    private static OkHttpClient mOkHttpClient;
    private static HttpInterceptor mOkHttpInterceptor;

    public static OkHttpClient getOkHttpClient() {
        if (mOkHttpClient == null) {

            mOkHttpInterceptor = new HttpInterceptor()
                    .addRequestInterceptor(new UserAgentInterceptor(
                                    // TODO Try to get rid of this dependency
                                    OwnCloudClientManagerFactory.getUserAgent()
                            )
                    );

            mOkHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(mOkHttpInterceptor)
                    .protocols(Arrays.asList(Protocol.HTTP_1_1))
                    .followRedirects(false)
                    .build();
        }

        return mOkHttpClient;
    }

    public static HttpInterceptor getOkHttpInterceptor() {
        return mOkHttpInterceptor;
    }
}