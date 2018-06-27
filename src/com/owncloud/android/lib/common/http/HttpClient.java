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

import android.content.Context;
import java.util.concurrent.TimeUnit;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.owncloud.android.lib.BuildConfig;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.http.interceptors.HttpInterceptor;
import com.owncloud.android.lib.common.http.interceptors.UserAgentInterceptor;
import com.owncloud.android.lib.common.network.AdvancedX509TrustManager;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;

import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * Client used to perform network operations
 * @author David González Verdugo
 */
public class HttpClient {
    private static final String TAG = HttpClient.class.toString();

    private static OkHttpClient sOkHttpClient;
    private static HttpInterceptor sOkHttpInterceptor;
    private static Context sContext;

    public HttpClient() {

    }

    public static void setContext(Context context) {
        sContext = context;
    }

    public static OkHttpClient getOkHttpClient() {
        if (sOkHttpClient == null) {
            try {
                final X509TrustManager trustManager = new AdvancedX509TrustManager(
                        NetworkUtils.getKnownServersStore(sContext));
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] {trustManager}, null);
                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                        .addInterceptor(getOkHttpInterceptor())
                        .protocols(Arrays.asList(Protocol.HTTP_1_1))
                        .followRedirects(false)
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier(new BrowserCompatHostnameVerifier());
                if(BuildConfig.DEBUG) {
                    clientBuilder.addNetworkInterceptor(new StethoInterceptor());
                }
                sOkHttpClient = clientBuilder.build();

            } catch (Exception e) {
                Log_OC.e(TAG, "Could not setup SSL system.", e);
            }
        }
        return sOkHttpClient;
    }

    public static HttpInterceptor getOkHttpInterceptor() {
        if (sOkHttpInterceptor == null) {
            sOkHttpInterceptor = new HttpInterceptor()
                    .addRequestInterceptor(new UserAgentInterceptor(
                                    // TODO Try to get rid of this dependency
                                    OwnCloudClientManagerFactory.getUserAgent()
                            )
                    );
        }
        return sOkHttpInterceptor;
    }

    /**
     * Sets the connection and wait-for-data timeouts to be applied by default to the methods
     * performed by this client.
     */
    public void setDefaultTimeouts(int defaultDataTimeout, int defaultConnectionTimeout) {
        OkHttpClient.Builder clientBuilder = getOkHttpClient().newBuilder();
        if (defaultDataTimeout >= 0) {
            clientBuilder
                    .readTimeout(defaultDataTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(defaultDataTimeout, TimeUnit.MILLISECONDS);
        }
        if (defaultConnectionTimeout >= 0) {
            clientBuilder.connectTimeout(defaultConnectionTimeout, TimeUnit.MILLISECONDS);
        }
        sOkHttpClient = clientBuilder.build();
    }
}