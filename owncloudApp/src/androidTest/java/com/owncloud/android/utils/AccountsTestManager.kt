/**
 * ownCloud Android client application
 *
 *
 * Copyright (C) 2016 ownCloud GmbH.
 *
 *
 *
 *
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.SystemClock
import com.owncloud.android.authentication.AccountAuthenticator
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.status.OwnCloudVersion

object AccountsTestManager {
    private const val accountType = "owncloud"
    private const val KEY_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE"
    private const val KEY_AUTH_TOKEN = "AUTH_TOKEN"
    private const val version = ""
    private const val WAIT_UNTIL_ACCOUNT_CREATED_MS = 1000
    private const val HTTP_SCHEME = "http://"
    private const val HTTPS_SCHEME = "https://"

    fun addAccount(
        context: Context?,
        account: Account,
        password: String?
    ) { // obtaining an AccountManager instance
        // obtaining an AccountManager instance
        val accountManager = AccountManager.get(context)

        accountManager.addAccountExplicitly(account, password, null)

        // include account version, user, server version and token with the new account
        accountManager.setUserData(
            account,
            AccountUtils.Constants.KEY_OC_VERSION,
            OwnCloudVersion("10.2").toString()
        )
        accountManager.setUserData(
            account,
            AccountUtils.Constants.KEY_OC_BASE_URL,
            "serverUrl:port"
        )
        accountManager.setUserData(
            account,
            AccountUtils.Constants.KEY_DISPLAY_NAME,
            "admin"
        )
        accountManager.setUserData(
            account,
            AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
            "1"
        )

        accountManager.setAuthToken(
            account,
            AccountAuthenticator.KEY_AUTH_TOKEN_TYPE,
            "AUTH_TOKEN"
        )
    }

    //Remove an account from the device
    fun deleteAccount(context: Context?, accountDel: String?) {
        val accountManager = AccountManager.get(context)
        val account = Account(accountDel, accountType)
        accountManager.removeAccount(account, null, null)
    }

    //Remove all accounts from the device
    fun deleteAllAccounts(context: Context?) {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.accounts
        for (account in accounts) {
            if (account.type.compareTo(accountType) == 0) {
                accountManager.removeAccount(account, null, null)
                SystemClock.sleep(WAIT_UNTIL_ACCOUNT_CREATED_MS.toLong())
            }
        }
    }
}
