/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.http.HttpClient;
import timber.log.Timber;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author David A. Velasco
 * @author masensio
 * @author Christian Schabesberger
 * @author David González Verdugo
 */

public class SingleSessionManager {

    private static SingleSessionManager sDefaultSingleton;
    private static String sUserAgent;

    private ConcurrentMap<String, OwnCloudClient> mClientsWithKnownUsername = new ConcurrentHashMap<>();
    private ConcurrentMap<String, OwnCloudClient> mClientsWithUnknownUsername = new ConcurrentHashMap<>();

    public static SingleSessionManager getDefaultSingleton() {
        if (sDefaultSingleton == null) {
            sDefaultSingleton = new SingleSessionManager();
        }
        return sDefaultSingleton;
    }

    public static String getUserAgent() {
        return sUserAgent;
    }

    public static void setUserAgent(String userAgent) {
        sUserAgent = userAgent;
    }

    public OwnCloudClient getClientFor(OwnCloudAccount account, Context context) throws OperationCanceledException,
            AuthenticatorException, IOException {

        Timber.d("getClientFor starting ");
        if (account == null) {
            throw new IllegalArgumentException("Cannot get an OwnCloudClient for a null account");
        }

        OwnCloudClient client = null;
        String accountName = account.getName();
        String sessionName = account.getCredentials() == null ? "" :
                AccountUtils.buildAccountName(account.getBaseUri(), account.getCredentials().getAuthToken());

        if (accountName != null) {
            client = mClientsWithKnownUsername.get(accountName);
        }
        boolean reusingKnown = false;    // just for logs
        if (client == null) {
            if (accountName != null) {
                client = mClientsWithUnknownUsername.remove(sessionName);
                if (client != null) {
                    Timber.v("reusing client for session %s", sessionName);

                    mClientsWithKnownUsername.put(accountName, client);
                    Timber.v("moved client to account %s", accountName);
                }
            } else {
                client = mClientsWithUnknownUsername.get(sessionName);
            }
        } else {
            Timber.v("reusing client for account %s", accountName);
            reusingKnown = true;
        }

        if (client == null) {
            // no client to reuse - create a new one
            client = OwnCloudClientFactory.createOwnCloudClient(
                    account.getBaseUri(),
                    context.getApplicationContext(),
                    true);    // TODO remove dependency on OwnCloudClientFactory
            client.setAccount(account);
            HttpClient.setContext(context);

            account.loadCredentials(context);
            client.setCredentials(account.getCredentials());

            if (accountName != null) {
                mClientsWithKnownUsername.put(accountName, client);
                Timber.v("new client for account %s", accountName);

            } else {
                mClientsWithUnknownUsername.put(sessionName, client);
                Timber.v("new client for session %s", sessionName);
            }
        } else {
            if (!reusingKnown) {
                Timber.v("reusing client for session %s", sessionName);
            }

            keepCredentialsUpdated(client);
            keepCookiesUpdated(context, account, client);
            keepUriUpdated(account, client);
        }
        Timber.d("getClientFor finishing ");
        return client;
    }

    public void removeClientFor(OwnCloudAccount account) {
        Timber.d("removeClientFor starting ");

        if (account == null) {
            return;
        }

        OwnCloudClient client;
        String accountName = account.getName();
        if (accountName != null) {
            client = mClientsWithKnownUsername.remove(accountName);
            if (client != null) {
                Timber.v("Removed client for account %s", accountName);
                return;
            } else {
                Timber.v("No client tracked for  account %s", accountName);
            }
        }

        mClientsWithUnknownUsername.clear();

        Timber.d("removeClientFor finishing ");
    }

    public void saveAllClients(Context context, String accountType) {
        Timber.d("Saving sessions... ");

        Iterator<String> accountNames = mClientsWithKnownUsername.keySet().iterator();
        String accountName;
        Account account;
        while (accountNames.hasNext()) {
            accountName = accountNames.next();
            account = new Account(accountName, accountType);
            AccountUtils.saveClient(mClientsWithKnownUsername.get(accountName), account, context);
        }

        Timber.d("All sessions saved");
    }

    private void keepCredentialsUpdated(OwnCloudClient reusedClient) {
        reusedClient.applyCredentials();
    }

    private void keepCookiesUpdated(Context context, OwnCloudAccount account, OwnCloudClient reusedClient) {
        AccountManager am = AccountManager.get(context.getApplicationContext());
        if (am != null && account.getSavedAccount() != null) {
            String recentCookies = am.getUserData(account.getSavedAccount(), AccountUtils.Constants.KEY_COOKIES);
            String previousCookies = reusedClient.getCookiesString();
            if (recentCookies != null && !previousCookies.equals("") && !recentCookies.equals(previousCookies)) {
                AccountUtils.restoreCookies(account.getSavedAccount(), reusedClient, context);
            }
        }
    }

    // this method is just a patch; we need to distinguish accounts in the same host but
    // different paths; but that requires updating the accountNames for apps upgrading
    private void keepUriUpdated(OwnCloudAccount account, OwnCloudClient reusedClient) {
        Uri recentUri = account.getBaseUri();
        if (!recentUri.equals(reusedClient.getBaseUri())) {
            reusedClient.setBaseUri(recentUri);
        }
    }
}
