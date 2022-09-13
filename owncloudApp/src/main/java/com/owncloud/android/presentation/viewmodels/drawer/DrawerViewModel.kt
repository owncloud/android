/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.presentation.viewmodels.drawer

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.domain.user.usecases.GetStoredQuotaUseCase
import com.owncloud.android.domain.user.usecases.GetUserQuotasUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.accounts.RemoveAccountUseCase
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.launch

class DrawerViewModel(
    private val getStoredQuotaUseCase: GetStoredQuotaUseCase,
    private val removeAccountUseCase: RemoveAccountUseCase,
    private val getUserQuotasUseCase: GetUserQuotasUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _userQuota = MediatorLiveData<Event<UIResult<UserQuota?>>>()
    val userQuota: LiveData<Event<UIResult<UserQuota?>>> = _userQuota

    fun getStoredQuota(
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = true,
        liveData = _userQuota,
        useCase = getStoredQuotaUseCase,
        useCaseParams = GetStoredQuotaUseCase.Params(accountName = accountName)
    )

    fun getAccounts(context: Context): List<Account> {
        return AccountUtils.getAccounts(context).asList()
    }

    fun getCurrentAccount(context: Context): Account? {
        return AccountUtils.getCurrentOwnCloudAccount(context)
    }

    fun getUsernameOfAccount(accountName: String): String {
        return AccountUtils.getUsernameOfAccount(accountName)
    }

    fun setCurrentAccount(context: Context, accountName: String): Boolean {
        return AccountUtils.setCurrentOwnCloudAccount(context, accountName)
    }

    fun removeAccount(context: Context) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val loggedAccounts = AccountUtils.getAccounts(context)
            FileStorageUtils.deleteUnusedUserDirs(loggedAccounts)

            val userQuotas = getUserQuotasUseCase.execute(Unit)
            val loggedAccountsNames = loggedAccounts.map { it.name }
            val totalAccountsNames = userQuotas.map { it.accountName }
            val removedAccountsNames = mutableListOf<String>()
            for (accountName in totalAccountsNames) {
                if (!loggedAccountsNames.contains(accountName)) {
                    removedAccountsNames.add(accountName)
                }
            }
            removedAccountsNames.forEach { removedAccountName ->
                removeAccountUseCase.execute(
                    RemoveAccountUseCase.Params(accountName = removedAccountName)
                )
            }
        }
    }
}
