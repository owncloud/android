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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.sharing.shares.usecases.PrivateSharesLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.PublicSharesLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshPrivateSharesUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshPublicSharesUseCase
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
    privateSharesLiveDataUseCase: PrivateSharesLiveDataUseCase = PrivateSharesLiveDataUseCase(context, account),
    publicSharesLiveDataUseCase: PublicSharesLiveDataUseCase = PublicSharesLiveDataUseCase(context, account),
    private val refreshPrivateSharesUseCase: RefreshPrivateSharesUseCase = RefreshPrivateSharesUseCase(
        context, account
    ),
    private val refreshPublicSharesUseCase: RefreshPublicSharesUseCase = RefreshPublicSharesUseCase(context, account)
) : ViewModel() {

    private val _privateShares = MutableLiveData<UIResult<List<OCShareEntity>>>()
    val privateShares: LiveData<UIResult<List<OCShareEntity>>> = _privateShares

    private var privateSharesLiveData: LiveData<List<OCShareEntity>>? = privateSharesLiveDataUseCase.execute(
        PrivateSharesLiveDataUseCase.Params(
            filePath = filePath,
            accountName = account.name
        )
    ).data

    // To detect changes in private shares
    private val privateSharesObserver: Observer<List<OCShareEntity>> = Observer {
        _privateShares.postValue(UIResult.success(it))
    }

    private val _publicShares = MutableLiveData<UIResult<List<OCShareEntity>>>()
    val publicShares: LiveData<UIResult<List<OCShareEntity>>> = _publicShares

    private var publicSharesLiveData: LiveData<List<OCShareEntity>>? = publicSharesLiveDataUseCase.execute(
        PublicSharesLiveDataUseCase.Params(
            filePath = filePath,
            accountName = account.name
        )
    ).data

    // To detect changes in public shares
    private val publicSharesObserver: Observer<List<OCShareEntity>> = Observer {
        _publicShares.postValue(UIResult.success(it))
    }

    init {
        privateSharesLiveData?.apply {
            observeForever(privateSharesObserver)
        }

        publicSharesLiveData?.apply {
            observeForever(publicSharesObserver)
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                refreshPrivateShares()
                refreshPublicShares()
            }
        }
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/
    private fun refreshPrivateShares() {
        _privateShares.postValue(
            UIResult.loading(privateSharesLiveData?.value)
        )

        refreshPrivateSharesUseCase.execute(
            RefreshPrivateSharesUseCase.Params(
                filePath = filePath,
                accountName = account.name
            )
        ).also { useCaseResult ->
            if (!useCaseResult.isSuccess()) {
                _privateShares.postValue(
                    UIResult.error(
                        privateSharesLiveData?.value,
                        errorMessage = ErrorMessageAdapter.getResultMessage(
                            useCaseResult.code,
                            useCaseResult.exception,
                            OperationType.GET_SHARES,
                            context.resources
                        )
                    )
                )
            }
        }
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
        _publicShares.postValue(
            UIResult.loading(publicSharesLiveData?.value)
        )

        refreshPublicSharesUseCase.execute(
            RefreshPublicSharesUseCase.Params(
                filePath = filePath,
                accountName = account.name
            )
        ).also { useCaseResult ->
            if (!useCaseResult.isSuccess()) {
                _publicShares.postValue(
                    UIResult.error(
                        publicSharesLiveData?.value,
                        errorMessage = ErrorMessageAdapter.getResultMessage(
                            useCaseResult.code,
                            useCaseResult.exception,
                            OperationType.GET_SHARES,
                            context.resources
                        )
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        publicSharesLiveData?.removeObserver(publicSharesObserver)
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
