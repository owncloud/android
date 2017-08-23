/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
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

package com.owncloud.android.lib.common.operations;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountsException;

import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.DavException;
import org.json.JSONException;

import javax.net.ssl.SSLException;


/**
 * The result of a remote operation required to an ownCloud server.
 * <p/>
 * Provides a common classification of remote operation results for all the
 * application.
 *
 * @author David A. Velasco
 */
public class RemoteOperationResult implements Serializable {

    /**
     * Generated - should be refreshed every time the class changes!!
     */
    private static final long serialVersionUID = 4968939884332372230L;

    private static final String TAG = RemoteOperationResult.class.getSimpleName();

    public enum ResultCode {
        OK,
        OK_SSL,
        OK_NO_SSL,
        UNHANDLED_HTTP_CODE,
        UNAUTHORIZED,
        FILE_NOT_FOUND,
        INSTANCE_NOT_CONFIGURED,
        UNKNOWN_ERROR,
        WRONG_CONNECTION,
        TIMEOUT,
        INCORRECT_ADDRESS,
        HOST_NOT_AVAILABLE,
        NO_NETWORK_CONNECTION,
        SSL_ERROR,
        SSL_RECOVERABLE_PEER_UNVERIFIED,
        BAD_OC_VERSION,
        CANCELLED,
        INVALID_LOCAL_FILE_NAME,
        INVALID_OVERWRITE,
        CONFLICT,
        OAUTH2_ERROR,
        SYNC_CONFLICT,
        LOCAL_STORAGE_FULL,
        LOCAL_STORAGE_NOT_MOVED,
        LOCAL_STORAGE_NOT_COPIED,
        OAUTH2_ERROR_ACCESS_DENIED,
        QUOTA_EXCEEDED,
        ACCOUNT_NOT_FOUND,
        ACCOUNT_EXCEPTION,
        ACCOUNT_NOT_NEW,
        ACCOUNT_NOT_THE_SAME,
        INVALID_CHARACTER_IN_NAME,
        SHARE_NOT_FOUND,
        LOCAL_STORAGE_NOT_REMOVED,
        FORBIDDEN,
        SHARE_FORBIDDEN,
        SPECIFIC_FORBIDDEN,
        OK_REDIRECT_TO_NON_SECURE_CONNECTION,
        INVALID_MOVE_INTO_DESCENDANT,
        INVALID_COPY_INTO_DESCENDANT,
        PARTIAL_MOVE_DONE,
        PARTIAL_COPY_DONE,
        SHARE_WRONG_PARAMETER,
        WRONG_SERVER_RESPONSE,
        INVALID_CHARACTER_DETECT_IN_SERVER,
        DELAYED_FOR_WIFI,
        LOCAL_FILE_NOT_FOUND,
        SERVICE_UNAVAILABLE,
        SPECIFIC_SERVICE_UNAVAILABLE,
        SPECIFIC_UNSUPPORTED_MEDIA_TYPE
    }

    private boolean mSuccess = false;
    private int mHttpCode = -1;
    private String mHttpPhrase = null;
    private Exception mException = null;
    private ResultCode mCode = ResultCode.UNKNOWN_ERROR;
    private String mRedirectedLocation;
    private ArrayList<String> mAuthenticate = new ArrayList<>();
    private String mLastPermanentLocation = null;

    private ArrayList<Object> mData;

    /**
     * Public constructor from result code.
     *
     * To be used when the caller takes the responsibility of interpreting the result of a {@link RemoteOperation}
     *
     * @param code {@link ResultCode} decided by the caller.
     */
    public RemoteOperationResult(ResultCode code) {
        mCode = code;
        mSuccess = (code == ResultCode.OK || code == ResultCode.OK_SSL ||
                code == ResultCode.OK_NO_SSL ||
                code == ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION);
        mData = null;
    }

    /**
     * Public constructor from exception.
     *
     * To be used when an exception prevented the end of the {@link RemoteOperation}.
     *
     * Determines a {@link ResultCode} depending on the type of the exception.
     *
     * @param e Exception that interrupted the {@link RemoteOperation}
     */
    public RemoteOperationResult(Exception e) {
        mException = e;

        if (e instanceof OperationCancelledException) {
            mCode = ResultCode.CANCELLED;

        } else if (e instanceof SocketException) {
            mCode = ResultCode.WRONG_CONNECTION;

        } else if (e instanceof SocketTimeoutException) {
            mCode = ResultCode.TIMEOUT;

        } else if (e instanceof ConnectTimeoutException) {
            mCode = ResultCode.TIMEOUT;

        } else if (e instanceof MalformedURLException) {
            mCode = ResultCode.INCORRECT_ADDRESS;

        } else if (e instanceof UnknownHostException) {
            mCode = ResultCode.HOST_NOT_AVAILABLE;

        } else if (e instanceof AccountNotFoundException) {
            mCode = ResultCode.ACCOUNT_NOT_FOUND;

        } else if (e instanceof AccountsException) {
            mCode = ResultCode.ACCOUNT_EXCEPTION;

        } else if (e instanceof SSLException || e instanceof RuntimeException) {
            CertificateCombinedException se = getCertificateCombinedException(e);
            if (se != null) {
                mException = se;
                if (se.isRecoverable()) {
                    mCode = ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED;
                }
            } else if (e instanceof RuntimeException) {
                mCode = ResultCode.HOST_NOT_AVAILABLE;

            } else {
                mCode = ResultCode.SSL_ERROR;
            }

        } else if (e instanceof FileNotFoundException) {
            mCode = ResultCode.LOCAL_FILE_NOT_FOUND;

        } else {
            mCode = ResultCode.UNKNOWN_ERROR;
        }

    }

    /**
     * Public constructor from separate elements of an HTTP or DAV response.
     *
     * To be used when the result needs to be interpreted from the response of an HTTP/DAV method.
     *
     * Determines a {@link ResultCode} from the already executed method received as a parameter. Generally,
     * will depend on the HTTP code and HTTP response headers received. In some cases will inspect also the
     * response body.
     *
     * @param success    The operation was considered successful or not.
     * @param httpMethod HTTP/DAV method already executed which response will be examined to interpret the
     *                   result.
     */
    public RemoteOperationResult(boolean success, HttpMethod httpMethod) throws IOException {
        this(
                success,
                httpMethod.getStatusCode(),
                httpMethod.getStatusText(),
                httpMethod.getResponseHeaders()
        );

        if (mHttpCode == HttpStatus.SC_BAD_REQUEST) {   // 400

            String bodyResponse = httpMethod.getResponseBodyAsString();
            // do not get for other HTTP codes!; could not be available

            if (bodyResponse != null && bodyResponse.length() > 0) {
                InputStream is = new ByteArrayInputStream(bodyResponse.getBytes());
                InvalidCharacterExceptionParser xmlParser = new InvalidCharacterExceptionParser();
                try {
                    if (xmlParser.parseXMLResponse(is)) {
                        mCode = ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER;
                    }

                } catch (Exception e) {
                    Log_OC.w(TAG, "Error reading exception from server: " + e.getMessage());
                    // mCode stays as set in this(success, httpCode, headers)
                }
            }
        }

        if (mHttpCode == HttpStatus.SC_FORBIDDEN) {  // 403

            parseErrorMessageAndSetCode(httpMethod, ResultCode.SPECIFIC_FORBIDDEN);
        }

        if (mHttpCode == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE) {    // 415

            parseErrorMessageAndSetCode(httpMethod, ResultCode.SPECIFIC_UNSUPPORTED_MEDIA_TYPE);
        }

        if (mHttpCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {   // 503

            parseErrorMessageAndSetCode(httpMethod, ResultCode.SPECIFIC_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Parse the error message included in the body response, if any, and set the specific result
     * code
     * @param httpMethod HTTP/DAV method already executed which response body will be parsed to get
     *                   the specific error message
     * @param resultCode specific result code
     * @throws IOException
     */
    private void parseErrorMessageAndSetCode(HttpMethod httpMethod, ResultCode resultCode)
            throws IOException {

        String bodyResponse = httpMethod.getResponseBodyAsString();

        if (bodyResponse != null && bodyResponse.length() > 0) {
            InputStream is = new ByteArrayInputStream(bodyResponse.getBytes());
            ErrorMessageParser xmlParser = new ErrorMessageParser();
            try {
                String errorMessage = xmlParser.parseXMLResponse(is);
                if (errorMessage != "" && errorMessage != null) {
                    mCode = resultCode;
                    mHttpPhrase = errorMessage;
                }
            } catch (Exception e) {
                Log_OC.w(TAG, "Error reading exception from server: " + e.getMessage());
                // mCode stays as set in this(success, httpCode, headers)
            }
        }
    }

    /**
     * Public constructor from separate elements of an HTTP or DAV response.
     *
     * To be used when the result needs to be interpreted from HTTP response elements that could come from
     * different requests (WARNING: black magic, try to avoid).
     *
     * If all the fields come from the same HTTP/DAV response, {@link #RemoteOperationResult(boolean, HttpMethod)}
     * should be used instead.
     *
     * Determines a {@link ResultCode} depending on the HTTP code and HTTP response headers received.
     *
     * @param success     The operation was considered successful or not.
     * @param httpCode    HTTP status code returned by an HTTP/DAV method.
     * @param httpPhrase  HTTP status line phrase returned by an HTTP/DAV method
     * @param httpHeaders HTTP response header returned by an HTTP/DAV method
     */
    public RemoteOperationResult(boolean success, int httpCode, String httpPhrase, Header[] httpHeaders) {
        this(success, httpCode, httpPhrase);
        if (httpHeaders != null) {
            for (Header httpHeader : httpHeaders) {
                if ("location".equals(httpHeader.getName().toLowerCase())) {
                    mRedirectedLocation = httpHeader.getValue();
                    continue;
                }
                if ("www-authenticate".equals(httpHeader.getName().toLowerCase())) {
                    mAuthenticate.add(httpHeader.getValue().toLowerCase());
                }
            }
        }
        if (isIdPRedirection()) {
            mCode = ResultCode.UNAUTHORIZED;    // overrides default ResultCode.UNKNOWN
        }
    }

    /**
     * Private constructor for results built interpreting a HTTP or DAV response.
     *
     * Determines a {@link ResultCode} depending of the type of the exception.
     *
     * @param success    Operation was successful or not.
     * @param httpCode   HTTP status code returned by the HTTP/DAV method.
     * @param httpPhrase HTTP status line phrase returned by the HTTP/DAV method
     */
    private RemoteOperationResult(boolean success, int httpCode, String httpPhrase) {
        mSuccess = success;
        mHttpCode = httpCode;
        mHttpPhrase = httpPhrase;

        if (success) {
            mCode = ResultCode.OK;

        } else if (httpCode > 0) {
            switch (httpCode) {
                case HttpStatus.SC_UNAUTHORIZED:                    // 401
                    mCode = ResultCode.UNAUTHORIZED;
                    break;
                case HttpStatus.SC_FORBIDDEN:                       // 403
                    mCode = ResultCode.FORBIDDEN;
                    break;
                case HttpStatus.SC_NOT_FOUND:                       // 404
                    mCode = ResultCode.FILE_NOT_FOUND;
                    break;
                case HttpStatus.SC_CONFLICT:                        // 409
                    mCode = ResultCode.CONFLICT;
                    break;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:           // 500
                    mCode = ResultCode.INSTANCE_NOT_CONFIGURED;     // assuming too much...
                    break;
                case HttpStatus.SC_SERVICE_UNAVAILABLE:             // 503
                    mCode = ResultCode.SERVICE_UNAVAILABLE;
                    break;
                case HttpStatus.SC_INSUFFICIENT_STORAGE:            // 507
                    mCode = ResultCode.QUOTA_EXCEEDED;              // surprise!
                    break;
                default:
                    mCode = ResultCode.UNHANDLED_HTTP_CODE;         // UNKNOWN ERROR
                    Log_OC.d(TAG,
                            "RemoteOperationResult has processed UNHANDLED_HTTP_CODE: " +
                                    mHttpCode + " " + mHttpPhrase
                    );
            }
        }
    }

    public void setData(ArrayList<Object> files) {
        mData = files;
    }

    public ArrayList<Object> getData() {
        return mData;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public boolean isCancelled() {
        return mCode == ResultCode.CANCELLED;
    }

    public int getHttpCode() {
        return mHttpCode;
    }

    public String getHttpPhrase() {
        return mHttpPhrase;
    }

    public ResultCode getCode() {
        return mCode;
    }

    public Exception getException() {
        return mException;
    }

    public boolean isSslRecoverableException() {
        return mCode == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED;
    }

    public boolean isRedirectToNonSecureConnection() {
        return mCode == ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION;
    }

    private CertificateCombinedException getCertificateCombinedException(Exception e) {
        CertificateCombinedException result = null;
        if (e instanceof CertificateCombinedException) {
            return (CertificateCombinedException) e;
        }
        Throwable cause = mException.getCause();
        Throwable previousCause = null;
        while (cause != null && cause != previousCause &&
                !(cause instanceof CertificateCombinedException)) {
            previousCause = cause;
            cause = cause.getCause();
        }
        if (cause != null && cause instanceof CertificateCombinedException) {
            result = (CertificateCombinedException) cause;
        }
        return result;
    }

    public String getLogMessage() {

        if (mException != null) {
            if (mException instanceof OperationCancelledException) {
                return "Operation cancelled by the caller";

            } else if (mException instanceof SocketException) {
                return "Socket exception";

            } else if (mException instanceof SocketTimeoutException) {
                return "Socket timeout exception";

            } else if (mException instanceof ConnectTimeoutException) {
                return "Connect timeout exception";

            } else if (mException instanceof MalformedURLException) {
                return "Malformed URL exception";

            } else if (mException instanceof UnknownHostException) {
                return "Unknown host exception";

            } else if (mException instanceof CertificateCombinedException) {
                if (((CertificateCombinedException) mException).isRecoverable())
                    return "SSL recoverable exception";
                else
                    return "SSL exception";

            } else if (mException instanceof SSLException) {
                return "SSL exception";

            } else if (mException instanceof DavException) {
                return "Unexpected WebDAV exception";

            } else if (mException instanceof HttpException) {
                return "HTTP violation";

            } else if (mException instanceof IOException) {
                return "Unrecovered transport exception";

            } else if (mException instanceof AccountNotFoundException) {
                Account failedAccount =
                        ((AccountNotFoundException) mException).getFailedAccount();
                return mException.getMessage() + " (" +
                        (failedAccount != null ? failedAccount.name : "NULL") + ")";

            } else if (mException instanceof AccountsException) {
                return "Exception while using account";

            } else if (mException instanceof JSONException) {
                return "JSON exception";

            } else {
                return "Unexpected exception";
            }
        }

        if (mCode == ResultCode.INSTANCE_NOT_CONFIGURED) {
            return "The ownCloud server is not configured!";

        } else if (mCode == ResultCode.NO_NETWORK_CONNECTION) {
            return "No network connection";

        } else if (mCode == ResultCode.BAD_OC_VERSION) {
            return "No valid ownCloud version was found at the server";

        } else if (mCode == ResultCode.LOCAL_STORAGE_FULL) {
            return "Local storage full";

        } else if (mCode == ResultCode.LOCAL_STORAGE_NOT_MOVED) {
            return "Error while moving file to final directory";

        } else if (mCode == ResultCode.ACCOUNT_NOT_NEW) {
            return "Account already existing when creating a new one";

        } else if (mCode == ResultCode.ACCOUNT_NOT_THE_SAME) {
            return "Authenticated with a different account than the one updating";

        } else if (mCode == ResultCode.INVALID_CHARACTER_IN_NAME) {
            return "The file name contains an forbidden character";

        } else if (mCode == ResultCode.FILE_NOT_FOUND) {
            return "Local file does not exist";

        } else if (mCode == ResultCode.SYNC_CONFLICT) {
            return "Synchronization conflict";
        }

        return "Operation finished with HTTP status code " + mHttpCode + " (" +
                (isSuccess() ? "success" : "fail") + ")";

    }

    public boolean isServerFail() {
        return (mHttpCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    public boolean isException() {
        return (mException != null);
    }

    public boolean isTemporalRedirection() {
        return (mHttpCode == 302 || mHttpCode == 307);
    }

    public String getRedirectedLocation() {
        return mRedirectedLocation;
    }

    public boolean isIdPRedirection() {
        return (mRedirectedLocation != null &&
                (mRedirectedLocation.toUpperCase().contains("SAML") ||
                        mRedirectedLocation.toLowerCase().contains("wayf")));
    }

    /**
     * Checks if is a non https connection
     *
     * @return boolean true/false
     */
    public boolean isNonSecureRedirection() {
        return (mRedirectedLocation != null && !(mRedirectedLocation.toLowerCase().startsWith("https://")));
    }

    public ArrayList<String> getAuthenticateHeaders() {
        return mAuthenticate;
    }

    public String getLastPermanentLocation() {
        return mLastPermanentLocation;
    }

    public void setLastPermanentLocation(String lastPermanentLocation) {
        mLastPermanentLocation = lastPermanentLocation;
    }

}
