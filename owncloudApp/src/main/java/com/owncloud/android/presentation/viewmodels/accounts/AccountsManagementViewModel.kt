/**
 * ownCloud Android client application
 *
 * @author Javier Rodríguez Pérez
 *
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
 */

package com.owncloud.android.presentation.viewmodels.accounts

import android.accounts.Account
import androidx.lifecycle.ViewModel
import com.owncloud.android.providers.AccountProvider

class AccountsManagementViewModel(
    private val accountProvider: AccountProvider
) : ViewModel() {

    fun getLoggedAccounts(): Array<Account> {
        return accountProvider.getLoggedAccounts()
    }

    fun getCurrentAccount(): Account? {
        return accountProvider.getCurrentOwnCloudAccount()
    }
}
