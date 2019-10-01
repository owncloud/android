/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2016 ownCloud GmbH.
 * <p>
 * <p>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.SystemClock;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.resources.status.RemoteCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

public class AccountsManager {

    private static String accountType = "owncloud";
    private static final String KEY_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE";
    private static final String KEY_AUTH_TOKEN = "AUTH_TOKEN";
    private static String version = "";
    private static int WAIT_UNTIL_ACCOUNT_CREATED_MS = 1000;
    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";

    public static void addAccount(Context context, String baseUrl, String username, String password) {

        // obtaining an AccountManager instance
        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account(username + "@" + regularURL(baseUrl), accountType);
        accountManager.addAccountExplicitly(account, password, null);

        // include account version, user, server version and token with the new account
        accountManager.setUserData(
                account,
                AccountUtils.Constants.KEY_OC_VERSION,
                new OwnCloudVersion(version).toString()
        );
        accountManager.setUserData(
                account,
                AccountUtils.Constants.KEY_OC_BASE_URL,
                baseUrl
        );
        accountManager.setUserData(
                account,
                AccountUtils.Constants.KEY_DISPLAY_NAME,
                username
        );

        accountManager.setAuthToken(
                account,
                KEY_AUTH_TOKEN_TYPE,
                KEY_AUTH_TOKEN
        );

    }

    //Remove an account from the device
    public static void deleteAccount(Context context, String accountDel) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account(accountDel, accountType);
        accountManager.removeAccount(account, null, null);
    }

    //Remove all accounts from the device
    public static void deleteAllAccounts(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccounts();
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].type.compareTo(accountType) == 0) {
                accountManager.removeAccount(accounts[i], null, null);
                SystemClock.sleep(WAIT_UNTIL_ACCOUNT_CREATED_MS);
            }
        }
    }

    //To regularize the URL to build the URL inserted in the account device
    public static String regularURL(String url) {
        String url_regularized = null;
        if (url.startsWith(HTTPS_SCHEME)) {
            url_regularized = url.substring(8, url.length()); //skipping the protocol
        } else if (url.startsWith(HTTP_SCHEME)) {
            url_regularized = url.substring(7, url.length()); //skipping the protocol
        } else {
            return url;
        }
        return url_regularized;
    }

    //Get server capabilities
    public static RemoteCapability getCapabilities(String server, String user, String pass) {
        //REDO -> need mocks or integration with new networking stuff
        return new RemoteCapability();

    }

    //Save capabilities (in device DB)
    public static void saveCapabilities(Context context, RemoteCapability capabilities, String server, String user) {
        FileDataStorageManager fm = new FileDataStorageManager(context, new Account(buildAccountName(user, server),
                accountType),
                MainApp.Companion.getAppContext().getContentResolver());
        fm.saveCapabilities(capabilities);
    }

    //Build account name
    private static String buildAccountName(String user, String server) {
        return user + "@" + regularURL(server);
    }

}
