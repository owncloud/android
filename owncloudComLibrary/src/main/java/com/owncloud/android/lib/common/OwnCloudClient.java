/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

import at.bitfire.dav4jvm.exception.HttpException;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials;
import com.owncloud.android.lib.common.http.HttpClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.utils.RandomUtils;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.exception.ExceptionUtils;
import timber.log.Timber;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.owncloud.android.lib.common.http.HttpConstants.AUTHORIZATION_HEADER;
import static com.owncloud.android.lib.common.http.HttpConstants.OC_X_REQUEST_ID;

public class OwnCloudClient extends HttpClient {

    public static final String WEBDAV_FILES_PATH_4_0 = "/remote.php/dav/files/";
    public static final String WEBDAV_PATH_4_0_AND_LATER = "/remote.php/dav";
    public static final String STATUS_PATH = "/status.php";
    private static final String WEBDAV_UPLOADS_PATH_4_0 = "/remote.php/dav/uploads/";
    private static final int MAX_REDIRECTIONS_COUNT = 5;
    private static final int MAX_REPEAT_COUNT_WITH_FRESH_CREDENTIALS = 1;

    private static int sIntanceCounter = 0;
    private OwnCloudCredentials mCredentials = null;
    private int mInstanceNumber;
    private Uri mBaseUri;
    private OwnCloudVersion mVersion = null;
    private OwnCloudAccount mAccount;
    private final ConnectionValidator mConnectionValidator;
    private Object mRequestMutex = new Object();

    // If set to true a mutex will be used to prevent parallel execution of the execute() method
    // if false the execute() method can be called even though the mutex is already aquired.
    // This is used for the ConnectionValidator, which has to be able to execute OperationsWhile all "normal" operations net
    // to be set on hold.
    private final Boolean mSynchronizeRequests;

    private SingleSessionManager mSingleSessionManager = null;

    private boolean mFollowRedirects = false;

    public OwnCloudClient(Uri baseUri, ConnectionValidator connectionValidator, boolean synchronizeRequests) {
        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be NULL");
        }
        mBaseUri = baseUri;
        mSynchronizeRequests = synchronizeRequests;

        mInstanceNumber = sIntanceCounter++;
        Timber.d("#" + mInstanceNumber + "Creating OwnCloudClient");

        clearCredentials();
        clearCookies();
        mConnectionValidator = connectionValidator;
    }

    public void clearCredentials() {
        if (!(mCredentials instanceof OwnCloudAnonymousCredentials)) {
            mCredentials = OwnCloudCredentialsFactory.getAnonymousCredentials();
        }
    }

    public int executeHttpMethod(HttpBaseMethod method) throws Exception {
        if(mSynchronizeRequests) {
            synchronized (mRequestMutex) {
                return saveExecuteHttpMethod(method);
            }
        } else {
            return saveExecuteHttpMethod(method);
        }
    }

    private int saveExecuteHttpMethod(HttpBaseMethod method) throws Exception {
        boolean repeatWithFreshCredentials;
        int repeatCounter = 0;
        int status;

        boolean retry = false;
        do {
            retry = false;
            String requestId = RandomUtils.generateRandomUUID();

            // Header to allow tracing requests in apache and ownCloud logs
            Timber.d("Executing in request with id %s", requestId);
            method.setRequestHeader(HttpConstants.OC_X_REQUEST_ID, requestId);
            method.setRequestHeader(HttpConstants.USER_AGENT_HEADER, SingleSessionManager.getUserAgent());
            method.setRequestHeader(HttpConstants.ACCEPT_ENCODING_HEADER, HttpConstants.ACCEPT_ENCODING_IDENTITY);
            if (mCredentials.getHeaderAuth() != null && method.getRequestHeader(AUTHORIZATION_HEADER) == null) {
                method.setRequestHeader(AUTHORIZATION_HEADER, mCredentials.getHeaderAuth());
            }

            status = method.execute();
            Timber.d("-------------------------------------");
            stacklog(status, method);

            if (mConnectionValidator != null &&
                    status == HttpConstants.HTTP_MOVED_TEMPORARILY) {
                mConnectionValidator.validate(method, this);
                retry = true;
            } else if (mFollowRedirects) {
                status = followRedirection(method).getLastStatus();
            }

            /*
            repeatWithFreshCredentials = checkUnauthorizedAccess(status, repeatCounter);
            if (repeatWithFreshCredentials) {
                repeatCounter++;
            }

             */
        } while (retry);

        return status;
    }

    private void stacklog(int status, HttpBaseMethod method) {
        try {
            throw new Exception("Stack log");
        } catch(Exception e) {
            Timber.d("\n---------------------------" +
                    "\nresponsecode: " + status +
                    "\nThread: " + Thread.currentThread().getName() +
                    "\nobject: " + this.toString() +
                    "\nMethod: " + method.toString() +
                    "\nUrl: " + method.getHttpUrl() +
                    "\nCookeis: " + getCookiesString() +
                    "\ntrace: " + ExceptionUtils.getStackTrace(e) +
                    "---------------------------");
        }
    }

    private int executeRedirectedHttpMethod(HttpBaseMethod method) throws Exception {
        boolean repeatWithFreshCredentials;
        int repeatCounter = 0;
        int status;

        do {
            String requestId = RandomUtils.generateRandomUUID();

            // Header to allow tracing requests in apache and ownCloud logs
            Timber.d("Executing in request with id %s", requestId);
            method.setRequestHeader(OC_X_REQUEST_ID, requestId);
            method.setRequestHeader(HttpConstants.USER_AGENT_HEADER, SingleSessionManager.getUserAgent());
            method.setRequestHeader(HttpConstants.ACCEPT_ENCODING_HEADER, HttpConstants.ACCEPT_ENCODING_IDENTITY);
            if (mCredentials.getHeaderAuth() != null) {
                method.setRequestHeader(AUTHORIZATION_HEADER, mCredentials.getHeaderAuth());
            }
            status = method.execute();

            repeatWithFreshCredentials = checkUnauthorizedAccess(status, repeatCounter);
            if (repeatWithFreshCredentials) {
                repeatCounter++;
            }
        } while (repeatWithFreshCredentials);

        return status;
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
                Timber.d("#" + mInstanceNumber + "Location to redirect: " + location);

                redirectionPath.addLocation(location);

                // Release the connection to avoid reach the max number of connections per hostClientManager
                // due to it will be set a different url
                exhaustResponse(method.getResponseBodyAsStream());

                Timber.d("+++++++++++++++++++++++++++++++++++++++ %s", getFullUrl(location));
                method.setUrl(getFullUrl(location));
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
                    if (e.getMessage().contains(Integer.toString(HttpConstants.HTTP_MOVED_TEMPORARILY))) {
                        status = HttpConstants.HTTP_MOVED_TEMPORARILY;
                    } else {
                        throw e;
                    }
                }
                redirectionPath.addStatus(status);
                redirectionsCount++;

            } else {
                Timber.d(" #" + mInstanceNumber + "No location to redirect!");
                status = HttpConstants.HTTP_NOT_FOUND;
            }
        }
        return redirectionPath;
    }

    private HttpUrl getFullUrl(String redirection) {
        if(redirection.startsWith("/")) {
            return HttpUrl.parse(mBaseUri.toString() + redirection);
        } else {
            return HttpUrl.parse(redirection);
        }
    }

    /**
     * Exhausts a not interesting HTTP response. Encouraged by HttpClient documentation.
     *
     * @param responseBodyAsStream InputStream with the HTTP response to exhaust.
     */
    public void exhaustResponse(InputStream responseBodyAsStream) {
        if (responseBodyAsStream != null) {
            try {
                responseBodyAsStream.close();

            } catch (IOException io) {
                Timber.e(io, "Unexpected exception while exhausting not interesting HTTP response; will be IGNORED");
            }
        }
    }

    public Uri getBaseFilesWebDavUri() {
        return Uri.parse(mBaseUri + WEBDAV_FILES_PATH_4_0);
    }

    public Uri getUserFilesWebDavUri() {
        return (mCredentials instanceof OwnCloudAnonymousCredentials || mAccount == null)
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

    public Uri getBaseUri() {
        return mBaseUri;
    }

    /**
     * Sets the root URI to the ownCloud server.
     * <p>
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

    public final OwnCloudCredentials getCredentials() {
        return mCredentials;
    }

    public void setCredentials(OwnCloudCredentials credentials) {
        if (credentials != null) {
            mCredentials = credentials;
        } else {
            clearCredentials();
        }
    }

    public String getCookiesString() {
        StringBuilder cookiesString = new StringBuilder();
        List<Cookie> cookieList = getCookiesForBaseUri();

        if (cookieList != null) {
            for (Cookie cookie : cookieList) {
                cookiesString.append(cookie.toString()).append(";");
            }
        }

        return cookiesString.toString();
    }

    public void setCookiesForBaseUri(List<Cookie> cookies) {
        getOkHttpClient().cookieJar().saveFromResponse(
                HttpUrl.parse(mBaseUri.toString()),
                cookies
        );
    }

    public List<Cookie> getCookiesForBaseUri() {
            return getOkHttpClient().cookieJar().loadForRequest(
                    HttpUrl.parse(mBaseUri.toString()));
    }

    public void clearCookies() {
        setCookiesForBaseUri(new ArrayList<>());
    }

    public OwnCloudVersion getOwnCloudVersion() {
        return mVersion;
    }

    public void setOwnCloudVersion(OwnCloudVersion version) {
        mVersion = version;
    }

    public OwnCloudAccount getAccount() {
        return mAccount;
    }

    public void setAccount(OwnCloudAccount account) {
        this.mAccount = account;
    }

    /**
     * Checks the status code of an execution and decides if should be repeated with fresh credentials.
     * <p>
     * Invalidates current credentials if the request failed as anauthorized.
     * <p>
     * Refresh current credentials if possible, and marks a retry.
     *
     * @param status
     * @param repeatCounter
     * @return
     */
    private boolean checkUnauthorizedAccess(int status, int repeatCounter) {
        boolean credentialsWereRefreshed = false;

        if (shouldInvalidateAccountCredentials(status)) {
            invalidateAccountCredentials();

            if (getCredentials().authTokenCanBeRefreshed() &&
                    repeatCounter < MAX_REPEAT_COUNT_WITH_FRESH_CREDENTIALS) {
                try {
                    mAccount.loadCredentials(getContext());
                    // if mAccount.getCredentials().length() == 0 --> refresh failed
                    setCredentials(mAccount.getCredentials());
                    credentialsWereRefreshed = true;

                } catch (AccountsException | IOException e) {
                    Timber.e(e, "Error while trying to refresh auth token for %s",
                            mAccount.getSavedAccount().name
                    );
                }

                if (!credentialsWereRefreshed && mSingleSessionManager != null) {
                    // if credentials are not refreshed, client must be removed
                    // from the OwnCloudClientManager to prevent it is reused once and again
                    mSingleSessionManager.removeClientFor(mAccount);
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
     * @param httpStatusCode Result of the last request ran with the 'credentials' belows.
     * @return 'True' if credentials should and might be invalidated, 'false' if shouldn't or
     * cannot be invalidated with the given arguments.
     */
    private boolean shouldInvalidateAccountCredentials(int httpStatusCode) {
        boolean shouldInvalidateAccountCredentials =
                (httpStatusCode == HttpConstants.HTTP_UNAUTHORIZED);

        shouldInvalidateAccountCredentials &= (mCredentials != null &&         // real credentials
                !(mCredentials instanceof OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials));

        // test if have all the needed to effectively invalidate ...
        shouldInvalidateAccountCredentials &= (mAccount != null && mAccount.getSavedAccount() != null && getContext() != null);

        return shouldInvalidateAccountCredentials;
    }

    /**
     * Invalidates credentials stored for the given account in the system  {@link AccountManager} and in
     * current {@link SingleSessionManager#getDefaultSingleton()} instance.
     * <p>
     * {@link #shouldInvalidateAccountCredentials(int)} should be called first.
     *
     */
    private void invalidateAccountCredentials() {
        AccountManager am = AccountManager.get(getContext());
        am.invalidateAuthToken(
                mAccount.getSavedAccount().type,
                mCredentials.getAuthToken()
        );
        am.clearPassword(mAccount.getSavedAccount()); // being strict, only needed for Basic Auth credentials
    }

    public boolean getFollowRedirects() {
        return mFollowRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.mFollowRedirects = followRedirects;
    }
}
