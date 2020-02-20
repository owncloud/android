/**
 * ownCloud Android client application
 *
 * @author David González V
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
import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.lib.common.accounts.AccountUtils

class OCLocalAuthenticationDataSource(
    private val context: Context
) : LocalAuthenticationDataSource {
    override fun addAccount(
        uri: Uri,
        userName: String,
        password: String,
        accountName: String,
        accountType: String,
        isOAuth: Boolean
    ) {
        val accountName = AccountUtils.buildAccountName(uri, userName)
        val newAccount = Account(accountName, accountType)

        // TODO check whether account exists
        val accountManager = AccountManager.get(context)

        if (isOAuth) {
            // with external authorizations, the password is never input in the app
            // with external authorizations, the password is never input in the app
            accountManager.addAccountExplicitly(newAccount, "", null)
        } else {
            accountManager.addAccountExplicitly(
                newAccount, password, null
            )
        }
    }

    override fun getAccounts() : Account[] {

    }

    override fun getAccountsByType() : Account[] {

    }

    override fun accountExists() {

    }
}
