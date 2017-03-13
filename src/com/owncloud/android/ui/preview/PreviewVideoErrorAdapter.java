/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
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

package com.owncloud.android.ui.preview;

import android.content.Context;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.owncloud.android.R;

import java.net.UnknownHostException;
import java.security.cert.CertificateException;

/**
 * Class to choose proper video player error messages to show to the user and proper behaviour to do
 * next depending on the ExoPlayer exceptions
 */

public class PreviewVideoErrorAdapter {

    private static final int NOT_FOUND_ERROR = 404;

    /**
     *
     * @param error Exoplayer exception
     * @param context
     * @return preview video error after processing the Exoplayer exception
     */
    public static PreviewVideoError handlePreviewVideoError(ExoPlaybackException error,
                                                            Context context) {

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
    private static PreviewVideoError handlePlayerSourceError(ExoPlaybackException error,
                                                             Context context) {

        PreviewVideoError previewVideoError;

        if (error.getSourceException().getCause() != null && error.getSourceException().getCause()
                .getCause() instanceof CertificateException) { // Current certificate untrusted

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.streaming_certificate_error),
                    true,
                    false);

        } else if (error.getSourceException().getCause() != null && error.getSourceException().
                getCause() instanceof UnknownHostException) {  // Cannot connect with the server

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
                message = context.getString(R.string.previewing_video_common_error);
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

        if (message != null) {

            previewVideoError = new PreviewVideoError(message, false, false);

        } else {

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.previewing_video_common_error),
                    false,
                    false);

        }

        return previewVideoError;
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

        if (message != null) {

            previewVideoError = new PreviewVideoError(message, false, false);

        } else {

            previewVideoError = new PreviewVideoError(
                    context.getString(R.string.previewing_video_common_error),
                    false,
                    false);
        }

        return previewVideoError;
    }
}
