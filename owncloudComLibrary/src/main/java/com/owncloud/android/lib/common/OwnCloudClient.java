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
import java.util.List;

import static com.owncloud.android.lib.common.http.HttpConstants.AUTHORIZATION_HEADER;
import static com.owncloud.android.lib.common.http.HttpConstants.OC_X_REQUEST_ID;

public class OwnCloudClient extends HttpClient {

    public static final String WEBDAV_FILES_PATH_4_0 = "/remote.php/dav/files/";
    public static final String STATUS_PATH = "/status.php";
    private static final String WEBDAV_UPLOADS_PATH_4_0 = "/remote.php/dav/uploads/";
    private static final int MAX_REDIRECTIONS_COUNT = 5;
    private static final int MAX_RETRY_COUNT = 2;

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

    public OwnCloudClient(Uri baseUri, ConnectionValidator connectionValidator, boolean synchronizeRequests, SingleSessionManager singleSessionManager) {
        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be NULL");
        }
        mBaseUri = baseUri;
        mSynchronizeRequests = synchronizeRequests;
        mSingleSessionManager = singleSessionManager;

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
        int repeatCounter = 0;
        int status;

        boolean retry;
        do {
            repeatCounter++;
            retry = false;
            String requestId = RandomUtils.generateRandomUUID();

            // Header to allow tracing requests in apache and ownCloud logs
            Timber.d("Executing in request with id %s", requestId);
            method.setRequestHeader(HttpConstants.OC_X_REQUEST_ID, requestId);
            method.setRequestHeader(HttpConstants.USER_AGENT_HEADER, SingleSessionManager.getUserAgent());
            method.setRequestHeader(HttpConstants.ACCEPT_ENCODING_HEADER, HttpConstants.ACCEPT_ENCODING_IDENTITY);
            if (mCredentials.getHeaderAuth() != null && !mCredentials.getHeaderAuth().isEmpty()) {
                method.setRequestHeader(AUTHORIZATION_HEADER, mCredentials.getHeaderAuth());
            }

            status = method.execute();
            stacklog(status, method);

            if (mConnectionValidator != null &&
                    (status == HttpConstants.HTTP_MOVED_TEMPORARILY ||
                            (!(mCredentials instanceof OwnCloudAnonymousCredentials) &&
                                    status == HttpConstants.HTTP_UNAUTHORIZED))) {
                retry = mConnectionValidator.validate(this, mSingleSessionManager); // retry on success fail on no success
            } else if (mFollowRedirects) {
                status = followRedirection(method).getLastStatus();
            }

        } while (retry && repeatCounter < MAX_RETRY_COUNT);

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
                    "\nCookeis: " + getCookiesForBaseUri().toString() +
                    "\nCredentials type: " + mCredentials.getClass().toString() +
                    "\ntoken: " + mCredentials.getAuthToken() +

                    "\nHeaders: ++++" +
                    "\n" + method.getRequest().headers().toString() +
                    "+++++++++++++" +
                    "\ntrace: " + ExceptionUtils.getStackTrace(e) +
                    "---------------------------");
        }
    }

    private int executeRedirectedHttpMethod(HttpBaseMethod method) throws Exception {
        int status;
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

    public boolean getFollowRedirects() {
        return mFollowRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.mFollowRedirects = followRedirects;
    }
}