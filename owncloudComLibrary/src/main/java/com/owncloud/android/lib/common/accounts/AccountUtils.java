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

package com.owncloud.android.lib.common.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import okhttp3.Cookie;
import timber.log.Timber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AccountUtils {

    /**
     * Constructs full url to host and webdav resource basing on host version
     *
     * @param context Valid Android {@link Context}, needed to access the {@link AccountManager}
     * @param account A stored ownCloud {@link Account}
     * @return Full URL to WebDAV endpoint in the server corresponding to 'account'.
     * @throws AccountNotFoundException When 'account' is unknown for the AccountManager
     */
    public static String getWebDavUrlForAccount(Context context, Account account)
            throws AccountNotFoundException {
        String webDavUrlForAccount = "";

        try {
            OwnCloudCredentials ownCloudCredentials = getCredentialsForAccount(context, account);
            webDavUrlForAccount = getBaseUrlForAccount(context, account) + OwnCloudClient.WEBDAV_FILES_PATH_4_0
                    + ownCloudCredentials.getUsername();
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            Timber.e(e);
        }

        return webDavUrlForAccount;
    }

    /**
     * Extracts url server from the account
     *
     * @param context Valid Android {@link Context}, needed to access the {@link AccountManager}
     * @param account A stored ownCloud {@link Account}
     * @return Full URL to the server corresponding to 'account', ending in the base path
     * common to all API endpoints.
     * @throws AccountNotFoundException When 'account' is unknown for the AccountManager
     */
    public static String getBaseUrlForAccount(Context context, Account account)
            throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context.getApplicationContext());
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);

        if (baseurl == null) {
            throw new AccountNotFoundException(account, "Account not found", null);
        }

        return baseurl;
    }

    /**
     * Get the username corresponding to an OC account.
     *
     * @param account An OC account
     * @return Username for the given account, extracted from the account.name
     */
    public static String getUsernameForAccount(Account account) {
        String username = null;
        try {
            username = account.name.substring(0, account.name.lastIndexOf('@'));
        } catch (Exception e) {
            Timber.e(e, "Couldn't get a username for the given account");
        }
        return username;
    }

    /**
     * Get the stored server version corresponding to an OC account.
     *
     * @param account An OC account
     * @param context Application context
     * @return Version of the OC server, according to last check
     */
    public static OwnCloudVersion getServerVersionForAccount(Account account, Context context) {
        AccountManager ama = AccountManager.get(context);
        OwnCloudVersion version = null;
        try {
            String versionString = ama.getUserData(account, Constants.KEY_OC_VERSION);
            version = new OwnCloudVersion(versionString);

        } catch (Exception e) {
            Timber.e(e, "Couldn't get a the server version for an account");
        }
        return version;
    }

    /**
     * @return
     * @throws IOException
     * @throws AuthenticatorException
     * @throws OperationCanceledException
     */
    public static OwnCloudCredentials getCredentialsForAccount(Context context, Account account)
            throws OperationCanceledException, AuthenticatorException, IOException {

        OwnCloudCredentials credentials;
        AccountManager am = AccountManager.get(context);

        String supportsOAuth2 = am.getUserData(account, AccountUtils.Constants.KEY_SUPPORTS_OAUTH2);
        boolean isOauth2 = supportsOAuth2 != null && supportsOAuth2.equals("TRUE");

        String username = AccountUtils.getUsernameForAccount(account);

        if (isOauth2) {
            String accessToken = am.blockingGetAuthToken(
                    account,
                    AccountTypeUtils.getAuthTokenTypeAccessToken(account.type),
                    false);

            credentials = OwnCloudCredentialsFactory.newBearerCredentials(username, accessToken);
        } else {
            String password = am.blockingGetAuthToken(
                    account,
                    AccountTypeUtils.getAuthTokenTypePass(account.type),
                    false);

            credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    username,
                    password
            );
        }

        return credentials;
    }

    /**
     * Get the user id corresponding to an OC account.
     *
     * @param account ownCloud account
     * @return user id
     */
    public static String getUserId(Account account, Context context) {
        AccountManager accountMgr = AccountManager.get(context);
        return accountMgr.getUserData(account, Constants.KEY_ID);
    }

    public static String buildAccountNameOld(Uri serverBaseUrl, String username) {
        if (serverBaseUrl.getScheme() == null) {
            serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString());
        }
        String accountName = username + "@" + serverBaseUrl.getHost();
        if (serverBaseUrl.getPort() >= 0) {
            accountName += ":" + serverBaseUrl.getPort();
        }
        return accountName;
    }

    public static String buildAccountName(Uri serverBaseUrl, String username) {
        if (serverBaseUrl.getScheme() == null) {
            serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString());
        }

        // Remove http:// or https://
        String url = serverBaseUrl.toString();
        if (url.contains("://")) {
            url = url.substring(serverBaseUrl.toString().indexOf("://") + 3);
        }

        return username + "@" + url;
    }

    public static void saveClient(OwnCloudClient client, Account savedAccount, Context context) {
        // Account Manager
        AccountManager ac = AccountManager.get(context.getApplicationContext());

        if (client != null) {
            String cookiesString = client.getCookiesString();
            if (!"".equals(cookiesString)) {
                ac.setUserData(savedAccount, Constants.KEY_COOKIES, cookiesString);
                Timber.d("Saving Cookies: %s", cookiesString);
            }
        }
    }

    /**
     * Restore the client cookies persisted in an account stored in the system AccountManager.
     *
     * @param account Stored account.
     * @param client  Client to restore cookies in.
     * @param context Android context used to access the system AccountManager.
     */
    public static void restoreCookies(Account account, OwnCloudClient client, Context context) {
        if (account == null) {
            Timber.d("Cannot restore cookie for null account");

        } else {
            Timber.d("Restoring cookies for %s", account.name);

            // Account Manager
            AccountManager am = AccountManager.get(context.getApplicationContext());

            Uri serverUri = (client.getBaseUri() != null) ? client.getBaseUri() : client.getUserFilesWebDavUri();

            String cookiesString = am.getUserData(account, Constants.KEY_COOKIES);
            if (cookiesString != null) {
                String[] rawCookies = cookiesString.split(";");
                List<Cookie> cookieList = new ArrayList<>(rawCookies.length);
                for (String rawCookie : rawCookies) {
                    rawCookie = rawCookie.replace(" ", "");
                    final int equalPos = rawCookie.indexOf('=');
                    if (equalPos == -1) {
                        continue;
                    }
                    cookieList.add(new Cookie.Builder()
                            .name(rawCookie.substring(0, equalPos))
                            .value(rawCookie.substring(equalPos + 1))
                            .domain(serverUri.getHost())
                            .path(
                                    serverUri.getPath().equals("")
                                            ? FileUtils.PATH_SEPARATOR
                                            : serverUri.getPath()
                            )
                            .build());
                }
                client.setCookiesForCurrentAccount(cookieList);
            }
        }
    }

    public static class AccountNotFoundException extends AccountsException {

        /**
         * Generated - should be refreshed every time the class changes!!
         */
        private static final long serialVersionUID = -1684392454798508693L;

        private Account mFailedAccount;

        public AccountNotFoundException(Account failedAccount, String message, Throwable cause) {
            super(message, cause);
            mFailedAccount = failedAccount;
        }

        public Account getFailedAccount() {
            return mFailedAccount;
        }
    }

    public static class Constants {
        /**
         * Version should be 3 numbers separated by dot so it can be parsed by
         * {@link OwnCloudVersion}
         */
        public static final String KEY_OC_VERSION = "oc_version";
        /**
         * Base url should point to owncloud installation without trailing / ie:
         * http://server/path or https://owncloud.server
         */
        public static final String KEY_OC_BASE_URL = "oc_base_url";
        /**
         * Flag signaling if the ownCloud server can be accessed with OAuth2 access tokens.
         */
        public static final String KEY_SUPPORTS_OAUTH2 = "oc_supports_oauth2";

        /**
         * OC account cookies
         */
        public static final String KEY_COOKIES = "oc_account_cookies";

        /**
         * OC account version
         */
        public static final String KEY_OC_ACCOUNT_VERSION = "oc_account_version";

        /**
         * User's id
         */
        public static final String KEY_ID = "oc_id";

        /**
         * User's display name
         */
        public static final String KEY_DISPLAY_NAME = "oc_display_name";

        /**
         * OAuth2 user id
         **/
        public static final String KEY_USER_ID = "user_id";

        /**
         * OAuth2 refresh token
         **/
        public static final String KEY_OAUTH2_REFRESH_TOKEN = "oc_oauth2_refresh_token";

        /**
         * OAuth2 scope
         */
        public static final String KEY_OAUTH2_SCOPE = "oc_oauth2_scope";
    }
}
