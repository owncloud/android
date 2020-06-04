/**
 * ownCloud Android client application
 *
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.capabilities.model.OCCapability;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import timber.log.Timber;

import java.util.Locale;

import static com.owncloud.android.data.authentication.AuthenticationConstantsKt.SELECTED_ACCOUNT;
import static com.owncloud.android.lib.common.accounts.AccountUtils.Constants.OAUTH_SUPPORTED_TRUE;

public class AccountUtils {

    private static final int ACCOUNT_VERSION = 1;

    /**
     * Can be used to get the currently selected ownCloud {@link Account} in the
     * application preferences.
     *
     * @param   context     The current application {@link Context}
     * @return The ownCloud {@link Account} currently saved in preferences, or the first
     *                      {@link Account} available, if valid (still registered in the system as ownCloud 
     *                      account). If none is available and valid, returns null.
     */
    public static Account getCurrentOwnCloudAccount(Context context) {
        Account[] ocAccounts = getAccounts(context);
        Account defaultAccount = null;

        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accountName = appPreferences.getString(SELECTED_ACCOUNT, null);

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        if (accountName != null) {
            for (Account account : ocAccounts) {
                if (account.name.equals(accountName)) {
                    defaultAccount = account;
                    break;
                }
            }
        }

        if (defaultAccount == null && ocAccounts.length != 0) {
            // take first account as fallback
            defaultAccount = ocAccounts[0];
        }

        return defaultAccount;
    }

    public static Account[] getAccounts(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        return accountManager.getAccountsByType(MainApp.Companion.getAccountType());
    }

    public static boolean exists(String accountName, Context context) {
        Account[] ocAccounts = getAccounts(context);

        if (accountName != null) {
            int lastAtPos = accountName.lastIndexOf("@");
            String hostAndPort = accountName.substring(lastAtPos + 1);
            String username = accountName.substring(0, lastAtPos);
            String otherHostAndPort, otherUsername;
            Locale currentLocale = context.getResources().getConfiguration().locale;
            for (Account otherAccount : ocAccounts) {
                lastAtPos = otherAccount.name.lastIndexOf("@");
                otherHostAndPort = otherAccount.name.substring(lastAtPos + 1);
                otherUsername = otherAccount.name.substring(0, lastAtPos);
                if (otherHostAndPort.equals(hostAndPort) &&
                        otherUsername.toLowerCase(currentLocale).
                                equals(username.toLowerCase(currentLocale))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * returns the user's name based on the account name.
     *
     * @param accountName the account name
     * @return the user's name
     */
    public static String getUsernameOfAccount(String accountName) {
        if (accountName != null) {
            return accountName.substring(0, accountName.lastIndexOf("@"));
        } else {
            return null;
        }
    }

    /**
     * Returns owncloud account identified by accountName or null if it does not exist.
     * @param context
     * @param accountName name of account to be returned
     * @return owncloud account named accountName
     */
    public static Account getOwnCloudAccountByName(Context context, String accountName) {
        Account[] ocAccounts = AccountManager.get(context).getAccountsByType(
                MainApp.Companion.getAccountType());
        for (Account account : ocAccounts) {
            if (account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }

    public static boolean setCurrentOwnCloudAccount(Context context, String accountName) {
        boolean result = false;
        if (accountName != null) {
            boolean found;
            for (Account account : getAccounts(context)) {
                found = (account.name.equals(accountName));
                if (found) {
                    SharedPreferences.Editor appPrefs = PreferenceManager
                            .getDefaultSharedPreferences(context).edit();
                    appPrefs.putString(SELECTED_ACCOUNT, accountName);

                    appPrefs.apply();
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Update the accounts in AccountManager to meet the current version of accounts expected by the app, if needed.
     *
     * Introduced to handle a change in the structure of stored account names needed to allow different OC servers
     * in the same domain, but not in the same path.
     *
     * @param   context     Used to access the AccountManager.
     */
    public static void updateAccountVersion(Context context) {
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(context);
        AccountManager accountMgr = AccountManager.get(context);

        if (currentAccount != null) {
            String currentAccountVersion = accountMgr.getUserData(currentAccount, Constants.KEY_OC_ACCOUNT_VERSION);

            if (currentAccountVersion == null) {
                Timber.i("Upgrading accounts to account version #%s", ACCOUNT_VERSION);
                Account[] ocAccounts = accountMgr.getAccountsByType(MainApp.Companion.getAccountType());
                String serverUrl, username, newAccountName, password;
                Account newAccount;
                for (Account account : ocAccounts) {
                    // build new account name
                    serverUrl = accountMgr.getUserData(account, Constants.KEY_OC_BASE_URL);
                    username = com.owncloud.android.lib.common.accounts.AccountUtils.
                            getUsernameForAccount(account);
                    newAccountName = com.owncloud.android.lib.common.accounts.AccountUtils.
                            buildAccountName(Uri.parse(serverUrl), username);

                    // migrate to a new account, if needed
                    if (!newAccountName.equals(account.name)) {
                        Timber.d("Upgrading " + account.name + " to " + newAccountName);

                        // create the new account
                        newAccount = new Account(newAccountName, MainApp.Companion.getAccountType());
                        password = accountMgr.getPassword(account);
                        accountMgr.addAccountExplicitly(newAccount, (password != null) ? password : "", null);

                        // copy base URL
                        accountMgr.setUserData(newAccount, Constants.KEY_OC_BASE_URL, serverUrl);

                        // copy server version
                        accountMgr.setUserData(
                                newAccount,
                                Constants.KEY_OC_VERSION,
                                accountMgr.getUserData(account, Constants.KEY_OC_VERSION)
                        );

                        // copy cookies
                        accountMgr.setUserData(
                                newAccount,
                                Constants.KEY_COOKIES,
                                accountMgr.getUserData(account, Constants.KEY_COOKIES)
                        );

                        String isOauthStr = accountMgr.getUserData(account, Constants.KEY_SUPPORTS_OAUTH2);
                        boolean isOAuth = OAUTH_SUPPORTED_TRUE.equals(isOauthStr);
                        if (isOAuth) {
                            accountMgr.setUserData(newAccount, Constants.KEY_SUPPORTS_OAUTH2, OAUTH_SUPPORTED_TRUE);
                        }

                        // don't forget the account saved in preferences as the current one
                        if (currentAccount.name.equals(account.name)) {
                            AccountUtils.setCurrentOwnCloudAccount(context, newAccountName);
                        }

                        // remove the old account
                        accountMgr.removeAccount(account, null, null);
                        // will assume it succeeds, not a big deal otherwise

                    } else {
                        // servers which base URL is in the root of their domain need no change
                        Timber.d("%s needs no upgrade ", account.name);
                        newAccount = account;
                    }

                    // at least, upgrade account version
                    Timber.d("Setting version " + ACCOUNT_VERSION + " to " + newAccountName);
                    accountMgr.setUserData(
                            newAccount, Constants.KEY_OC_ACCOUNT_VERSION, Integer.toString(ACCOUNT_VERSION)
                    );

                }
            }
        }
    }

    /**
     * Access the version of the OC server corresponding to an account SAVED IN THE ACCOUNTMANAGER
     *
     * @param   account     ownCloud account
     * @return Version of the OC server corresponding to account, according to the data saved
     *                      in the system AccountManager
     */
    @Nullable
    public static OwnCloudVersion getServerVersion(Account account) {
        OwnCloudVersion serverVersion = null;
        if (account != null) {
            // capabilities are now the preferred source for version info
            FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(
                    MainApp.Companion.getAppContext(),
                    account,
                    MainApp.Companion.getAppContext().getContentResolver()
            );
            OCCapability capability = fileDataStorageManager.getCapability(account.name);
            if (capability != null) {
                serverVersion = new OwnCloudVersion(capability.getVersionString());
            } else {
                // legacy: AccountManager as source of version info
                AccountManager accountMgr = AccountManager.get(MainApp.Companion.getAppContext());
                String serverVersionStr = accountMgr.getUserData(account, Constants.KEY_OC_VERSION);
                if (serverVersionStr != null) {
                    serverVersion = new OwnCloudVersion(serverVersionStr);
                }
            }
        }
        return serverVersion;
    }
}
