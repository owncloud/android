package com.owncloud.android.lib.common.authentication.oauth

import android.net.Uri
import net.openid.appauth.Preconditions
import net.openid.appauth.connectivity.ConnectionBuilder
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Based on [net.openid.appauth.connectivity.DefaultConnectionBuilder] but permitting http connections in addition
 * to https connections
 */
class OAuthConnectionBuilder : ConnectionBuilder {
    @Throws(IOException::class)
    override fun openConnection(uri: Uri): HttpURLConnection {
        Preconditions.checkNotNull(uri, "url must not be null")
        val conn = URL(uri.toString()).openConnection() as HttpURLConnection
        return conn.apply {
            connectTimeout = CONNECTION_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
        }
    }

    companion object {
        private val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
        private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10).toInt()
    }
}
