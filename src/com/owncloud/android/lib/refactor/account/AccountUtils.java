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

package com.owncloud.android.lib.refactor.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.refactor.Log_OC;
import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.RemoteOperation;
import com.owncloud.android.lib.refactor.authentication.credentials.OCBasicCredentials;
import com.owncloud.android.lib.refactor.authentication.credentials.OCBearerCredentials;
import com.owncloud.android.lib.refactor.authentication.credentials.OCCredentials;
import com.owncloud.android.lib.refactor.authentication.credentials.OCSamlSsoCredentials;
import com.owncloud.android.lib.refactor.exceptions.AccountNotFoundException;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.apache.commons.httpclient.auth.AuthenticationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;

public class AccountUtils {

    private static final String TAG = AccountUtils.class.getSimpleName();

    /**
     * Constructs full url to host and webdav resource basing on host version
     *
     * @param context               Valid Android {@link Context}, needed to access the {@link AccountManager}
     * @param account               A stored ownCloud {@link Account}
     * @return                      Full URL to WebDAV endpoint in the server corresponding to 'account'.
     * @throws AccountNotFoundException When 'account' is unknown for the AccountManager
     */
    public static String getWebDavUrlForAccount(Context context, Account account)
        throws AccountNotFoundException {

        return getBaseUrlForAccount(context, account) + RemoteOperation.WEBDAV_PATH_4_0;
    }


    /**
     * Extracts url server from the account
     *
     * @param context               Valid Android {@link Context}, needed to access the {@link AccountManager}
     * @param account               A stored ownCloud {@link Account}
     * @return                      Full URL to the server corresponding to 'account', ending in the base path
     *                              common to all API endpoints.
     * @throws AccountNotFoundException When 'account' is unknown for the AccountManager
     */
    public static String getBaseUrlForAccount(Context context, Account account)
        throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context.getApplicationContext());
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);

        if (baseurl == null)
            throw new AccountNotFoundException(account, "Account not found", null);

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
            Log_OC.e(TAG, "Couldn't get a username for the given account", e);
        }
        return username;
    }

    /**
     * Get the stored server version corresponding to an OC account.
     *
     * @param account   An OC account
     * @param context   Application context
     * @return Version of the OC server, according to last check
     */
    public static OwnCloudVersion getServerVersionForAccount(Account account, Context context) {
        AccountManager ama = AccountManager.get(context);
        OwnCloudVersion version = null;
        try {
            String versionString = ama.getUserData(account, Constants.KEY_OC_VERSION);
            version = new OwnCloudVersion(versionString);

        } catch (Exception e) {
            Log_OC.e(TAG, "Couldn't get a the server version for an account", e);
        }
        return version;
    }

    /**
     * @return OCCredentials
     * @throws IOException
     * @throws AuthenticatorException
     * @throws OperationCanceledException
     */

    /**
     *
     * @param context an Android context
     * @param account the coresponding Android account
     * @return
     * @throws OperationCanceledException
     * @throws AuthenticatorException
     * @throws IOException
     */
    public static OCCredentials getCredentialsForAccount(Context context, Account account)
        throws OperationCanceledException, AuthenticatorException, IOException {

        final AccountManager am = AccountManager.get(context);
        final String supportsOAuth2 = am.getUserData(account, AccountUtils.Constants.KEY_SUPPORTS_OAUTH2);
        final boolean isOauth2 = supportsOAuth2 != null && supportsOAuth2.equals("TRUE");
        String supportsSamlSSo = am.getUserData(account,
                AccountUtils.Constants.KEY_SUPPORTS_SAML_WEB_SSO);
        final boolean isSamlSso = supportsSamlSSo != null && supportsSamlSSo.equals("TRUE");
        final String username = AccountUtils.getUsernameForAccount(account);

        if (isOauth2) {
            final String accessToken = am.blockingGetAuthToken(
                account,
                AccountTypeUtils.getAuthTokenTypeAccessToken(account.type),
                false);

            return new OCBearerCredentials(username, accessToken);

        } else if (isSamlSso) {

            try {
                final String accessToken = am.blockingGetAuthToken(
                        account,
                        AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(account.type),
                        false);

                return new OCSamlSsoCredentials(username, accessToken,
                        Uri.parse(getBaseUrlForAccount(context, account)));
            } catch (AccountNotFoundException e) {
                throw new AuthenticationException("Account not found", e);
            }

        } else {
            final String password = am.blockingGetAuthToken(
                account,
                AccountTypeUtils.getAuthTokenTypePass(account.type),
                false);

            return new OCBasicCredentials(username, password);
        }
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
        String accountName = username + "@" + url;

        return accountName;
    }

    public static void saveCookies(List<Cookie> cookies, Account savedAccount, Context context) {

        // Account Manager
        AccountManager ac = AccountManager.get(context.getApplicationContext());

        if (cookies != null && cookies.size() != 0) {
            StringBuilder cookiesString = new StringBuilder();
            for (Cookie cookie : cookies) {
                cookiesString.append(cookiesString + cookie.toString() + ";");
            }

            ac.setUserData(savedAccount, Constants.KEY_COOKIES, cookiesString.toString());
        }

    }


    /**
     *  Restore the client cookies persisted in an account stored in the system AccountManager.
     *
     * @param account
     * @param context
     * @return
     * @throws AccountsException
     */
    public static List<Cookie> getCookiesFromAccount(Account account, Context context) throws AccountsException {
        if (account == null) {
            Log_OC.d(TAG, "Cannot restore cookie for null account");
            return new ArrayList<>();
        }

        Log_OC.d(TAG, "Restoring cookies for " + account.name);

        final AccountManager am = AccountManager.get(context.getApplicationContext());
        final Uri serverUri = Uri.parse(getBaseUrlForAccount(context, account));
        final String cookiesString = am.getUserData(account, Constants.KEY_COOKIES);
        final List<Cookie> cookies = new ArrayList<>();

        if (cookiesString != null) {
            String[] rawCookies = cookiesString.split(";");
            for (String rawCookie : rawCookies) {
                final int equalPos = rawCookie.indexOf('=');

                cookies.add(new Cookie.Builder()
                        .name(rawCookie.substring(0, equalPos))
                        .value(rawCookie.substring(equalPos + 1))
                        .domain(serverUri.getHost())
                        .path(serverUri.getPath())
                        .build());
            }
        }
        return cookies;
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
         * Flag signaling if the ownCloud server can be accessed with session cookies from SAML-based web single-sign-on.
         */
        public static final String KEY_SUPPORTS_SAML_WEB_SSO = "oc_supports_saml_web_sso";
        /**
         * OC account cookies
         */
        public static final String KEY_COOKIES = "oc_account_cookies";

        /**
         * OC account version
         */
        public static final String KEY_OC_ACCOUNT_VERSION = "oc_account_version";

        /**
         * User's display name
         */
        public static final String KEY_DISPLAY_NAME = "oc_display_name";

        /**
         * OAuth2 refresh token
         **/
        public static final String KEY_OAUTH2_REFRESH_TOKEN = "oc_oauth2_refresh_token";

    }

}
