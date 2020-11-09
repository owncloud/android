/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import com.owncloud.android.data.authentication.SELECTED_ACCOUNT
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory.getAnonymousCredentials
import com.owncloud.android.lib.resources.files.services.FileService
import com.owncloud.android.lib.resources.files.services.implementation.OCFileService
import com.owncloud.android.lib.resources.users.services.UserService
import com.owncloud.android.lib.resources.users.services.implementation.OCUserService

class ClientManager(
    private val accountManager: AccountManager,
    private val preferencesProvider: SharedPreferencesProvider,
    val context: Context
) {
    // This client will maintain cookies across the whole login process.
    private var ownCloudClient: OwnCloudClient? = null

    /**
     * Returns a client for the login process.
     * Helpful to keep the cookies from the status request to the final login and user info retrieval.
     * For regular uses, use [getClientForAccount]
     */
    fun getClientForUnExistingAccount(
        path: String,
        requiresNewClient: Boolean,
        ownCloudCredentials: OwnCloudCredentials? = getAnonymousCredentials()
    ): OwnCloudClient {
        val safeClient = ownCloudClient

        return if (requiresNewClient || safeClient == null) {
            OwnCloudClient(Uri.parse(path)).apply {
                credentials = ownCloudCredentials
            }.also {
                ownCloudClient = it
            }
        } else {
            safeClient
        }
    }

    private fun getClientForAccount(
        accountName: String?
    ): OwnCloudClient {
        val account: Account? = if (accountName.isNullOrBlank()) {
            getCurrentAccount()
        } else {
            accountManager.accounts.firstOrNull { it.name == accountName }
        }
        val ownCloudAccount = OwnCloudAccount(account, context)
        return SingleSessionManager.getDefaultSingleton().getClientFor(ownCloudAccount, context)
    }

    private fun getCurrentAccount(): Account? {
        val ocAccounts = accountManager.accounts

        val accountName = preferencesProvider.getString(SELECTED_ACCOUNT, null)

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        accountName?.let { selectedAccountName ->
            ocAccounts.firstOrNull { it.name == selectedAccountName }?.let { return it }
        }

        // take first account as fallback
        return ocAccounts.firstOrNull()
    }

    fun getUserService(accountName: String? = ""): UserService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCUserService(client = ownCloudClient)
    }

    fun getFileService(accountName: String? = ""): FileService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCFileService(client = ownCloudClient)
    }
}
