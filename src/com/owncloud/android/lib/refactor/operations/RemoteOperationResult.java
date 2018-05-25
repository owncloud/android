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

package com.owncloud.android.lib.refactor.operations;

import android.accounts.Account;
import android.accounts.AccountsException;

import com.owncloud.android.lib.refactor.Log_OC;
import com.owncloud.android.lib.refactor.exceptions.AccountNotFoundException;
import com.owncloud.android.lib.refactor.exceptions.CertificateCombinedException;
import com.owncloud.android.lib.refactor.exceptions.OperationCancelledException;
import com.owncloud.android.lib.refactor.utils.ErrorMessageParser;
import com.owncloud.android.lib.refactor.utils.InvalidCharacterExceptionParser;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import at.bitfire.dav4android.exception.ConflictException;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.exception.InvalidDavResponseException;
import at.bitfire.dav4android.exception.NotFoundException;
import at.bitfire.dav4android.exception.PreconditionFailedException;
import at.bitfire.dav4android.exception.ServiceUnavailableException;
import at.bitfire.dav4android.exception.UnauthorizedException;
import at.bitfire.dav4android.exception.UnsupportedDavException;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;


/**
 * The result of a remote operation required to an ownCloud server.
 * <p/>
 * Provides a common classification of remote operation results for all the
 * application.
 *
 * @author David A. Velasco
 */
public abstract class RemoteOperationResult implements Serializable {

    /**
     * Generated - should be refreshed every time the class changes!!
     */
    private static final long serialVersionUID = 4968939884332652230L;

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
        if (e instanceof SSLException || e instanceof RuntimeException) {
            CertificateCombinedException se = getCertificateCombinedException(e);
            mException = se;
        } else {
            mException = e;
        }

        mCode = getResultCodeByException(e);
    }

    private ResultCode getResultCodeByException(Exception e) {
        return (e instanceof UnauthorizedException) ? ResultCode.UNAUTHORIZED
                : (e instanceof NotFoundException) ? ResultCode.FILE_NOT_FOUND
                : (e instanceof ConflictException) ? ResultCode.CONFLICT
                : (e instanceof PreconditionFailedException) ? ResultCode.UNKNOWN_ERROR
                : (e instanceof ServiceUnavailableException) ? ResultCode.SERVICE_UNAVAILABLE
                : (e instanceof HttpException) ? ResultCode.UNHANDLED_HTTP_CODE
                : (e instanceof InvalidDavResponseException) ? ResultCode.UNKNOWN_ERROR
                : (e instanceof UnsupportedDavException) ? ResultCode.UNKNOWN_ERROR
                : (e instanceof DavException) ? ResultCode.UNKNOWN_ERROR
                : (e instanceof SSLException || e instanceof RuntimeException) ? handleSSLException(e)
                : (e instanceof SocketException) ? ResultCode.WRONG_CONNECTION
                : (e instanceof SocketTimeoutException) ? ResultCode.TIMEOUT
                : (e instanceof MalformedURLException) ? ResultCode.INCORRECT_ADDRESS
                : (e instanceof UnknownHostException) ? ResultCode.HOST_NOT_AVAILABLE
                : ResultCode.UNKNOWN_ERROR;
    }

    private ResultCode handleSSLException(Exception e) {
        final CertificateCombinedException se = getCertificateCombinedException(e);
        return (se != null && se.isRecoverable()) ? ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED
                : (e instanceof RuntimeException) ? ResultCode.HOST_NOT_AVAILABLE
                : ResultCode.SSL_ERROR;
    }

    /**
     * Public constructor from separate elements of an HTTP or DAV response.
     *
     * To be used when the result needs to be interpreted from the response of an HTTP/DAV method.
     *
     * Determines a {@link ResultCode} from the already executed method received as a parameter. Generally,
     * will depend on the HTTP code and HTTP response headers received. In some cases will inspect also the
     * response body
     *
     * @param success
     * @param request
     * @param response
     * @throws IOException
     */
    public RemoteOperationResult(boolean success, Request request, Response response) throws IOException {
        this(success, response.code(), HttpStatus.getStatusText(response.code()), response.headers());

        if (mHttpCode == HttpStatus.SC_BAD_REQUEST) {   // 400

            String bodyResponse = response.body().string();
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

        // before
        switch (mHttpCode) {
            case HttpStatus.SC_FORBIDDEN:
                parseErrorMessageAndSetCode(request, response, ResultCode.SPECIFIC_FORBIDDEN);
                break;
            case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
                parseErrorMessageAndSetCode(request, response, ResultCode.SPECIFIC_UNSUPPORTED_MEDIA_TYPE);
                break;
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
                parseErrorMessageAndSetCode(request, response, ResultCode.SPECIFIC_SERVICE_UNAVAILABLE);
                break;
            default:
                break;
        }
    }

    /**
     * Parse the error message included in the body response, if any, and set the specific result
     * code
     */

    /**
     * Parse the error message included in the body response, if any, and set the specific result
     * code
     *
     * @param request okHttp request
     * @param response okHttp respnse
     * @param resultCode our own custom result code
     * @throws IOException
     */
    private void parseErrorMessageAndSetCode(Request request, Response response, ResultCode resultCode)
            throws IOException {

        String bodyResponse = response.body().string();

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
     * Determines a {@link ResultCode} depending on the HTTP code and HTTP response headers received.
     *
     * @param success     The operation was considered successful or not.
     * @param httpCode    HTTP status code returned by an HTTP/DAV method.
     * @param httpPhrase  HTTP status line phrase returned by an HTTP/DAV method
     * @param headers     HTTP response header returned by an HTTP/DAV method
     */
    public RemoteOperationResult(boolean success, int httpCode, String httpPhrase, Headers headers) {
        this(success, httpCode, httpPhrase);
        for (Map.Entry<String, List<String>> header : headers.toMultimap().entrySet()) {
            if ("location".equals(header.getKey().toLowerCase())) {
                mRedirectedLocation = header.getValue().get(0);
                continue;
            }
            if ("www-authenticate".equals(header.getKey().toLowerCase())) {
                mAuthenticate.add(header.getValue().get(0).toLowerCase());
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
        mCode = success
                ? ResultCode.OK
                : getCodeFromStatus(httpCode);
    }

    private ResultCode getCodeFromStatus(int status) {
        switch (status) {
            case HttpStatus.SC_UNAUTHORIZED: return ResultCode.UNAUTHORIZED;
            case HttpStatus.SC_FORBIDDEN: return ResultCode.FORBIDDEN;
            case HttpStatus.SC_NOT_FOUND: return ResultCode.FILE_NOT_FOUND;
            case HttpStatus.SC_CONFLICT: return ResultCode.CONFLICT;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR: return ResultCode.INSTANCE_NOT_CONFIGURED;
            case HttpStatus.SC_SERVICE_UNAVAILABLE: return ResultCode.SERVICE_UNAVAILABLE;
            case HttpStatus.SC_INSUFFICIENT_STORAGE: return ResultCode.QUOTA_EXCEEDED;
            default:
                Log_OC.d(TAG,
                        "RemoteOperationResult has processed UNHANDLED_HTTP_CODE: " +
                                mHttpCode + " " + mHttpPhrase
                );
                return ResultCode.UNHANDLED_HTTP_CODE;
        }
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
        return (cause != null && cause instanceof CertificateCombinedException)
                ? (CertificateCombinedException) cause
                : result;
    }

    public String getLogMessage() {

        if (mException != null) {
            return (mException instanceof OperationCancelledException)
                    ? "Operation cancelled by the caller"
                    : (mException instanceof SocketException) ? "Socket exception"
                    : (mException instanceof SocketTimeoutException) ? "Socket timeout exception"
                    : (mException instanceof MalformedURLException) ? "Malformed URL exception"
                    : (mException instanceof UnknownHostException) ? "Unknown host exception"
                    : (mException instanceof CertificateCombinedException) ?
                        (((CertificateCombinedException) mException).isRecoverable()
                                ? "SSL recoverable exception"
                                : "SSL exception")
                    : (mException instanceof SSLException) ? "SSL exception"
                    : (mException instanceof DavException) ? "Unexpected WebDAV exception"
                    : (mException instanceof HttpException) ? "HTTP violation"
                    : (mException instanceof IOException) ? "Unrecovered transport exception"
                    : (mException instanceof AccountNotFoundException)
                        ? handleFailedAccountException((AccountNotFoundException)mException)
                    : (mException instanceof AccountsException) ? "Exception while using account"
                    : (mException instanceof JSONException) ? "JSON exception"
                    : "Unexpected exception";
        }

        switch (mCode) {
            case INSTANCE_NOT_CONFIGURED: return "The ownCloud server is not configured!";
            case NO_NETWORK_CONNECTION: return "No network connection";
            case BAD_OC_VERSION: return "No valid ownCloud version was found at the server";
            case LOCAL_STORAGE_FULL: return "Local storage full";
            case LOCAL_STORAGE_NOT_MOVED: return "Error while moving file to final directory";
            case ACCOUNT_NOT_NEW: return "Account already existing when creating a new one";
            case INVALID_CHARACTER_IN_NAME: return "The file name contains an forbidden character";
            case FILE_NOT_FOUND: return "Local file does not exist";
            case SYNC_CONFLICT: return "Synchronization conflict";
            default: return "Operation finished with HTTP status code "
                    + mHttpCode
                    + " ("
                    + (isSuccess() ? "success" : "fail")
                    + ")";
        }
    }

    private String handleFailedAccountException(AccountNotFoundException e) {
        final Account failedAccount = e.getFailedAccount();
        return e.getMessage() + " (" +
                (failedAccount != null ? failedAccount.name : "NULL") + ")";
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

    /** TODO: make this set via constructor
     * Checks if is a non https connection
     *
     * @return boolean true/false

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
    */
}
