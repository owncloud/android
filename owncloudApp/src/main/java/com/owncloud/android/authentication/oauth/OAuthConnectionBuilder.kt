package com.owncloud.android.authentication.oauth

import android.content.Context
import android.net.Uri
import com.owncloud.android.lib.common.network.AdvancedX509TrustManager
import com.owncloud.android.lib.common.network.NetworkUtils
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultHttpConnectionImpl
import net.openid.appauth.connectivity.HttpConnection
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.NoSuchAlgorithmException
import java.util.Objects
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Based on [net.openid.appauth.connectivity.DefaultConnectionBuilder] but permitting http connections in addition
 * to https connections
 */
class OAuthConnectionBuilder(val context: Context) : ConnectionBuilder {
    /**
     * The singleton instance of the default connection builder.
     */

    @Throws(IOException::class)
    override fun openConnection(uri: Uri): HttpConnection {
        val conn: HttpURLConnection
        val conWrapper: HttpConnection

        if (Objects.equals(uri.scheme, HTTPS_SCHEME)) {
            conn = URL(uri.toString()).openConnection() as HttpsURLConnection
            try {
                val trustManager: X509TrustManager = AdvancedX509TrustManager(
                    NetworkUtils.getKnownServersStore(context)
                )
                val sslContext: SSLContext
                sslContext = try {
                    Timber.w("$TLS_v1_3 is supported in this device;")
                    SSLContext.getInstance(TLS_v1_3)
                } catch (tlsv13Exception: NoSuchAlgorithmException) {
                    try {
                        Timber.w("$TLS_v1_3 is not supported in this device; falling through $TLS_v1_2")
                        SSLContext.getInstance(TLS_v1_2)
                    } catch (tlsv12Exception: NoSuchAlgorithmException) {
                        try {
                            Timber.w("$TLS_v1_2 is not supported in this device; falling through $TLS_v1_1")
                            SSLContext.getInstance(TLS_v1_1)
                        } catch (tlsv11Exception: NoSuchAlgorithmException) {
                            Timber.w("$TLS_v1_1 is not supported in this device; falling through $TLS_v1")
                            SSLContext.getInstance(TLS_v1)
                            // should be available in any device; see reference of supported protocols in
                            // http://developer.android.com/reference/javax/net/ssl/SSLSocket.html
                        }
                    }
                }
                sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
                conn.hostnameVerifier = HostnameVerifier { _, _ -> true } // Do not verify the host for now
                conn.sslSocketFactory = sslContext.socketFactory
            } catch (e: Exception) {
                Timber.e(e, "Could not setup SSL system")
            }
        } else {
            conn = URL(uri.toString()).openConnection() as HttpURLConnection
        }



        conn.apply {
            connectTimeout = CONNECTION_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
        }
        return DefaultHttpConnectionImpl(conn)
    }

    companion object {
        private val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
        private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10).toInt()
        private const val HTTPS_SCHEME = "https"
        private const val TLS_v1 = "TLSv1"
        private const val TLS_v1_1 = "TLSv1.1"
        private const val TLS_v1_2 = "TLSv1.2"
        private const val TLS_v1_3 = "TLSv1.3"
    }
}
