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
import android.preference.PreferenceManager
import com.owncloud.android.data.authentication.SELECTED_ACCOUNT
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager

class ClientHandler(
    private val accountManager: AccountManager,
    val context: Context
) {
    fun getClientForAccount(
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
        var defaultAccount: Account? = null

        val appPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val accountName = appPreferences.getString(SELECTED_ACCOUNT, null)

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
}
