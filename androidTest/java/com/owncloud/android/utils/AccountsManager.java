/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.accounts.AuthenticatorException;

import com.owncloud.android.authentication.AccountAuthenticator;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.operations.DetectAuthenticationMethodOperation;
import com.owncloud.android.operations.GetServerInfoOperation;
import com.owncloud.android.lib.common.OwnCloudAccount;


public class AccountsManager {

    private static String accountType = "owncloud";
    private static final String KEY_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE";
    private static final String KEY_AUTH_TOKEN = "AUTH_TOKEN";
    private static String version = "";
    private static int WAIT_UNTIL_ACCOUNT_CREATED = 1000;

    public static void addAccount(Context context, String baseUrl, String username, String password)
            throws AuthenticatorException {

        // obtaining an AccountManager instance
        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account(username+"@"+baseUrl, accountType);
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

    //
    public static void deleteAccount(Context context, String accountDel) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account(accountDel, accountType);
        accountManager.removeAccount(account,null,null);
    }

    public static void deleteAllAccounts(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccounts();
        for (int i=0;i<accounts.length;i++){
            if (accounts[i].type.compareTo(accountType)==0) {
                accountManager.removeAccount(accounts[i], null, null);
                SystemClock.sleep(WAIT_UNTIL_ACCOUNT_CREATED);
            }
        }
    }
}
