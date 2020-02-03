/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.authentication.usecases.LoginAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetUserInfoUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber

class OCAuthenticationViewModel(
    private val loginAsyncUseCase: LoginAsyncUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {
    fun login(
        serverUrl: String,
        username: String,
        password: String
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult = loginAsyncUseCase.execute(
                LoginAsyncUseCase.Params(
                    serverPath = serverUrl,
                    username = username,
                    password = password
                )
            )
            Timber.d(useCaseResult.toString())
        }
    }

    fun getUserInfo() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult = getUserInfoUseCase.execute(Unit)
            Timber.d(useCaseResult.toString())
        }
    }
}
