/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudSamlSsoCredentials;
import com.owncloud.android.lib.common.accounts.AccountUtils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * Created by davidgonzalez on 22/2/17.
 *
 * An utils provider for building data sources used in video preview
 */

public class PreviewVideoUtils {

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final int NOT_FOUND_ERROR = 404;

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    protected static DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter,
                                                               Context context, OCFile file,
                                                               Account account) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null, context, file, account);
    }

    protected static DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter,
                                                               Context context, OCFile file, Account account) {
        return new DefaultDataSourceFactory(context, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter, file, account));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param bandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    protected static HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter,
                                                                       OCFile file, Account account) {

        if (file.isDown()) {

            return new DefaultHttpDataSourceFactory(MainApp.getUserAgent(), bandwidthMeter);

        } else {

            try {

                //Get account credentials asynchronously
                final GetCredentialsTask task = new GetCredentialsTask();
                task.execute(account);

                OwnCloudCredentials credentials = task.get();

                String login = credentials.getUsername();
                String password = credentials.getAuthToken();

                Map<String, String> params = new HashMap<String, String>(1);

                if (credentials instanceof OwnCloudBasicCredentials) {
                    // Basic auth
                    String cred = login + ":" + password;
                    String auth = "Basic " + Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE);
                    params.put("Authorization", auth);
                } else if (credentials instanceof OwnCloudSamlSsoCredentials) {
                    // SAML SSO
                    params.put("Cookie", password);
                }

                return new CustomHttpDataSourceFactory(MainApp.getUserAgent(), bandwidthMeter, params);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Task for getting account credentials asynchronously
     */
    private static class GetCredentialsTask extends AsyncTask<Object, Void, OwnCloudCredentials> {
        @Override
        protected OwnCloudCredentials doInBackground(Object... params) {
            Object account = params[0];
            try {
                OwnCloudCredentials ocCredentials = AccountUtils.getCredentialsForAccount(MainApp.getAppContext(), (Account) account);
                return ocCredentials;
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    /**
     *
     * @param error Exoplayer exception
     * @param context
     * @return preview video error after processing the Exoplayer exception
     */
    public static PreviewVideoError handlePreviewVideoError (ExoPlaybackException error, Context context) {

        PreviewVideoError previewVideoError = null;

        switch (error.type) {

            case ExoPlaybackException.TYPE_SOURCE:

                previewVideoError = handlePlayerSourceError(error, context);

                break;

            case ExoPlaybackException.TYPE_UNEXPECTED:

                previewVideoError = handlePlayerUnexpectedError(error, context);

                break;

            case ExoPlaybackException.TYPE_RENDERER:

                previewVideoError = handlePlayerRendererError(error, context);

                break;
        }

        return previewVideoError;
    }

    /**
     * Handle video player source exceptions and create a PreviewVideoError with the appropriate info
     *
     * @param error Exoplayer source exception
     * @param context
     * @return preview video error after processing the Exoplayer source exception
     */
    private static PreviewVideoError handlePlayerSourceError(ExoPlaybackException error, Context context) {

        PreviewVideoError previewVideoError;

        if (error.getSourceException().getCause() != null && error.getSourceException().getCause()
                .getCause() instanceof CertificateException) { // Current certificate untrusted

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.streaming_certificate_error),
                    true,
                    false);

        } else if (error.getSourceException().getCause() != null && error.getSourceException().getCause()
                instanceof UnknownHostException) {  // Cannot connect with the server

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.network_error_socket_exception),
                    false,
                    false);

        } else if (error.getSourceException() instanceof UnrecognizedInputFormatException) {

            // Unsupported video file format
            // Important: this error is also thrown when the saml session expires. In this case,
            // the parent folder starts to synchronize and login view is shown

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.streaming_unrecognized_input),
                    false,
                    true);

        } else if (error.getSourceException() instanceof HttpDataSource.InvalidResponseCodeException

                && ((HttpDataSource.InvalidResponseCodeException) error.getSourceException())

                .responseCode == NOT_FOUND_ERROR) { // Video file no longer exists in the server

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.streaming_file_not_found_error),
                    false,
                    false);
        } else {

            String message = error.getSourceException().getMessage();

            if (message == null) {
                message = context.getString(R.string.streaming_common_error);
            }

            previewVideoError = new PreviewVideoError(message, false, false);
        }

        return previewVideoError;
    }

    /**
     * Handle video player unexpected exceptions and create a PreviewVideoError with the appropriate
     * info
     *
     * @param error Exoplayer unexpected exception
     * @param context
     * @return preview video error after processing the Exoplayer unexpected exception
     */
    private static PreviewVideoError handlePlayerUnexpectedError(ExoPlaybackException error,
                                                                 Context context) {

        PreviewVideoError previewVideoError;

        String message = error.getUnexpectedException().getMessage();

        if (message != null ) {

            previewVideoError = new PreviewVideoError(message, false, false);

        } else {

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.streaming_common_error),
                    false,
                    false);

        }

        return  previewVideoError;
    }

    /**
     * Handle video player renderer exceptions and create a PreviewVideoError with the appropriate
     * info
     * @param error Exoplayer renderer exception
     * @param context
     * @return preview video error after processing the Exoplayer renderer exception
     */
    private static PreviewVideoError handlePlayerRendererError(ExoPlaybackException error,
                                                               Context context) {

        PreviewVideoError previewVideoError;

        String message = error.getRendererException().getMessage();

        if (message != null ) {

            previewVideoError = new PreviewVideoError(message, false, false);

        } else {

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.streaming_common_error),
                    false,
                    false);
        }

        return previewVideoError;
    }
}
