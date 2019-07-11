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

package com.owncloud.android.presentation.sharing.shares

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.data.Resource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.sharing.shares.usecases.GetPublicSharesUsecase
import com.owncloud.android.operations.common.OperationType
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
class OCShareViewModel(
    private val filePath: String,
    val context: Context,
    val account: Account,
    private val getPublicSharesUseCase: GetPublicSharesUsecase = GetPublicSharesUsecase(context, account)
) : ViewModel() {

    private val _publicShares = MediatorLiveData<UIResult<List<OCShareEntity>>>()
    val publicShares: MediatorLiveData<UIResult<List<OCShareEntity>>> = _publicShares

    private lateinit var publicSharesDbLiveData: LiveData<List<OCShareEntity>>

    private val _privateShares = MediatorLiveData<UIResult<List<OCShareEntity>>>()
    val privateShares: MediatorLiveData<UIResult<List<OCShareEntity>>> = _privateShares

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                refreshPublicShares()
                refreshPrivateShares()
            }
        }
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/
    fun refreshPrivateShares() {
    }

//    fun insertPrivateShare(
//        filePath: String,
//        shareType: ShareType?,
//        shareeName: String, // User or group name of the target sharee.
//        permissions: Int
//    ): LiveData<Resource<Unit>> = shareRepository.insertPrivateShare(
//        filePath, shareType, shareeName, permissions
//    )

    fun updatePrivateShare(
        remoteId: Long,
        permissions: Int
    ): LiveData<Resource<Unit>> = shareRepository.updatePrivateShare(
        remoteId, permissions
    )

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/
    private fun refreshPublicShares() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                publicShares.value = UIResult.loading()
            }
        }

        val getPublicSharesUseCaseResult = getPublicSharesUseCase.execute(
            GetPublicSharesUsecase.Params(
                filePath = filePath,
                accountName = account.name
            )
        )

        publicSharesDbLiveData = getPublicSharesUseCaseResult.data as LiveData<List<OCShareEntity>>

        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                if (getPublicSharesUseCaseResult.isSuccess()) {
                    publicShares.addSource(
                        publicSharesDbLiveData
                    ) { shares ->
                        publicShares.value = UIResult.success(shares)
                    }
                } else { // Show db shares and the error
                    publicShares.addSource(
                        publicSharesDbLiveData
                    ) { shares ->
                        publicShares.value = UIResult.error(
                            shares,
                            errorMessage = ErrorMessageAdapter.getResultMessage(
                                getPublicSharesUseCaseResult.code,
                                getPublicSharesUseCaseResult.exception,
                                OperationType.GET_SHARES,
                                context.resources
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        publicShares.removeSource(publicSharesDbLiveData)
    }

//    fun insertPublicShare(
//        filePath: String,
//        permissions: Int,
//        name: String,
//        password: String,
//        expirationTimeInMillis: Long,
//        publicUpload: Boolean
//    ): LiveData<Resource<Unit>> = shareRepository.insertPublicShare(
//        filePath, permissions, name, password, expirationTimeInMillis, publicUpload
//    )
//
//    fun updatePublicShareForFile(
//        remoteId: Long,
//        name: String,
//        password: String?,
//        expirationDateInMillis: Long,
//        permissions: Int,
//        publicUpload: Boolean
//    ): LiveData<Resource<Unit>> = shareRepository.updatePublicShare(
//        remoteId, name, password, expirationDateInMillis, permissions, publicUpload
//    )
//
//    fun deletePublicShare(
//        remoteId: Long
//    ): LiveData<Resource<Unit>> = shareRepository.deletePublicShare(remoteId)
}
