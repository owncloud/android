package com.owncloud.android.lib.common.authentication.oauth;

import android.net.Uri;

import androidx.annotation.NonNull;
import net.openid.appauth.Preconditions;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Based on {@link net.openid.appauth.connectivity.DefaultConnectionBuilder} but permitting http connections in addition
 * to https connections
 */
public final class OAuthConnectionBuilder implements ConnectionBuilder {

    /**
     * The singleton instance of the default connection builder.
     */
    public static final OAuthConnectionBuilder INSTANCE = new OAuthConnectionBuilder();

    private static final int CONNECTION_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(15);
    private static final int READ_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(10);

    @NonNull
    @Override
    public HttpURLConnection openConnection(@NonNull Uri uri) throws IOException {
        Preconditions.checkNotNull(uri, "url must not be null");
        HttpURLConnection conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
        conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(false);
        return conn;
    }
}
