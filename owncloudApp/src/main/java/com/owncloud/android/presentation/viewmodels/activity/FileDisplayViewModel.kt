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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.GetPrivateLinkDiscoveredUseCase
import kotlinx.coroutines.launch

class FileDisplayViewModel(
    private val contextProvider: ContextProvider,
    private val getPrivateLinkDiscoveredUseCase: GetPrivateLinkDiscoveredUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _privateLink = MediatorLiveData<Event<UIResult<String>>>()
    val privateLink: LiveData<Event<UIResult<String>>> = _privateLink

    fun getPotentialAccountsToOpenDeepLink(uri: Uri): List<Account> {
        val accounts = AccountUtils.getAccounts(contextProvider.getContext())

        return accounts.filter { account ->
            uri.authority == AccountUtils.getHostOfAccount(account.name)
        }
    }

    fun syncFolderOperation(url: String, account: Account, ownCloudAccount: OwnCloudAccount, storageManager: FileDataStorageManager) {
        val accountName = getAccountName()
        val urlCleaned = url.substringAfter(accountName).substringBeforeLast('/')

        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val client = SingleSessionManager.getDefaultSingleton().getClientFor(ownCloudAccount, contextProvider.getContext())
            val result = SynchronizeFolderOperation(
                contextProvider.getContext(),
                urlCleaned,
                account,
                System.currentTimeMillis(),
                false,
                false,
                false
            ).execute(client, storageManager)
        }
    }

    fun getPrivateLink(url: String) =
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = false,
            liveData = _privateLink,
            useCase = getPrivateLinkDiscoveredUseCase,
            useCaseParams = GetPrivateLinkDiscoveredUseCase.Params(url)
        )

    private fun getAccountName(): String {
        val account = AccountUtils.getCurrentOwnCloudAccount(contextProvider.getContext())
        return account.name.substringBefore('@')
    }
}
