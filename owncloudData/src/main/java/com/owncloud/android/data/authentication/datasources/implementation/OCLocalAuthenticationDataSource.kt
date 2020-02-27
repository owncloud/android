/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.authentication.datasources.implementation

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.domain.exceptions.AccountNotNewException
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.WebdavUtils
import timber.log.Timber
import java.util.Locale

class OCLocalAuthenticationDataSource(
    private val context: Context,
    private val accountManager: AccountManager,
    private val accountType: String
) : LocalAuthenticationDataSource {

    override fun addAccountIfDoesNotExist(
        lastPermanentLocation: String?,
        userName: String,
        password: String,
        serverInfo: ServerInfo,
        userInfo: UserInfo?
    ): Account = addNewAccount(lastPermanentLocation, serverInfo, userName, password).also {
        updateUserAndServerInfo(it, serverInfo, userInfo, userName)
    }

    override fun addOAuthAccountIfDoesNotExist(
        lastPermanentLocation: String?,
        userName: String,
        authTokenType: String,
        accessToken: String,
        serverInfo: ServerInfo,
        userInfo: UserInfo?,
        refreshToken: String,
        scope: String
    ) {
        addNewAccount(lastPermanentLocation, serverInfo, userName).also {
            updateUserAndServerInfo(it, serverInfo, userInfo, userName)

            accountManager.setAuthToken(it, authTokenType, accessToken)

            accountManager.setUserData(it, AccountUtils.Constants.KEY_SUPPORTS_OAUTH2, "TRUE")
            accountManager.setUserData(it, AccountUtils.Constants.KEY_OAUTH2_REFRESH_TOKEN, refreshToken)
            accountManager.setUserData(it, AccountUtils.Constants.KEY_OAUTH2_SCOPE, scope)
        }
    }

    /**
     * Add new account to account manager, shared preferences and returns it
     */
    private fun addNewAccount(
        lastPermanentLocation: String?,
        serverInfo: ServerInfo,
        userName: String,
        password: String = ""
    ): Account {
        if (lastPermanentLocation != null) {
            serverInfo.baseUrl = WebdavUtils.trimWebdavSuffix(lastPermanentLocation)
        }

        val uri = Uri.parse(serverInfo.baseUrl)

        val accountName = AccountUtils.buildAccountName(uri, userName)
        val newAccount = Account(accountName, accountType)

        if (accountExists(accountName)) {
            Timber.d("The account already exists")
            throw AccountNotNewException()
        } else {
            // with external authorizations, the password is never input in the app
            accountManager.addAccountExplicitly(newAccount, password, null)

            /// add the new account as default in preferences, if there is none already
            val defaultAccount: Account? = getCurrentOwnCloudAccount(context)
            if (defaultAccount == null) {
                val editor = PreferenceManager
                    .getDefaultSharedPreferences(context).edit()
                editor.putString("select_oc_account", accountName)
                editor.apply()
            }

            return newAccount
        }
    }

    private fun updateUserAndServerInfo(
        newAccount: Account,
        serverInfo: ServerInfo,
        userInfo: UserInfo?,
        userName: String
    ) {
        // include account version with the new account
        accountManager.setUserData(
            newAccount,
            AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
            AccountUtils.ACCOUNT_VERSION.toString()
        )

        accountManager.setUserData(
            newAccount, AccountUtils.Constants.KEY_OC_VERSION, serverInfo.ownCloudVersion
        )

        accountManager.setUserData(
            newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, serverInfo.baseUrl
        )

        if (userInfo != null) {
            accountManager.setUserData(
                newAccount, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.displayName
            )
        } else {
            Timber.w("Couldn't get display name for %s", userName)
        }
    }

    override fun getAccounts(): Array<Account> {
        return accountManager.getAccountsByType(accountType)
    }

    override fun accountExists(accountName: String?): Boolean {
        val ocAccounts: Array<Account> = getAccounts()

        if (accountName != null) {
            var lastAtPos = accountName.lastIndexOf("@")
            val hostAndPort = accountName.substring(lastAtPos + 1)
            val username = accountName.substring(0, lastAtPos)
            var otherHostAndPort: String
            var otherUsername: String
            val currentLocale: Locale = context.resources.configuration.locale
            for (otherAccount in ocAccounts) {
                lastAtPos = otherAccount.name.lastIndexOf("@")
                otherHostAndPort = otherAccount.name.substring(lastAtPos + 1)
                otherUsername = otherAccount.name.substring(0, lastAtPos)
                if (otherHostAndPort == hostAndPort && otherUsername.toLowerCase(currentLocale) == username.toLowerCase(
                        currentLocale
                    )
                ) {
                    return true
                }
            }
        }
        return false
    }

    override fun getCurrentOwnCloudAccount(context: Context): Account? {
        val ocAccounts = getAccounts()
        var defaultAccount: Account? = null

        val appPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        val accountName = appPreferences
            .getString("select_oc_account", null)

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        if (accountName != null) {
            for (account in ocAccounts) {
                if (account.name == accountName) {
                    defaultAccount = account
                    break
                }
            }
        }

        if (!ocAccounts.isNullOrEmpty()) { // take first account as fallback
            defaultAccount = ocAccounts[0]
        }

        return defaultAccount
    }

    override fun getUserData(account: Account, key: String): String = accountManager.getUserData(account, key)
}
