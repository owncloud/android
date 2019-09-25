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

package com.owncloud.android.presentation.viewmodels.sharing

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.sharing.shares.usecases.CreatePrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.CreatePublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.DeleteShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetShareAsLiveDataAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetSharesAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshSharesFromNetworkAsyncUseCase
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.common.OperationType
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import kotlinx.coroutines.CoroutineDispatcher
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
    getSharesAsLiveDataUseCase: GetSharesAsLiveDataUseCase = GetSharesAsLiveDataUseCase(context, account),
    private val getShareAsLiveDataUseCase: GetShareAsLiveDataAsyncUseCase = GetShareAsLiveDataAsyncUseCase(context, account),
    private val refreshSharesFromNetworkUseCase: RefreshSharesFromNetworkAsyncUseCase =
        RefreshSharesFromNetworkAsyncUseCase(context, account),
    private val createPrivateShareUseCase: CreatePrivateShareAsyncUseCase = CreatePrivateShareAsyncUseCase(context, account),
    private val editPrivateShareUseCase: EditPrivateShareAsyncUseCase = EditPrivateShareAsyncUseCase(context, account),
    private val createPublicShareUseCase: CreatePublicShareAsyncUseCase = CreatePublicShareAsyncUseCase(context, account),
    private val editPublicShareUseCase: EditPublicShareAsyncUseCase = EditPublicShareAsyncUseCase(context, account),
    private val deletePublicShareUseCase: DeleteShareAsyncUseCase = DeleteShareAsyncUseCase(context, account),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _shares = MediatorLiveData<UIResult<List<OCShareEntity>>>()
    val shares: LiveData<UIResult<List<OCShareEntity>>> = _shares

    private var sharesLiveData: LiveData<List<OCShareEntity>>? = getSharesAsLiveDataUseCase.execute(
        GetSharesAsLiveDataUseCase.Params(
            filePath = filePath,
            accountName = account.name
        )
    ).getDataOrNull()

    init {
        sharesLiveData?.let {
            _shares.addSource(it) { shares ->
                _shares.postValue(UIResult.Success(shares))
            }
        }

        refreshSharesFromNetwork()
    }

    private fun refreshSharesFromNetwork() {
        viewModelScope.launch(ioDispatcher) {
            _shares.postValue(
                UIResult.Loading(sharesLiveData?.value)
            )

            val useCaseResult = refreshSharesFromNetworkUseCase.execute(
                RefreshSharesFromNetworkAsyncUseCase.Params(
                    filePath = filePath,
                    accountName = account.name
                )
            )

            if (!useCaseResult.isSuccess) {
                _shares.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            }
        }
    }

    private val _shareDeletionStatus = MutableLiveData<UIResult<Unit>>()
    val shareDeletionStatus: LiveData<UIResult<Unit>> = _shareDeletionStatus

//    fun deleteShare(
//        remoteId: Long
//    ) {
//        _shareDeletionStatus.postValue(
//            UIResult.loading()
//        )
//
//        viewModelScope.launch {
//            val useCaseResult = withContext(Dispatchers.IO) {
//                deletePublicShareUseCase.execute(
//                    DeleteShareAsyncUseCase.Params(
//                        remoteId
//                    )
//                )
//            }
//
//            withContext(Dispatchers.Main) {
//                if (!useCaseResult.isSuccess()) {
//                    _shareDeletionStatus.postValue(
//                        UIResult.error(
//                            errorMessage = useCaseResult.msg ?: ErrorMessageAdapter.getResultMessage(
//                                useCaseResult.code,
//                                useCaseResult.exception,
//                                OperationType.REMOVE_SHARE,
//                                context.resources
//                            )
//                        )
//                    )
//                } else {
//                    _shareDeletionStatus.postValue(UIResult.success())
//                }
//            }
//        }
//    }

//    /******************************************************************************************************
//     ******************************************* PRIVATE SHARES *******************************************
//     ******************************************************************************************************/
//
//    private val _privateShareCreationStatus = MutableLiveData<UIResult<Unit>>()
//    val privateShareCreationStatus: LiveData<UIResult<Unit>> = _privateShareCreationStatus
//
//    fun insertPrivateShare(
//        filePath: String,
//        shareType: ShareType?,
//        shareeName: String, // User or group name of the target sharee.
//        permissions: Int
//    ) {
//        _privateShareCreationStatus.postValue(
//            UIResult.loading()
//        )
//
//        viewModelScope.launch {
//            val useCaseResult = withContext(Dispatchers.IO) {
//                createPrivateShareUseCase.execute(
//                    CreatePrivateShareAsyncUseCase.Params(
//                        filePath,
//                        shareType,
//                        shareeName,
//                        permissions
//                    )
//                )
//            }
//
//            withContext(Dispatchers.Main) {
//                if (!useCaseResult.isSuccess) {
//                    _privateShareCreationStatus.postValue(
//                        UIResult.error(
//                            errorMessage = useCaseResult.msg ?: ErrorMessageAdapter.getResultMessage(
//                                useCaseResult.code,
//                                useCaseResult.exception,
//                                OperationType.CREATE_SHARE_WITH_SHAREES,
//                                context.resources
//                            )
//                        )
//                    )
//                }
//            }
//        }
//    }
//
//    private val _privateShare = MutableLiveData<UIResult<OCShareEntity>>()
//    val privateShare: LiveData<UIResult<OCShareEntity>> = _privateShare
//
//    private var privateShareLiveData: LiveData<OCShareEntity>? = null
//    private lateinit var privateShareObserver: Observer<OCShareEntity>
//
//    // Used to get a specific private share after updating it
//    fun refreshPrivateShare(
//        remoteId: Long
//    ) {
//        privateShareLiveData = getShareAsLiveDataUseCase.execute(
//            GetShareAsLiveDataAsyncUseCase.Params(remoteId)
//        ).data!!
//
//        privateShareObserver = Observer { privateShare ->
//            _privateShare.postValue(UIResult.success(privateShare))
//        }
//
//        privateShareLiveData?.observeForever(privateShareObserver)
//    }
//
//    private val _privateShareEditionStatus = MutableLiveData<UIResult<Unit>>()
//    val privateShareEditionStatus: LiveData<UIResult<Unit>> = _privateShareEditionStatus
//
//    fun updatePrivateShare(
//        remoteId: Long,
//        permissions: Int
//    ) {
//        _privateShareEditionStatus.postValue(
//            UIResult.loading()
//        )
//
//        viewModelScope.launch {
//            val useCaseResult = withContext(Dispatchers.IO) {
//                editPrivateShareUseCase.execute(
//                    EditPrivateShareAsyncUseCase.Params(
//                        remoteId,
//                        permissions
//                    )
//                )
//            }
//
//            withContext(Dispatchers.Main) {
//                if (!useCaseResult.isSuccess()) {
//                    _privateShareEditionStatus.postValue(
//                        UIResult.error(
//                            errorMessage = useCaseResult.msg ?: ErrorMessageAdapter.getResultMessage(
//                                useCaseResult.code,
//                                useCaseResult.exception,
//                                OperationType.UPDATE_SHARE,
//                                context.resources
//                            )
//                        )
//                    )
//                } else {
//                    _privateShareEditionStatus.postValue(UIResult.success())
//                }
//            }
//        }
//    }
//
//    /******************************************************************************************************
//     ******************************************* PUBLIC SHARES ********************************************
//     ******************************************************************************************************/
//
//    private val _publicShareCreationStatus = MutableLiveData<UIResult<Unit>>()
//    val publicShareCreationStatus: LiveData<UIResult<Unit>> = _publicShareCreationStatus
//
//    fun insertPublicShare(
//        filePath: String,
//        permissions: Int,
//        name: String,
//        password: String,
//        expirationTimeInMillis: Long,
//        publicUpload: Boolean
//    ) {
//        _publicShareCreationStatus.postValue(
//            UIResult.loading()
//        )
//
//        viewModelScope.launch {
//            val useCaseResult = withContext(Dispatchers.IO) {
//                createPublicShareUseCase.execute(
//                    CreatePublicShareAsyncUseCase.Params(
//                        filePath,
//                        permissions,
//                        name,
//                        password,
//                        expirationTimeInMillis,
//                        publicUpload
//                    )
//                )
//            }
//
//            withContext(Dispatchers.Main) {
//                if (!useCaseResult.isSuccess()) {
//                    _publicShareCreationStatus.postValue(
//                        UIResult.error(
//                            errorMessage = useCaseResult.msg ?: ErrorMessageAdapter.getResultMessage(
//                                useCaseResult.code,
//                                useCaseResult.exception,
//                                OperationType.CREATE_PUBLIC_SHARE,
//                                context.resources
//                            )
//                        )
//                    )
//                } else {
//                    _publicShareCreationStatus.postValue(UIResult.success())
//                }
//            }
//        }
//    }
//
//    private val _publicShareEditionStatus = MutableLiveData<UIResult<Unit>>()
//    val publicShareEditionStatus: LiveData<UIResult<Unit>> = _publicShareEditionStatus
//
//    fun updatePublicShare(
//        remoteId: Long,
//        name: String,
//        password: String?,
//        expirationDateInMillis: Long,
//        permissions: Int,
//        publicUpload: Boolean
//    ) {
//        _publicShareEditionStatus.postValue(
//            UIResult.loading()
//        )
//
//        viewModelScope.launch {
//            val useCaseResult = withContext(Dispatchers.IO) {
//                editPublicShareUseCase.execute(
//                    EditPublicShareAsyncUseCase.Params(
//                        remoteId,
//                        name,
//                        password,
//                        expirationDateInMillis,
//                        permissions,
//                        publicUpload
//                    )
//                )
//            }
//
//            withContext(Dispatchers.Main) {
//                if (!useCaseResult.isSuccess()) {
//                    _publicShareEditionStatus.postValue(
//                        UIResult.error(
//                            errorMessage = useCaseResult.msg ?: ErrorMessageAdapter.getResultMessage(
//                                useCaseResult.code,
//                                useCaseResult.exception,
//                                OperationType.UPDATE_SHARE,
//                                context.resources
//                            )
//                        )
//                    )
//                } else {
//                    _publicShareEditionStatus.postValue(UIResult.success())
//                }
//            }
//        }
//    }

}
