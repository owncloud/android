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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.http.HttpClient;
import timber.log.Timber;

import java.io.IOException;
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
    private static ConnectionValidator sConnectionValidator;

    private ConcurrentMap<String, OwnCloudClient> mClientsWithKnownUsername = new ConcurrentHashMap<>();
    private ConcurrentMap<String, OwnCloudClient> mClientsWithUnknownUsername = new ConcurrentHashMap<>();

    public static SingleSessionManager getDefaultSingleton() {
        if (sDefaultSingleton == null) {
            sDefaultSingleton = new SingleSessionManager();
        }
        return sDefaultSingleton;
    }

    public static void setConnectionValidator(ConnectionValidator connectionValidator) {
        sConnectionValidator = connectionValidator;
    }

    public static ConnectionValidator getConnectionValidator() {
        return sConnectionValidator;
    }

    public static String getUserAgent() {
        return sUserAgent;
    }

    public static void setUserAgent(String userAgent) {
        sUserAgent = userAgent;
    }

    private static OwnCloudClient createOwnCloudClient(Uri uri, Context context, boolean followRedirects, ConnectionValidator connectionValidator) {
        OwnCloudClient client = new OwnCloudClient(uri, connectionValidator, true);
        client.setFollowRedirects(followRedirects);
        HttpClient.setContext(context);

        return client;
    }

    public OwnCloudClient getClientFor(OwnCloudAccount account,
                                       Context context) throws OperationCanceledException,
            AuthenticatorException, IOException {
        return getClientFor(account, context, getConnectionValidator());
    }

    public OwnCloudClient getClientFor(OwnCloudAccount account,
                                       Context context,
                                       ConnectionValidator connectionValidator) throws OperationCanceledException,
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
            client = createOwnCloudClient(
                    account.getBaseUri(),
                    context.getApplicationContext(),
                    true,
                    connectionValidator);    // TODO remove dependency on OwnCloudClientFactory

            //the next two lines are a hack because okHttpclient is used as a singleton instead of being an
            //injected instance that can be deleted when required
            client.clearCookies();
            client.clearCredentials();

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

    public void refreshCredentialsForAccount(String accountName, OwnCloudCredentials credentials) {
        OwnCloudClient ownCloudClient = mClientsWithKnownUsername.get(accountName);
        if (ownCloudClient == null) {
            return;
        }
        ownCloudClient.setCredentials(credentials);
        mClientsWithKnownUsername.replace(accountName, ownCloudClient);
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
