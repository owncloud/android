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
import com.owncloud.android.data.authentication.KEY_CLIENT_REGISTRATION_CLIENT_EXPIRATION_DATE
import com.owncloud.android.data.authentication.KEY_CLIENT_REGISTRATION_CLIENT_ID
import com.owncloud.android.data.authentication.KEY_CLIENT_REGISTRATION_CLIENT_SECRET
import com.owncloud.android.data.authentication.KEY_OAUTH2_REFRESH_TOKEN
import com.owncloud.android.data.authentication.KEY_OAUTH2_SCOPE
import com.owncloud.android.data.authentication.SELECTED_ACCOUNT
import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.exceptions.AccountNotFoundException
import com.owncloud.android.domain.exceptions.AccountNotNewException
import com.owncloud.android.domain.exceptions.AccountNotTheSameException
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.ACCOUNT_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_DISPLAY_NAME
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_ID
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_BASE_URL
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_SUPPORTS_OAUTH2
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.OAUTH_SUPPORTED_TRUE
import com.owncloud.android.lib.common.authentication.OwnCloudBasicCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudBearerCredentials
import timber.log.Timber
import java.util.Locale

class OCLocalAuthenticationDataSource(
    private val context: Context,
    private val accountManager: AccountManager,
    private val preferencesProvider: SharedPreferencesProvider,
    private val accountType: String
) : LocalAuthenticationDataSource {

    override fun addBasicAccount(
        userName: String,
        lastPermanentLocation: String?,
        password: String,
        serverInfo: ServerInfo,
        userInfo: UserInfo,
        updateAccountWithUsername: String?
    ): String =
        addAccount(
            lastPermanentLocation = lastPermanentLocation,
            serverInfo = serverInfo,
            userName = userName,
            password = password,
            updateAccountWithUsername = updateAccountWithUsername
        ).also { account ->
            updateAccountWithUsername?.let {
                accountManager.setPassword(account, password)
                SingleSessionManager.getDefaultSingleton().refreshCredentialsForAccount(
                    account.name, OwnCloudBasicCredentials(userName, password)
                )
            }
            updateUserAndServerInfo(account, serverInfo, userInfo)
        }.name

    override fun addOAuthAccount(
        userName: String,
        lastPermanentLocation: String?,
        authTokenType: String,
        accessToken: String,
        serverInfo: ServerInfo,
        userInfo: UserInfo,
        refreshToken: String,
        scope: String?,
        updateAccountWithUsername: String?,
        clientRegistrationInfo: ClientRegistrationInfo?
    ): String =
        addAccount(
            lastPermanentLocation = lastPermanentLocation,
            serverInfo = serverInfo,
            userName = userName,
            updateAccountWithUsername = updateAccountWithUsername
        ).also {
            updateUserAndServerInfo(it, serverInfo, userInfo)

            accountManager.setAuthToken(it, authTokenType, accessToken)

            updateAccountWithUsername?.let { userName ->
                SingleSessionManager.getDefaultSingleton().refreshCredentialsForAccount(
                    it.name, OwnCloudBearerCredentials(userName, accessToken)
                )
            }

            clientRegistrationInfo?.let { clientRegistrationInfo ->
                accountManager.apply {
                    setUserData(it, KEY_CLIENT_REGISTRATION_CLIENT_ID, clientRegistrationInfo.clientId)
                    setUserData(it, KEY_CLIENT_REGISTRATION_CLIENT_SECRET, clientRegistrationInfo.clientSecret)
                    setUserData(it, KEY_CLIENT_REGISTRATION_CLIENT_EXPIRATION_DATE, clientRegistrationInfo.clientSecretExpiration.toString())
                }
            }

            accountManager.setUserData(it, KEY_SUPPORTS_OAUTH2, OAUTH_SUPPORTED_TRUE)
            accountManager.setUserData(it, KEY_OAUTH2_REFRESH_TOKEN, refreshToken)
            scope?.run {
                accountManager.setUserData(it, KEY_OAUTH2_SCOPE, this)
            }
        }.name

    /**
     * Add new account to account manager, shared preferences and returns account
     */
    private fun addAccount(
        lastPermanentLocation: String?,
        serverInfo: ServerInfo,
        userName: String,
        password: String = "",
        updateAccountWithUsername: String?
    ): Account {

        lastPermanentLocation?.let {
            serverInfo.baseUrl = it
        }

        val uri = Uri.parse(serverInfo.baseUrl)

        val accountName = AccountUtils.buildAccountName(uri, userName)

        // Check if the entered user matches the user of the account to update
        if (!updateAccountWithUsername.isNullOrBlank() && accountName != updateAccountWithUsername) {
            throw AccountNotTheSameException()
        }

        val account = getAccountIfExists(accountName)
        if (account != null) {
            if (updateAccountWithUsername.isNullOrBlank()) {
                Timber.d("The account already exists")
                throw AccountNotNewException()
            } else {
                return account // Account won't be null, since we've already checked it exists
            }
        } else {
            val newAccount = Account(accountName, accountType)

            // with external authorizations, the password is never input in the app
            accountManager.addAccountExplicitly(newAccount, password, null)

            /// add the new account as default in preferences, if there is none already
            val defaultAccount: Account? = getCurrentAccount()
            if (defaultAccount == null) {
                preferencesProvider.putString(SELECTED_ACCOUNT, accountName)
            }

            return newAccount
        }
    }

    private fun updateUserAndServerInfo(
        newAccount: Account,
        serverInfo: ServerInfo,
        userInfo: UserInfo
    ) {
        // include account version with the new account
        accountManager.setUserData(
            newAccount,
            KEY_OC_ACCOUNT_VERSION,
            ACCOUNT_VERSION.toString()
        )

        accountManager.setUserData(
            newAccount, KEY_OC_VERSION, serverInfo.ownCloudVersion
        )

        accountManager.setUserData(
            newAccount, KEY_OC_BASE_URL, serverInfo.baseUrl
        )

        accountManager.setUserData(
            newAccount, KEY_DISPLAY_NAME, userInfo.displayName
        )

        accountManager.setUserData(
            newAccount, KEY_ID, userInfo.id
        )
    }

    private fun getAccountIfExists(accountName: String): Account? {
        val ocAccounts: Array<Account> = getAccounts()

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
                return otherAccount
            }
        }

        return null
    }

    private fun getAccounts(): Array<Account> {
        return accountManager.getAccountsByType(accountType)
    }

    private fun getCurrentAccount(): Account? {
        val ocAccounts = getAccounts()
        var defaultAccount: Account? = null

        val accountName = preferencesProvider.getString(SELECTED_ACCOUNT, null)

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        if (accountName != null) {
            for (account in ocAccounts) {
                if (account.name == accountName) {
                    defaultAccount = account
                    break
                }
            }
        }

        return defaultAccount
    }

    override fun supportsOAuth2(accountName: String): Boolean {
        val account = getAccountIfExists(accountName) ?: throw AccountNotFoundException()
        return accountManager.getUserData(account, KEY_SUPPORTS_OAUTH2) == OAUTH_SUPPORTED_TRUE
    }

    override fun getBaseUrl(accountName: String): String {
        val account = getAccountIfExists(accountName) ?: throw AccountNotFoundException()
        return accountManager.getUserData(account, KEY_OC_BASE_URL)
    }
}
