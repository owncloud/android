/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2022 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.presentation.viewmodels.activity

import android.accounts.Account
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.providers.ContextProvider

class FileDisplayViewModel(
    private val contextProvider: ContextProvider
) : ViewModel() {

    fun getPotentialAccountsToOpenDeepLink(uri: Uri): List<Account> {
        val accounts = AccountUtils.getAccounts(contextProvider.getContext())

        return accounts.filter { account ->
            uri.authority == AccountUtils.getHostOfAccount(account.name)
        }
    }
}
