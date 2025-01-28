/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.presentation.common

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.R
import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.domain.user.usecases.GetStoredQuotaAsStreamUseCase
import com.owncloud.android.domain.user.usecases.GetUserQuotasUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.accounts.RemoveAccountUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class DrawerViewModel(
    private val getStoredQuotaAsStreamUseCase: GetStoredQuotaAsStreamUseCase,
    private val removeAccountUseCase: RemoveAccountUseCase,
    private val getUserQuotasUseCase: GetUserQuotasUseCase,
    private val localStorageProvider: LocalStorageProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val contextProvider: ContextProvider,
) : ViewModel() {

    private val _userQuota = MutableStateFlow<Event<UIResult<Flow<UserQuota?>>>?>(null)
    val userQuota: StateFlow<Event<UIResult<Flow<UserQuota?>>>?> = _userQuota

    fun getUserQuota(accountName: String) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            requiresConnection = false,
            showLoading = true,
            flow = _userQuota,
            useCase = getStoredQuotaAsStreamUseCase,
            useCaseParams = GetStoredQuotaAsStreamUseCase.Params(accountName = accountName),
        )
    }

    fun getAccounts(context: Context): List<Account> =
        AccountUtils.getAccounts(context).asList()

    fun getUsernameOfAccount(accountName: String): String =
        AccountUtils.getUsernameOfAccount(accountName)

    fun getFeedbackMail() = contextProvider.getString(R.string.mail_feedback)

    fun removeAccount(context: Context) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val loggedAccounts = AccountUtils.getAccounts(context)
            localStorageProvider.deleteUnusedUserDirs(loggedAccounts)

            val userQuotas = getUserQuotasUseCase(Unit)
            val loggedAccountsNames = loggedAccounts.map { it.name }
            val totalAccountsNames = userQuotas.map { it.accountName }
            val removedAccountsNames = mutableListOf<String>()
            for (accountName in totalAccountsNames) {
                if (!loggedAccountsNames.contains(accountName)) {
                    removedAccountsNames.add(accountName)
                }
            }
            removedAccountsNames.forEach { removedAccountName ->
                Timber.d("$removedAccountName is being removed")
                removeAccountUseCase(
                    RemoveAccountUseCase.Params(accountName = removedAccountName)
                )
                localStorageProvider.removeLocalStorageForAccount(removedAccountName)
            }
        }
    }

}
