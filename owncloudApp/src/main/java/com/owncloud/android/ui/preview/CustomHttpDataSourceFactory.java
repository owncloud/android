package com.owncloud.android.ui.preview;

/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.util.Base64;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource.BaseFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource.Factory;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.util.Map;

/** A {@link Factory} that produces {@link CustomHttpDataSourceFactory} instances. */
public final class CustomHttpDataSourceFactory extends BaseFactory {

    private final String userAgent;
    private final TransferListener<? super DataSource> listener;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final boolean allowCrossProtocolRedirects;
    private final Map<String, String> headers;

    /**
     * Constructs a CustomHttpDataSourceFactory. Sets {@link
     * DefaultHttpDataSource#DEFAULT_CONNECT_TIMEOUT_MILLIS} as the connection timeout, {@link
     * DefaultHttpDataSource#DEFAULT_READ_TIMEOUT_MILLIS} as the read timeout and disables
     * cross-protocol redirects.
     *
     * @param userAgent The User-Agent string that should be used.
     * @param listener An optional listener.
     * @param params http authentication header
     * @see #CustomHttpDataSourceFactory(String, TransferListener, int, int, boolean,
     * Map<String, String>)
     */
    public CustomHttpDataSourceFactory(
            String userAgent, TransferListener<? super DataSource> listener, Map<String,
            String> params) {
        this(userAgent, listener, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, false, params);
    }

    /**
     * @param userAgent The User-Agent string that should be used.
     * @param listener An optional listener.
     * @param connectTimeoutMillis The connection timeout that should be used when requesting remote
     *     data, in milliseconds. A timeout of zero is interpreted as an infinite timeout.
     * @param readTimeoutMillis The read timeout that should be used when requesting remote data, in
     *     milliseconds. A timeout of zero is interpreted as an infinite timeout.
     * @param allowCrossProtocolRedirects Whether cross-protocol redirects (i.e. redirects from HTTP
     *     to HTTPS and vice versa) are enabled.
     */
    public CustomHttpDataSourceFactory(String userAgent,
                                       TransferListener<? super DataSource> listener,
                                       int connectTimeoutMillis, int readTimeoutMillis,
                                       boolean allowCrossProtocolRedirects,
                                       Map<String, String> params) {
        this.userAgent = userAgent;
        this.listener = listener;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
        this.headers = params;
    }

    @Override
    protected DefaultHttpDataSource createDataSourceInternal() {
        DefaultHttpDataSource defaultHttpDataSource = new DefaultHttpDataSource(userAgent, null,
                listener, connectTimeoutMillis,
                readTimeoutMillis, allowCrossProtocolRedirects);

        // Set headers in http data source
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            defaultHttpDataSource.setRequestProperty(entry.getKey(), entry.getValue());
        }

        return defaultHttpDataSource;
    }
}

