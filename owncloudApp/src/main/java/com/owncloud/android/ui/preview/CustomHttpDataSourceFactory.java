package com.owncloud.android.ui.preview;

/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2021 ownCloud GmbH.
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

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import okhttp3.Call;
import okhttp3.OkHttpClient;

import java.util.Map;

/**
 * A {@link HttpDataSource.Factory} that produces {@link OkHttpDataSource} instances
 * with custom X509TrustManager support through OkHttpClient.
 */
@OptIn(markerClass = UnstableApi.class)
public final class CustomHttpDataSourceFactory extends HttpDataSource.BaseFactory {

    private final OkHttpClient okHttpClient;
    private final String userAgent;
    private final TransferListener listener;
    private final Map<String, String> headers;

    /**
     * Constructs a CustomHttpDataSourceFactory using OkHttp as the underlying HTTP client.
     *
     * @param okHttpClient OkHttpClient with configured SSL/TrustManager
     * @param userAgent    The User-Agent string that should be used.
     * @param headers      HTTP headers to include in requests (e.g., Authorization header)
     */
    public CustomHttpDataSourceFactory(
            OkHttpClient okHttpClient,
            String userAgent, TransferListener listener,
            Map<String, String> headers) {
        this.okHttpClient = okHttpClient;
        this.userAgent = userAgent;
        this.listener = listener;
        this.headers = headers;
    }

    @NonNull
    @Override
    protected HttpDataSource createDataSourceInternal(@NonNull HttpDataSource.RequestProperties defaultRequestProperties) {
        OkHttpDataSource dataSource = new OkHttpDataSource.Factory((Call.Factory) okHttpClient)
                .setUserAgent(userAgent)
                .setTransferListener(listener)
                .createDataSource();

        // Set headers in http data source
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            dataSource.setRequestProperty(entry.getKey(), entry.getValue());
        }

        return dataSource;
    }
}
