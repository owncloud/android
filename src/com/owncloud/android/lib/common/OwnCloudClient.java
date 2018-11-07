/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.lib.common;

import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.net.Uri;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials;
import com.owncloud.android.lib.common.http.HttpClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.common.utils.RandomUtils;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import at.bitfire.dav4android.exception.HttpException;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.HttpUrl;

import static com.owncloud.android.lib.common.http.HttpConstants.OC_X_REQUEST_ID;

public class OwnCloudClient extends HttpClient {

    public static final String WEBDAV_FILES_PATH_4_0 = "/remote.php/dav/files/";
    public static final String WEBDAV_UPLOADS_PATH_4_0 = "/remote.php/dav/uploads/";
    public static final String STATUS_PATH = "/status.php";
    public static final String FILES_WEB_PATH = "/index.php/apps/files";

    private static final String TAG = OwnCloudClient.class.getSimpleName();
    private static final int MAX_REDIRECTIONS_COUNT = 3;
    private static final int MAX_REPEAT_COUNT_WITH_FRESH_CREDENTIALS = 1;
    private static final String PARAM_PROTOCOL_VERSION = "http.protocol.version";

    private static byte[] sExhaustBuffer = new byte[1024];
    private static int sIntanceCounter = 0;
    private OwnCloudCredentials mCredentials = null;
    private int mInstanceNumber = 0;
    private Uri mBaseUri;
    private OwnCloudVersion mVersion = null;
    private OwnCloudAccount mAccount;

    /**
     * {@link @OwnCloudClientManager} holding a reference to this object and delivering it to callers; might be
     * NULL
     */
    private OwnCloudClientManager mOwnCloudClientManager = null;

    private String mRedirectedLocation;
    private boolean mFollowRedirects;

    public OwnCloudClient(Uri baseUri) {
        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be NULL");
        }
        mBaseUri = baseUri;

        mInstanceNumber = sIntanceCounter++;
        Log_OC.d(TAG + " #" + mInstanceNumber, "Creating OwnCloudClient");

        clearCredentials();
        clearCookies();
    }

    public void setCredentials(OwnCloudCredentials credentials) {
        if (credentials != null) {
            mCredentials = credentials;
            mCredentials.applyTo(this);
        } else {
            clearCredentials();
        }
    }

    public void clearCredentials() {
        if (!(mCredentials instanceof OwnCloudAnonymousCredentials)) {
            mCredentials = OwnCloudCredentialsFactory.getAnonymousCredentials();
        }
        mCredentials.applyTo(this);
    }

    public void applyCredentials() {
        mCredentials.applyTo(this);
    }

    public int executeHttpMethod (HttpBaseMethod method) throws Exception {
        boolean repeatWithFreshCredentials;
        int repeatCounter = 0;
        int status;

        do {
            setRequestId(method);

            status = method.execute();
            checkFirstRedirection(method);

            if(mFollowRedirects && !isIdPRedirection()) {
                status = followRedirection(method).getLastStatus();
            }

            repeatWithFreshCredentials = checkUnauthorizedAccess(status, repeatCounter);
            if (repeatWithFreshCredentials) {
                repeatCounter++;
            }
        } while (repeatWithFreshCredentials);

        return status;
    }

    private void checkFirstRedirection(HttpBaseMethod method) {
        final String location = method.getResponseHeader(HttpConstants.LOCATION_HEADER_LOWER);
        if(location != null && !location.isEmpty()) {
            mRedirectedLocation = location;
        }
    }

    private int executeRedirectedHttpMethod (HttpBaseMethod method) throws Exception {
        boolean repeatWithFreshCredentials;
        int repeatCounter = 0;
        int status;

        do {
            setRequestId(method);

            status = method.execute();

            repeatWithFreshCredentials = checkUnauthorizedAccess(status, repeatCounter);
            if (repeatWithFreshCredentials) {
                repeatCounter++;
            }
        } while (repeatWithFreshCredentials);

        return status;
    }

    private void setRequestId(HttpBaseMethod method) {
        // Clean previous request id. This is a bit hacky but is the only way to add request headers in WebDAV
        // methods by using Dav4Android
        deleteHeaderForAllRequests(OC_X_REQUEST_ID);

        String requestId = RandomUtils.generateRandomUUID();

        // Header to allow tracing requests in apache and ownCloud logs
        addHeaderForAllRequests(OC_X_REQUEST_ID, requestId);

        Log_OC.d(TAG, "Executing " + method.getClass().getSimpleName() + " in request with id " + requestId);
    }

    public RedirectionPath followRedirection(HttpBaseMethod method) throws Exception {
        int redirectionsCount = 0;
        int status = method.getStatusCode();
        RedirectionPath redirectionPath = new RedirectionPath(status, MAX_REDIRECTIONS_COUNT);

        while (redirectionsCount < MAX_REDIRECTIONS_COUNT &&
                (status == HttpConstants.HTTP_MOVED_PERMANENTLY ||
                        status == HttpConstants.HTTP_MOVED_TEMPORARILY ||
                        status == HttpConstants.HTTP_TEMPORARY_REDIRECT)
                ) {

            final String location = method.getResponseHeader(HttpConstants.LOCATION_HEADER) != null
                    ? method.getResponseHeader(HttpConstants.LOCATION_HEADER)
                    : method.getResponseHeader(HttpConstants.LOCATION_HEADER_LOWER);

            if (location != null) {
                Log_OC.d(TAG + " #" + mInstanceNumber,
                        "Location to redirect: " + location);

                redirectionPath.addLocation(location);

                // Release the connection to avoid reach the max number of connections per host
                // due to it will be set a different url
                exhaustResponse(method.getResponseBodyAsStream());

                method.setUrl(HttpUrl.parse(location));
                final String destination = method.getRequestHeader("Destination") != null
                        ? method.getRequestHeader("Destination")
                        : method.getRequestHeader("destination");

                if (destination != null) {
                    final int suffixIndex = location.lastIndexOf(getUserFilesWebDavUri().toString());
                    final String redirectionBase = location.substring(0, suffixIndex);
                    final String destinationPath = destination.substring(mBaseUri.toString().length());

                    method.setRequestHeader("destination", redirectionBase + destinationPath);
                }
                try {
                    status = executeRedirectedHttpMethod(method);
                } catch (HttpException e) {
                    if(e.getMessage().contains(Integer.toString(HttpConstants.HTTP_MOVED_TEMPORARILY))) {
                        status = HttpConstants.HTTP_MOVED_TEMPORARILY;
                    } else  {
                        throw e;
                    }
                }
                redirectionPath.addStatus(status);
                redirectionsCount++;

            } else {
                Log_OC.d(TAG + " #" + mInstanceNumber, "No location to redirect!");
                status = HttpConstants.HTTP_NOT_FOUND;
            }
        }
        return redirectionPath;
    }

    /**
     * Exhausts a not interesting HTTP response. Encouraged by HttpClient documentation.
     *
     * @param responseBodyAsStream InputStream with the HTTP response to exhaust.
     */
    public void exhaustResponse(InputStream responseBodyAsStream) {
        if (responseBodyAsStream != null) {
            try {
                while (responseBodyAsStream.read(sExhaustBuffer) >= 0) ;
                responseBodyAsStream.close();

            } catch (IOException io) {
                Log_OC.e(TAG, "Unexpected exception while exhausting not interesting HTTP response;" +
                        " will be IGNORED", io);
            }
        }
    }

    public Uri getBaseFilesWebDavUri(){
        return Uri.parse(mBaseUri + WEBDAV_FILES_PATH_4_0);
    }

    public Uri getUserFilesWebDavUri() {
        return mCredentials instanceof OwnCloudAnonymousCredentials
                ? Uri.parse(mBaseUri + WEBDAV_FILES_PATH_4_0)
                : Uri.parse(mBaseUri + WEBDAV_FILES_PATH_4_0 + AccountUtils.getUserId(
                        mAccount.getSavedAccount(), getContext()
                )
        );
    }

    public Uri getUploadsWebDavUri() {
        return mCredentials instanceof OwnCloudAnonymousCredentials
                ? Uri.parse(mBaseUri + WEBDAV_UPLOADS_PATH_4_0)
                : Uri.parse(mBaseUri + WEBDAV_UPLOADS_PATH_4_0 + AccountUtils.getUserId(
                        mAccount.getSavedAccount(), getContext()
                )
        );
    }

    /**
     * Sets the root URI to the ownCloud server.
     *
     * Use with care.
     *
     * @param uri
     */
    public void setBaseUri(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be NULL");
        }
        mBaseUri = uri;
    }

    public Uri getBaseUri() {
        return mBaseUri;
    }

    public final OwnCloudCredentials getCredentials() {
        return mCredentials;
    }

    private void logCookie(Cookie cookie) {
        Log_OC.d(TAG, "Cookie name: " + cookie.name());
        Log_OC.d(TAG, "       value: " + cookie.value());
        Log_OC.d(TAG, "       domain: " + cookie.domain());
        Log_OC.d(TAG, "       path: " + cookie.path());
        Log_OC.d(TAG, "       expiryDate: " + cookie.expiresAt());
        Log_OC.d(TAG, "       secure: " + cookie.secure());
    }

    private void logCookiesAtRequest(Headers headers, String when) {
        int counter = 0;
        for (final String cookieHeader : headers.toMultimap().get("cookie")) {
            Log_OC.d(TAG + " #" + mInstanceNumber,
                    "Cookies at request (" + when + ") (" + counter++ + "): "
                            + cookieHeader);
        }
        if (counter == 0) {
            Log_OC.d(TAG + " #" + mInstanceNumber, "No cookie at request before");
        }
    }

    private void logSetCookiesAtResponse(Headers headers) {
        int counter = 0;
        for (final String cookieHeader : headers.toMultimap().get("set-cookie")) {
            Log_OC.d(TAG + " #" + mInstanceNumber,
                    "Set-Cookie (" + counter++ + "): " + cookieHeader);
        }
        if (counter == 0) {
            Log_OC.d(TAG + " #" + mInstanceNumber, "No set-cookie");
        }
    }

    public String getCookiesString() {
        String cookiesString = "";
        List<Cookie> cookieList = getCookiesFromUrl(HttpUrl.parse(mBaseUri.toString()));

        if (cookieList != null) {
            for (Cookie cookie : cookieList) {
                cookiesString += cookie.toString() + ";";
            }
        }

        return cookiesString;
    }

    public void setCookiesForCurrentAccount(List<Cookie> cookies) {
        getOkHttpClient().cookieJar().saveFromResponse(
                HttpUrl.parse(getAccount().getBaseUri().toString()),
                cookies
        );
    }

    public void setOwnCloudVersion(OwnCloudVersion version) {
        mVersion = version;
    }

    public OwnCloudVersion getOwnCloudVersion() {
        return mVersion;
    }

    public void setAccount(OwnCloudAccount account) {
        this.mAccount = account;
    }

    public OwnCloudAccount getAccount() {
        return mAccount;
    }

    /**
     * Checks the status code of an execution and decides if should be repeated with fresh credentials.
     *
     * Invalidates current credentials if the request failed as anauthorized.
     *
     * Refresh current credentials if possible, and marks a retry.
     *
     * @param status
     * @param repeatCounter
     * @return
     */
    private boolean checkUnauthorizedAccess(int status, int repeatCounter) {
        boolean credentialsWereRefreshed = false;

        if (shouldInvalidateAccountCredentials(status)) {
            boolean invalidated = invalidateAccountCredentials();

            if (invalidated) {
                if (getCredentials().authTokenCanBeRefreshed() &&
                        repeatCounter < MAX_REPEAT_COUNT_WITH_FRESH_CREDENTIALS) {

                    try {
                        mAccount.loadCredentials(getContext());
                        // if mAccount.getCredentials().length() == 0 --> refresh failed
                        setCredentials(mAccount.getCredentials());
                        credentialsWereRefreshed = true;

                    } catch (AccountsException | IOException e) {
                        Log_OC.e(
                                TAG,
                                "Error while trying to refresh auth token for " + mAccount.getSavedAccount().name,
                                e
                        );
                    }
                }

                if (!credentialsWereRefreshed && mOwnCloudClientManager != null) {
                    // if credentials are not refreshed, client must be removed
                    // from the OwnCloudClientManager to prevent it is reused once and again
                    mOwnCloudClientManager.removeClientFor(mAccount);
                }
            }
            // else: onExecute will finish with status 401
        }

        return credentialsWereRefreshed;
    }

    /**
     * Determines if credentials should be invalidated according the to the HTTPS status
     * of a network request just performed.
     *
     * @param httpStatusCode    Result of the last request ran with the 'credentials' belows.

     * @return                  'True' if credentials should and might be invalidated, 'false' if shouldn't or
     *                          cannot be invalidated with the given arguments.
     */
    private boolean shouldInvalidateAccountCredentials(int httpStatusCode) {

        boolean should = (httpStatusCode == HttpConstants.HTTP_UNAUTHORIZED || isIdPRedirection());   // invalid credentials

        should &= (mCredentials != null &&         // real credentials
                !(mCredentials instanceof OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials));

        // test if have all the needed to effectively invalidate ...
        should &= (mAccount != null && mAccount.getSavedAccount() != null && getContext() != null);

        return should;
    }

    /**
     * Invalidates credentials stored for the given account in the system  {@link AccountManager} and in
     * current {@link OwnCloudClientManagerFactory#getDefaultSingleton()} instance.
     *
     * {@link #shouldInvalidateAccountCredentials(int)} should be called first.
     *
     * @return                  'True' if invalidation was successful, 'false' otherwise.
     */
    private boolean invalidateAccountCredentials() {
        AccountManager am = AccountManager.get(getContext());
        am.invalidateAuthToken(
                mAccount.getSavedAccount().type,
                mCredentials.getAuthToken()
        );
        am.clearPassword(mAccount.getSavedAccount()); // being strict, only needed for Basic Auth credentials
        return true;
    }

    public OwnCloudClientManager getOwnCloudClientManager() {
        return mOwnCloudClientManager;
    }

    void setOwnCloudClientManager(OwnCloudClientManager clientManager) {
        mOwnCloudClientManager = clientManager;
    }

    /**
     * Check if the redirection is to an identity provider such as SAML or wayf
     * @return true if the redirection location includes SAML or wayf, false otherwise
     */
    private boolean isIdPRedirection() {
        return (mRedirectedLocation != null &&
                (mRedirectedLocation.toUpperCase().contains("SAML") ||
                        mRedirectedLocation.toLowerCase().contains("wayf")));
    }

    public boolean followRedirects() {
        return mFollowRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.mFollowRedirects = followRedirects;
    }
}