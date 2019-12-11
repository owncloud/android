/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2019 ownCloud GmbH.
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

import android.os.Build;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.http.interceptors.HttpInterceptor;
import com.owncloud.android.lib.common.http.interceptors.RequestHeaderInterceptor;
import com.owncloud.android.lib.common.network.AdvancedX509TrustManager;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Client used to perform network operations
 *
 * @author David González Verdugo
 */
public class HttpClient {
    private static final String TAG = HttpClient.class.toString();

    private static OkHttpClient sOkHttpClient;
    private static HttpInterceptor sOkHttpInterceptor;
    private static Context sContext;
    private static HashMap<String, List<Cookie>> sCookieStore = new HashMap<>();

    public static OkHttpClient getOkHttpClient() {
        if (sOkHttpClient == null) {
            try {
                final X509TrustManager trustManager = new AdvancedX509TrustManager(
                        NetworkUtils.getKnownServersStore(sContext));

                SSLContext sslContext;

                try {
                    sslContext = SSLContext.getInstance("TLSv1.2");
                } catch (NoSuchAlgorithmException tlsv12Exception) {
                    try {
                        Log_OC.w(TAG, "TLSv1.2 is not supported in this device; falling through TLSv1.1");
                        sslContext = SSLContext.getInstance("TLSv1.1");
                    } catch (NoSuchAlgorithmException tlsv11Exception) {
                        Log_OC.w(TAG, "TLSv1.1 is not supported in this device; falling through TLSv1.0");
                        sslContext = SSLContext.getInstance("TLSv1");
                        // should be available in any device; see reference of supported protocols in
                        // http://developer.android.com/reference/javax/net/ssl/SSLSocket.html
                    }
                }

                sslContext.init(null, new TrustManager[]{trustManager}, null);

                SSLSocketFactory sslSocketFactory;

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    // TLS v1.2 is disabled by default in API 19, use custom SSLSocketFactory to enable it
                    sslSocketFactory = new TLSSocketFactory(sslContext.getSocketFactory());
                } else {
                    sslSocketFactory = sslContext.getSocketFactory();
                }

                // Automatic cookie handling, NOT PERSISTENT
                CookieJar cookieJar = new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        // Avoid duplicated cookies
                        Set<Cookie> nonDuplicatedCookiesSet = new HashSet<>();
                        nonDuplicatedCookiesSet.addAll(cookies);
                        List<Cookie> nonDuplicatedCookiesList = new ArrayList<>();
                        nonDuplicatedCookiesList.addAll(nonDuplicatedCookiesSet);

                        sCookieStore.put(url.host(), nonDuplicatedCookiesList);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = sCookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                };

                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                        .addInterceptor(getOkHttpInterceptor())
                        .protocols(Arrays.asList(Protocol.HTTP_1_1))
                        .readTimeout(HttpConstants.DEFAULT_DATA_TIMEOUT, TimeUnit.MILLISECONDS)
                        .writeTimeout(HttpConstants.DEFAULT_DATA_TIMEOUT, TimeUnit.MILLISECONDS)
                        .connectTimeout(HttpConstants.DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                        .followRedirects(false)
                        .sslSocketFactory(sslSocketFactory, trustManager)
                        .hostnameVerifier((asdf, usdf) -> true)
                        .cookieJar(cookieJar);
                // TODO: Not verifying the hostname against certificate. ask owncloud security human if this is ok.
                //.hostnameVerifier(new BrowserCompatHostnameVerifier());
                sOkHttpClient = clientBuilder.build();

            } catch (Exception e) {
                Log_OC.e(TAG, "Could not setup SSL system.", e);
            }
        }
        return sOkHttpClient;
    }

    private static HttpInterceptor getOkHttpInterceptor() {
        if (sOkHttpInterceptor == null) {
            sOkHttpInterceptor = new HttpInterceptor();
            addHeaderForAllRequests(HttpConstants.USER_AGENT_HEADER, OwnCloudClientManagerFactory.getUserAgent());
            addHeaderForAllRequests(HttpConstants.PARAM_SINGLE_COOKIE_HEADER, "true");
            addHeaderForAllRequests(HttpConstants.ACCEPT_ENCODING_HEADER, HttpConstants.ACCEPT_ENCODING_IDENTITY);
        }
        return sOkHttpInterceptor;
    }

    /**
     * Add header that will be included for all the requests from now on
     *
     * @param headerName
     * @param headerValue
     */
    public static void addHeaderForAllRequests(String headerName, String headerValue) {
        HttpInterceptor httpInterceptor = getOkHttpInterceptor();

        if(getOkHttpInterceptor() != null) {
            httpInterceptor.addRequestInterceptor(
                    new RequestHeaderInterceptor(headerName, headerValue)
            );
        }
    }

    public static void deleteHeaderForAllRequests(String headerName) {
        getOkHttpInterceptor().deleteRequestHeaderInterceptor(headerName);
    }

    public Context getContext() {
        return sContext;
    }

    public static void setContext(Context context) {
        sContext = context;
    }

    public void disableAutomaticCookiesHandling() {
        OkHttpClient.Builder clientBuilder = getOkHttpClient().newBuilder();
        clientBuilder.cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                // DO NOTHING
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                return new ArrayList<>();
            }
        });
        sOkHttpClient = clientBuilder.build();
    }

    public List<Cookie> getCookiesFromUrl(HttpUrl httpUrl) {
        return sCookieStore.get(httpUrl.host());
    }

    public void clearCookies() {
        sCookieStore.clear();
    }
}