/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.presentation.viewmodels.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.operations.common.OperationType
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OCAuthViewModel(
    val context: Context
//    private val loginUseCase: LoginAsyncUseCase
) : ViewModel() {

    private val _loginStatus = MutableLiveData<UIResult<Unit>>()
    val loginStatus: LiveData<UIResult<Unit>> = _loginStatus

//    fun login(ownCloudCredentials: OwnCloudCredentials) {
//        viewModelScope.launch {
//            val loginUseCaseResult = withContext(Dispatchers.IO) {
//                loginUseCase.execute(
//                    LoginAsyncUseCase.Params(
//                        ownCloudCredentials
//                    )
//                )
//            }
//
//            withContext(Dispatchers.Main) {
//                if (!loginUseCaseResult.isSuccess) {
//                    _loginStatus.postValue(
//                        UIResult.Error(loginUseCaseResult.getErrorOrNull())
//                    )
//                } else {
//                    _loginStatus.postValue(UIResult.Success())
//                }
//            }
//        }
//    }
}
