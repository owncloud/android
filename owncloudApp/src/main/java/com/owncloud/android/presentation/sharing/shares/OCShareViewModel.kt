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
import com.owncloud.android.domain.sharing.shares.usecases.SharesLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshSharesUseCase
import com.owncloud.android.lib.resources.shares.ShareType
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
    sharesLiveDataUseCase: SharesLiveDataUseCase = SharesLiveDataUseCase(context, account),
    private val refreshSharesUseCase: RefreshSharesUseCase = RefreshSharesUseCase(
        context, account
    )
) : ViewModel() {

    private val _shares = MutableLiveData<UIResult<List<OCShareEntity>>>()
    val shares: LiveData<UIResult<List<OCShareEntity>>> = _shares

    private var sharesLiveData: LiveData<List<OCShareEntity>>? = sharesLiveDataUseCase.execute(
        SharesLiveDataUseCase.Params(
            filePath = filePath,
            accountName = account.name
        )
    ).data

    // To detect changes in shares
    private val sharesObserver: Observer<List<OCShareEntity>> = Observer { shares ->
        if (shares.isNotEmpty()) {
            _shares.postValue(UIResult.success(shares))
        }
    }

    init {
        sharesLiveData?.observeForever(sharesObserver)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                refreshShares()
            }
        }
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/
//    fun insertPrivateShare(
//        filePath: String,
//        shareType: ShareType?,
//        shareeName: String, // User or group name of the target sharee.
//        permissions: Int
//    ): LiveData<DataResult<Unit>> = shareRepository.insertPrivateShare(
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

//    fun insertPublicShare(
//        filePath: String,
//        permissions: Int,
//        name: String,
//        password: String,
//        expirationTimeInMillis: Long,
//        publicUpload: Boolean
//    ): LiveData<DataResult<Unit>> = shareRepository.insertPublicShare(
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
//    ): LiveData<DataResult<Unit>> = shareRepository.updatePublicShare(
//        remoteId, name, password, expirationDateInMillis, permissions, publicUpload
//    )
//
//    fun deletePublicShare(
//        remoteId: Long
//    ): LiveData<DataResult<Unit>> = shareRepository.deletePublicShare(remoteId)

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    private fun refreshShares() {
        _shares.postValue(
            UIResult.loading(sharesLiveData?.value)
        )

        refreshSharesUseCase.execute(
            RefreshSharesUseCase.Params(
                filePath = filePath,
                accountName = account.name
            )
        ).also { useCaseResult ->
            if (!useCaseResult.isSuccess()) {
                _shares.postValue(
                    UIResult.error(
                        sharesLiveData?.value,
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
        sharesLiveData?.removeObserver(sharesObserver)
    }
}
