/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2017 ownCloud GmbH.
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;

import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

public class OwnCloudClient extends HttpClient {

    public static final String WEBDAV_PATH_4_0 = "/remote.php/webdav";
    public static final String STATUS_PATH = "/status.php";
    public static final String FILES_WEB_PATH = "/index.php/apps/files";

    private static final String TAG = OwnCloudClient.class.getSimpleName();
    private static final int MAX_REDIRECTIONS_COUNT = 3;
    private static final int MAX_REPEAT_COUNT_WITH_FRESH_CREDENTIALS = 1;
    private static final String PARAM_SINGLE_COOKIE_HEADER = "http.protocol.single-cookie-header";
    private static final boolean PARAM_SINGLE_COOKIE_HEADER_VALUE = true;
    private static final String PARAM_PROTOCOL_VERSION = "http.protocol.version";

    private static byte[] sExhaustBuffer = new byte[1024];

    private static int sIntanceCounter = 0;
    private boolean mFollowRedirects = true;
    private OwnCloudCredentials mCredentials = null;
    private int mInstanceNumber = 0;

    private Uri mBaseUri;

    private OwnCloudVersion mVersion = null;

    /// next too attributes are a very ugly dependency, added to grant silent retry of OAuth token when needed ;
    /// see #shouldInvalidateCredentials and #invalidateCredentials for more details
    private Context mContext;
    private OwnCloudAccount mAccount;

    /**
     * {@link @OwnCloudClientManager} holding a reference to this object and delivering it to callers; might be
     * NULL
     */
    private OwnCloudClientManager mOwnCloudClientManager = null;

    /**
     * When 'true', the method {@link #executeMethod(HttpMethod)}  tries to silently refresh credentials
     * if fails due to lack of authorization, if credentials support authorization refresh.
     */
    private boolean mSilentRefreshOfAccountCredentials = true;

    private String mRedirectedLocation;

    /**
     * Constructor
     */
    public OwnCloudClient(Uri baseUri, HttpConnectionManager connectionMgr) {
        super(connectionMgr);

        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be NULL");
        }
        mBaseUri = baseUri;

        mInstanceNumber = sIntanceCounter++;
        Log_OC.d(TAG + " #" + mInstanceNumber, "Creating OwnCloudClient");

        String userAgent = OwnCloudClientManagerFactory.getUserAgent();
        getParams().setParameter(HttpMethodParams.USER_AGENT, userAgent);
        getParams().setParameter(
            PARAM_PROTOCOL_VERSION,
            HttpVersion.HTTP_1_1
        );

        getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        getParams().setParameter(
            PARAM_SINGLE_COOKIE_HEADER,             // to avoid problems with some web servers
            PARAM_SINGLE_COOKIE_HEADER_VALUE
        );

        applyProxySettings();

        clearCredentials();
    }


    private void applyProxySettings() {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPortSt = System.getProperty("http.proxyPort");
        int proxyPort = 0;
        try {
            if (proxyPortSt != null && proxyPortSt.length() > 0) {
                proxyPort = Integer.parseInt(proxyPortSt);
            }
        } catch (Exception e) {
            Log_OC.w(TAG, "Proxy port could not be read, keeping default value " + proxyPort);
        }

        if (proxyHost != null && proxyHost.length() > 0) {
            HostConfiguration hostCfg = getHostConfiguration();
            hostCfg.setProxy(proxyHost, proxyPort);
            Log_OC.d(TAG, "Proxy settings: " + proxyHost + ":" + proxyPort);
        }
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

    /**
     * Requests the received method with the received timeout (milliseconds).
     *
     * Executes the method through the inherited HttpClient.executedMethod(method).
     *
     * Sets the socket and connection timeouts only for the method received.
     *
     * The timeouts are both in milliseconds; 0 means 'infinite';
     * < 0 means 'do not change the default'
     *
     * @param method            HTTP method request.
     * @param readTimeout       Timeout to set for data reception
     * @param connectionTimeout Timeout to set for connection establishment
     */
    public int executeMethod(HttpMethodBase method, int readTimeout, int connectionTimeout) throws IOException {

        int oldSoTimeout = getParams().getSoTimeout();
        int oldConnectionTimeout = getHttpConnectionManager().getParams().getConnectionTimeout();
        try {
            if (readTimeout >= 0) {
                method.getParams().setSoTimeout(readTimeout);   // this should be enough...
                getParams().setSoTimeout(readTimeout);          // ... but HTTPS needs this
            }
            if (connectionTimeout >= 0) {
                getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeout);
            }
            return executeMethod(method);
        } finally {
            getParams().setSoTimeout(oldSoTimeout);
            getHttpConnectionManager().getParams().setConnectionTimeout(oldConnectionTimeout);
        }
    }


    /**
     * Requests the received method.
     *
     * Executes the method through the inherited HttpClient.executedMethod(method).
     *
     * @param method HTTP method request.
     */
    @Override
    public int executeMethod(HttpMethod method) throws IOException {

        boolean repeatWithFreshCredentials;
        int repeatCounter = 0;
        int status;

        do {
            // Update User Agent
            HttpParams params = method.getParams();
            String userAgent = OwnCloudClientManagerFactory.getUserAgent();
            params.setParameter(HttpMethodParams.USER_AGENT, userAgent);

            preventCrashDueToInvalidPort(method);

            Log_OC.d(TAG + " #" + mInstanceNumber, "REQUEST " +
            method.getName() + " " + method.getPath());

            //logCookiesAtRequest(method.getRequestHeaders(), "before");
            //logCookiesAtState("before");
            method.setFollowRedirects(false);

            status = super.executeMethod(method);

            checkFirstRedirection(method);

            if (mFollowRedirects) {
                status = followRedirection(method).getLastStatus();
            }

            repeatWithFreshCredentials = checkUnauthorizedAccess(status, repeatCounter);
            if (repeatWithFreshCredentials) {
                repeatCounter++;
            }

        } while (repeatWithFreshCredentials);

        //logCookiesAtRequest(method.getRequestHeaders(), "after");
        //logCookiesAtState("after");
        //logSetCookiesAtResponse(method.getResponseHeaders());

        return status;
    }

    private void checkFirstRedirection(HttpMethod method) {
        Header[] httpHeaders = method.getResponseHeaders();

        for (Header httpHeader : httpHeaders) {

            if ("location".equals(httpHeader.getName().toLowerCase())) {
                mRedirectedLocation = httpHeader.getValue();
                break;
            }
        }
    }

    /**
     * Fix for https://github.com/owncloud/android/issues/1847#issuecomment-267558274
     *
     * The problem: default SocketFactory in HTTPClient 3.x for HTTP connections creates a separate thread
     * to create the socket. When a port out of TCP bounds is passed, an exception is thrown in that
     * separate thread, and our original thread is not able to catch it. This is not happenning with HTTPS
     * connections because we had to define our own socket factory,
     * {@link com.owncloud.android.lib.common.network.AdvancedSslSocketFactory}, and it does not mess with
     * threads.
     *
     * The solution: validate the input (the port number) ourselves before let the work to HTTPClient 3.x.
     *
     * @param method HTTP method to run.
     * @throws IllegalArgumentException If 'method' targets an invalid port in an HTTP URI.
     * @throws URIException             If the URI to the target server cannot be built.
     */
    private void preventCrashDueToInvalidPort(HttpMethod method) throws URIException {
        int port = method.getURI().getPort();
        String scheme = method.getURI().getScheme().toLowerCase();
        if ("http".equals(scheme) && port > 0xFFFF) {
            // < 0 is not tested because -1 is used when no port number is specified in the URL;
            // no problem, the network library will convert that in the default HTTP port
            throw new IllegalArgumentException("Invalid port number " + port);
        }
    }


    public RedirectionPath followRedirection(HttpMethod method) throws IOException {
        int redirectionsCount = 0;
        int status = method.getStatusCode();
        RedirectionPath result = new RedirectionPath(status, MAX_REDIRECTIONS_COUNT);

        while (redirectionsCount < MAX_REDIRECTIONS_COUNT &&
            (status == HttpStatus.SC_MOVED_PERMANENTLY ||
                status == HttpStatus.SC_MOVED_TEMPORARILY ||
                status == HttpStatus.SC_TEMPORARY_REDIRECT)
            ) {

            Header location = method.getResponseHeader("Location");
            if (location == null) {
                location = method.getResponseHeader("location");
            }
            if (location != null) {
                String locationStr = location.getValue();

                Log_OC.d(TAG + " #" + mInstanceNumber,
                    "Location to redirect: " + locationStr);

                result.addLocation(locationStr);

                // Release the connection to avoid reach the max number of connections per host
                // due to it will be set a different url
                exhaustResponse(method.getResponseBodyAsStream());
                method.releaseConnection();

                method.setURI(new URI(locationStr, true));
                Header destination = method.getRequestHeader("Destination");
                if (destination == null) {
                    destination = method.getRequestHeader("destination");
                }
                if (destination != null) {
                    int suffixIndex = locationStr.lastIndexOf(WEBDAV_PATH_4_0);
                    String redirectionBase = locationStr.substring(0, suffixIndex);

                    String destinationStr = destination.getValue();
                    String destinationPath = destinationStr.substring(mBaseUri.toString().length());
                    String redirectedDestination = redirectionBase + destinationPath;

                    destination.setValue(redirectedDestination);
                    method.setRequestHeader(destination);
                }
                status = super.executeMethod(method);
                result.addStatus(status);
                redirectionsCount++;

            } else {
                Log_OC.d(TAG + " #" + mInstanceNumber, "No location to redirect!");
                status = HttpStatus.SC_NOT_FOUND;
            }
        }
        return result;
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

    /**
     * Sets the connection and wait-for-data timeouts to be applied by default to the methods
     * performed by this client.
     */
    public void setDefaultTimeouts(int defaultDataTimeout, int defaultConnectionTimeout) {
        if (defaultDataTimeout >= 0) {
            getParams().setSoTimeout(defaultDataTimeout);
        }
        if (defaultConnectionTimeout >= 0) {
            getHttpConnectionManager().getParams().setConnectionTimeout(defaultConnectionTimeout);
        }
    }

    public Uri getWebdavUri() {
        return Uri.parse(mBaseUri + WEBDAV_PATH_4_0);
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

    public void setFollowRedirects(boolean followRedirects) {
        mFollowRedirects = followRedirects;
    }

    public boolean getFollowRedirects() {
        return mFollowRedirects;
    }

    private void logCookiesAtRequest(Header[] headers, String when) {
        int counter = 0;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].getName().toLowerCase().equals("cookie")) {
                Log_OC.d(TAG + " #" + mInstanceNumber,
                    "Cookies at request (" + when + ") (" + counter++ + "): " +
                        headers[i].getValue());
            }
        }
        if (counter == 0) {
            Log_OC.d(TAG + " #" + mInstanceNumber, "No cookie at request before");
        }
    }

    private void logCookiesAtState(String string) {
        Cookie[] cookies = getState().getCookies();
        if (cookies.length == 0) {
            Log_OC.d(TAG + " #" + mInstanceNumber, "No cookie at STATE before");
        } else {
            Log_OC.d(TAG + " #" + mInstanceNumber, "Cookies at STATE (before)");
            for (int i = 0; i < cookies.length; i++) {
                Log_OC.d(TAG + " #" + mInstanceNumber, "    (" + i + "):" +
                    "\n        name: " + cookies[i].getName() +
                    "\n        value: " + cookies[i].getValue() +
                    "\n        domain: " + cookies[i].getDomain() +
                    "\n        path: " + cookies[i].getPath()
                );
            }
        }
    }

    private void logSetCookiesAtResponse(Header[] headers) {
        int counter = 0;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].getName().toLowerCase().equals("set-cookie")) {
                Log_OC.d(TAG + " #" + mInstanceNumber,
                    "Set-Cookie (" + counter++ + "): " + headers[i].getValue());
            }
        }
        if (counter == 0) {
            Log_OC.d(TAG + " #" + mInstanceNumber, "No set-cookie");
        }
    }

    public String getCookiesString() {
        Cookie[] cookies = getState().getCookies();
        String cookiesString = "";
        for (Cookie cookie : cookies) {
            cookiesString = cookiesString + cookie.toString() + ";";

            // logCookie(cookie);
        }

        return cookiesString;

    }

    public int getConnectionTimeout() {
        return getHttpConnectionManager().getParams().getConnectionTimeout();
    }

    public int getDataTimeout() {
        return getParams().getSoTimeout();
    }

    private void logCookie(Cookie cookie) {
        Log_OC.d(TAG, "Cookie name: " + cookie.getName());
        Log_OC.d(TAG, "       value: " + cookie.getValue());
        Log_OC.d(TAG, "       domain: " + cookie.getDomain());
        Log_OC.d(TAG, "       path: " + cookie.getPath());
        Log_OC.d(TAG, "       version: " + cookie.getVersion());
        Log_OC.d(TAG, "       expiryDate: " +
            (cookie.getExpiryDate() != null ? cookie.getExpiryDate().toString() : "--"));
        Log_OC.d(TAG, "       comment: " + cookie.getComment());
        Log_OC.d(TAG, "       secure: " + cookie.getSecure());
    }


    public void setOwnCloudVersion(OwnCloudVersion version) {
        mVersion = version;
    }

    public OwnCloudVersion getOwnCloudVersion() {
        return mVersion;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public void setAccount(OwnCloudAccount account) {
        this.mAccount = account;
    }

    public OwnCloudAccount getAccount() {
        return mAccount;
    }

    /**
     * Enables or disables silent refresh of credentials, if supported by credentials themselves.
     */
    public void setSilentRefreshOfAccountCredentials(boolean silentRefreshOfAccountCredentials) {
        mSilentRefreshOfAccountCredentials = silentRefreshOfAccountCredentials;
    }

    public boolean getSilentRefreshOfAccountCredentials() {
        return mSilentRefreshOfAccountCredentials;
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
                        mAccount.loadCredentials(mContext);
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
            // else: execute will finish with status 401
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

        boolean should = (httpStatusCode == HttpStatus.SC_UNAUTHORIZED || isIdPRedirection());   // invalid credentials

        should &= (mCredentials != null &&         // real credentials
            !(mCredentials instanceof OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials));

        // test if have all the needed to effectively invalidate ...
        should &= (mAccount != null && mAccount.getSavedAccount() != null && mContext != null);

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
        AccountManager am = AccountManager.get(mContext);
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
}
