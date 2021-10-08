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

package com.owncloud.android.lib.common.http;

import android.content.Context;

import com.owncloud.android.lib.common.network.AdvancedX509TrustManager;
import com.owncloud.android.lib.common.network.NetworkUtils;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.TlsVersion;
import timber.log.Timber;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client used to perform network operations
 *
 * @author David González Verdugo
 */

public class HttpClient {
    private static OkHttpClient sOkHttpClient;
    private static Context sContext;
    private static HashMap<String, List<Cookie>> sCookieStore = new HashMap<>();
    private static LogInterceptor sLogInterceptor;
    private static Interceptor sDebugInterceptor;

    public static OkHttpClient getOkHttpClient() {
        if (sOkHttpClient == null) {
            try {
                final X509TrustManager trustManager = new AdvancedX509TrustManager(
                        NetworkUtils.getKnownServersStore(sContext));
                final SSLSocketFactory sslSocketFactory = getNewSslSocketFactory(trustManager);
                // Automatic cookie handling, NOT PERSISTENT
                final CookieJar cookieJar = new CookieJarImpl(sCookieStore);

                // TODO: Not verifying the hostname against certificate. ask owncloud security human if this is ok.
                //.hostnameVerifier(new BrowserCompatHostnameVerifier());
                sOkHttpClient = buildNewOkHttpClient(sslSocketFactory, trustManager, cookieJar);

            } catch (Exception e) {
                Timber.e(e, "Could not setup SSL system.");
            }
        }
        return sOkHttpClient;
    }

    private static SSLContext getSslContext() throws NoSuchAlgorithmException {
        try {
            return SSLContext.getInstance(TlsVersion.TLS_1_3.javaName());
        } catch (NoSuchAlgorithmException tlsv13Exception) {
            try {
                Timber.w("TLSv1.3 is not supported in this device; falling through TLSv1.2");
                return SSLContext.getInstance(TlsVersion.TLS_1_2.javaName());
            } catch (NoSuchAlgorithmException tlsv12Exception) {
                try {
                    Timber.w("TLSv1.2 is not supported in this device; falling through TLSv1.1");
                    return SSLContext.getInstance(TlsVersion.TLS_1_1.javaName());
                } catch (NoSuchAlgorithmException tlsv11Exception) {
                    Timber.w("TLSv1.1 is not supported in this device; falling through TLSv1.0");
                    return SSLContext.getInstance(TlsVersion.TLS_1_0.javaName());
                    // should be available in any device; see reference of supported protocols in
                    // http://developer.android.com/reference/javax/net/ssl/SSLSocket.html
                }
            }
        }
    }

    private static SSLSocketFactory getNewSslSocketFactory(X509TrustManager trustManager)
            throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslContext = getSslContext();
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        return sslContext.getSocketFactory();
    }

    private static OkHttpClient buildNewOkHttpClient(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager,
                                                     CookieJar cookieJar) {
        return new OkHttpClient.Builder()
                .addNetworkInterceptor(getLogInterceptor())
                .addNetworkInterceptor(DebugInterceptorFactory.Companion.getInterceptor())
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .readTimeout(HttpConstants.DEFAULT_DATA_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(HttpConstants.DEFAULT_DATA_TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(HttpConstants.DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(false)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier((asdf, usdf) -> true)
                .cookieJar(cookieJar)
                .build();
    }

    public static LogInterceptor getLogInterceptor() {
        if (sLogInterceptor == null) {
            sLogInterceptor = new LogInterceptor();
        }
        return sLogInterceptor;
    }

    public static List<Cookie> getCookiesFromUrl(HttpUrl httpUrl) {
        return sCookieStore.get(httpUrl.host());
    }

    public Context getContext() {
        return sContext;
    }

    public static void setContext(Context context) {
        sContext = context;
    }

    public void clearCookies() {
        sCookieStore.clear();
    }
}
