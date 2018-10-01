/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
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
import android.util.Log;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.authentication.OwnCloudSamlSsoCredentials;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of {@link OwnCloudClientManager}
 *
 * TODO check multithreading safety
 *
 * @author David A. Velasco
 * @author masensio
 * @author Christian Schabesberger
 * @author David González Verdugo
 */

public class SingleSessionManager implements OwnCloudClientManager {

    private static final String TAG = SingleSessionManager.class.getSimpleName();

    private ConcurrentMap<String, OwnCloudClient> mClientsWithKnownUsername =
        new ConcurrentHashMap<>();

    private ConcurrentMap<String, OwnCloudClient> mClientsWithUnknownUsername =
        new ConcurrentHashMap<>();


    @Override
    public OwnCloudClient getClientFor(OwnCloudAccount account, Context context) throws OperationCanceledException,
            AuthenticatorException, IOException {

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log_OC.d(TAG, "getClientFor starting ");
        }
        if (account == null) {
            throw new IllegalArgumentException("Cannot get an OwnCloudClient for a null account");
        }

        OwnCloudClient client = null;
        String accountName = account.getName();
        String sessionName = account.getCredentials() == null ? "" :
            AccountUtils.buildAccountName(
                account.getBaseUri(),
                account.getCredentials().getAuthToken());

        if (accountName != null) {
            client = mClientsWithKnownUsername.get(accountName);
        }
        boolean reusingKnown = false;    // just for logs
        if (client == null) {
            if (accountName != null) {
                client = mClientsWithUnknownUsername.remove(sessionName);
                if (client != null) {
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log_OC.v(TAG, "reusing client for session " + sessionName);
                    }
                    mClientsWithKnownUsername.put(accountName, client);
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log_OC.v(TAG, "moved client to account " + accountName);
                    }
                }
            } else {
                client = mClientsWithUnknownUsername.get(sessionName);
            }
        } else {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log_OC.v(TAG, "reusing client for account " + accountName);
            }
            reusingKnown = true;
        }

        if (client == null) {
            // no client to reuse - create a new one
            client = OwnCloudClientFactory.createOwnCloudClient(
                account.getBaseUri(),
                context.getApplicationContext(),
                true);    // TODO remove dependency on OwnCloudClientFactory
            client.setAccount(account);
            client.setContext(context);
            client.setOwnCloudClientManager(this);

            account.loadCredentials(context);
            client.setCredentials(account.getCredentials());

            if (client.getCredentials() instanceof OwnCloudSamlSsoCredentials) {
                client.disableAutomaticCookiesHandling();
            }

            if (accountName != null) {
                mClientsWithKnownUsername.put(accountName, client);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log_OC.v(TAG, "new client for account " + accountName);
                }

            } else {
                mClientsWithUnknownUsername.put(sessionName, client);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log_OC.v(TAG, "new client for session " + sessionName);
                }
            }
        } else {
            if (!reusingKnown && Log.isLoggable(TAG, Log.VERBOSE)) {
                Log_OC.v(TAG, "reusing client for session " + sessionName);
            }

            keepCredentialsUpdated(client);
            keepCookiesUpdated(context, account, client);
            keepUriUpdated(account, client);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log_OC.d(TAG, "getClientFor finishing ");
        }
        return client;
    }


    @Override
    public OwnCloudClient removeClientFor(OwnCloudAccount account) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log_OC.d(TAG, "removeClientFor starting ");
        }

        if (account == null) {
            return null;
        }

        OwnCloudClient client;
        String accountName = account.getName();
        if (accountName != null) {
            client = mClientsWithKnownUsername.remove(accountName);
            if (client != null) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log_OC.v(TAG, "Removed client for account " + accountName);
                }
                return client;
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log_OC.v(TAG, "No client tracked for  account " + accountName);
                }
            }
        }

        mClientsWithUnknownUsername.clear();

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log_OC.d(TAG, "removeClientFor finishing ");
        }
        return null;
    }


    @Override
    public void saveAllClients(Context context, String accountType) {

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log_OC.d(TAG, "Saving sessions... ");
        }

        Iterator<String> accountNames = mClientsWithKnownUsername.keySet().iterator();
        String accountName;
        Account account;
        while (accountNames.hasNext()) {
            accountName = accountNames.next();
            account = new Account(accountName, accountType);
            AccountUtils.saveClient(
                mClientsWithKnownUsername.get(accountName),
                account,
                context);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log_OC.d(TAG, "All sessions saved");
        }
    }

    private void keepCredentialsUpdated(OwnCloudClient reusedClient) {
        reusedClient.applyCredentials();
    }

    private void keepCookiesUpdated(Context context, OwnCloudAccount account, OwnCloudClient reusedClient) {
        AccountManager am = AccountManager.get(context.getApplicationContext());
        if (am != null && account.getSavedAccount() != null) {
            String recentCookies = am.getUserData(account.getSavedAccount(), AccountUtils.Constants.KEY_COOKIES);
            String previousCookies = reusedClient.getCookiesString();
            if (recentCookies != null && previousCookies != "" && !recentCookies.equals(previousCookies)) {
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