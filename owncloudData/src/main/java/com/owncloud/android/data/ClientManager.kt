/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2023 ownCloud GmbH.
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
import androidx.core.net.toUri
import com.owncloud.android.data.authentication.SELECTED_ACCOUNT
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.lib.common.ConnectionValidator
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory.getAnonymousCredentials
import com.owncloud.android.lib.resources.appregistry.services.AppRegistryService
import com.owncloud.android.lib.resources.appregistry.services.OCAppRegistryService
import com.owncloud.android.lib.resources.files.services.FileService
import com.owncloud.android.lib.resources.files.services.implementation.OCFileService
import com.owncloud.android.lib.resources.shares.services.ShareService
import com.owncloud.android.lib.resources.shares.services.ShareeService
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareService
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareeService
import com.owncloud.android.lib.resources.spaces.services.OCSpacesService
import com.owncloud.android.lib.resources.spaces.services.SpacesService
import com.owncloud.android.lib.resources.status.services.CapabilityService
import com.owncloud.android.lib.resources.status.services.implementation.OCCapabilityService
import com.owncloud.android.lib.resources.users.services.UserService
import com.owncloud.android.lib.resources.users.services.implementation.OCUserService
import timber.log.Timber

class ClientManager(
    private val accountManager: AccountManager,
    private val preferencesProvider: SharedPreferencesProvider,
    val context: Context,
    val accountType: String,
    private val connectionValidator: ConnectionValidator
) {
    // This client will maintain cookies across the whole login process.
    private var ownCloudClient: OwnCloudClient? = null

    // Cached client to avoid retrieving the client for each service
    private var ownCloudClientForCurrentAccount: OwnCloudClient? = null

    init {
        SingleSessionManager.setConnectionValidator(connectionValidator)
    }

    /**
     * Returns a client for the login process.
     * Helpful to keep the cookies from the status request to the final login and user info retrieval.
     * For regular uses, use [getClientForAccount]
     */
    fun getClientForAnonymousCredentials(
        path: String,
        requiresNewClient: Boolean,
        ownCloudCredentials: OwnCloudCredentials? = getAnonymousCredentials()
    ): OwnCloudClient {
        val safeClient = ownCloudClient
        val pathUri = path.toUri()

        return if (requiresNewClient || safeClient == null || safeClient.baseUri != pathUri) {
            Timber.d("Creating new client for path: $pathUri. Old client path: ${safeClient?.baseUri}, requiresNewClient: $requiresNewClient")
            OwnCloudClient(
                pathUri,
                connectionValidator,
                true,
                SingleSessionManager.getDefaultSingleton(),
                context
            ).apply {
                credentials = ownCloudCredentials
            }.also {
                ownCloudClient = it
            }
        } else {
            Timber.d("Reusing anonymous client for ${safeClient.baseUri}")
            safeClient
        }
    }

    private fun getClientForAccount(
        accountName: String?
    ): OwnCloudClient {
        val account: Account? = if (accountName.isNullOrBlank()) {
            getCurrentAccount()
        } else {
            accountManager.getAccountsByType(accountType).firstOrNull { it.name == accountName }
        }

        val ownCloudAccount = OwnCloudAccount(account, context)
        return SingleSessionManager.getDefaultSingleton().getClientFor(ownCloudAccount, context, connectionValidator).also {
            ownCloudClientForCurrentAccount = it
        }
    }

    private fun getCurrentAccount(): Account? {
        val ocAccounts = accountManager.getAccountsByType(accountType)

        val accountName = preferencesProvider.getString(SELECTED_ACCOUNT, null)

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        accountName?.let { selectedAccountName ->
            ocAccounts.firstOrNull { it.name == selectedAccountName }?.let { return it }
        }

        // take first account as fallback
        return ocAccounts.firstOrNull()
    }

    fun getClientForCoilThumbnails(accountName: String) = getClientForAccount(accountName = accountName)

    fun getUserService(accountName: String? = ""): UserService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCUserService(client = ownCloudClient)
    }

    fun getFileService(accountName: String? = ""): FileService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCFileService(client = ownCloudClient)
    }

    fun getCapabilityService(accountName: String? = ""): CapabilityService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCCapabilityService(client = ownCloudClient)
    }

    fun getShareService(accountName: String? = ""): ShareService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCShareService(client = ownCloudClient)
    }

    fun getShareeService(accountName: String? = ""): ShareeService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCShareeService(client = ownCloudClient)
    }

    fun getSpacesService(accountName: String): SpacesService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCSpacesService(client = ownCloudClient)
    }

    fun getAppRegistryService(accountName: String): AppRegistryService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCAppRegistryService(client = ownCloudClient)
    }
}
