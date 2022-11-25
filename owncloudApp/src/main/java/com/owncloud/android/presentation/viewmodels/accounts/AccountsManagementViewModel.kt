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