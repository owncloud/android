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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.domain.sharing.shares.usecases.CreatePrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.CreatePublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.DeleteShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetShareAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetSharesAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshSharesFromServerAsyncUseCase
import com.owncloud.android.presentation.UIResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
class OCShareViewModel(
    private val filePath: String,
    private val accountName: String,
    getSharesAsLiveDataUseCase: GetSharesAsLiveDataUseCase,
    private val getShareAsLiveDataUseCase: GetShareAsLiveDataUseCase,
    private val refreshSharesFromServerAsyncUseCase: RefreshSharesFromServerAsyncUseCase,
    private val createPrivateShareUseCase: CreatePrivateShareAsyncUseCase,
    private val editPrivateShareUseCase: EditPrivateShareAsyncUseCase,
    private val createPublicShareUseCase: CreatePublicShareAsyncUseCase,
    private val editPublicShareUseCase: EditPublicShareAsyncUseCase,
    private val deletePublicShareUseCase: DeleteShareAsyncUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _shares = MediatorLiveData<UIResult<List<OCShare>>>()
    val shares: LiveData<UIResult<List<OCShare>>> = _shares

    private var sharesLiveData: LiveData<List<OCShare>?>? = getSharesAsLiveDataUseCase.execute(
        GetSharesAsLiveDataUseCase.Params(
            filePath = filePath,
            accountName = accountName
        )
    )

    init {
        sharesLiveData?.let {
            _shares.addSource(it) { shares ->
                _shares.postValue(UIResult.Success(shares))
            }
        }

        refreshSharesFromNetwork()
    }

    fun refreshSharesFromNetwork() {
        viewModelScope.launch {
            _shares.postValue(
                UIResult.Loading(sharesLiveData?.value)
            )

            val useCaseResult = withContext(ioDispatcher) {
                refreshSharesFromServerAsyncUseCase.execute(
                    RefreshSharesFromServerAsyncUseCase.Params(
                        filePath = filePath,
                        accountName = accountName
                    )
                )
            }

            if (!useCaseResult.isSuccess) {
                _shares.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull(), sharesLiveData?.value)

                )
            }
        }
    }

    private val _shareDeletionStatus = MutableLiveData<UIResult<Unit>>()
    val shareDeletionStatus: LiveData<UIResult<Unit>> = _shareDeletionStatus

    fun deleteShare(
        remoteId: Long
    ) {
        viewModelScope.launch {
            _shareDeletionStatus.postValue(
                UIResult.Loading()
            )

            val useCaseResult = withContext(ioDispatcher) {
                deletePublicShareUseCase.execute(
                    DeleteShareAsyncUseCase.Params(
                        remoteId
                    )
                )
            }

            if (!useCaseResult.isSuccess) {
                _shareDeletionStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            } else {
                _shareDeletionStatus.postValue(UIResult.Success())
            }
        }
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private val _privateShareCreationStatus = MutableLiveData<UIResult<Unit>>()
    val privateShareCreationStatus: LiveData<UIResult<Unit>> = _privateShareCreationStatus

    fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String, // User or group name of the target sharee.
        permissions: Int,
        accountName: String
    ) {
        viewModelScope.launch {
            _privateShareCreationStatus.postValue(
                UIResult.Loading()
            )

            val useCaseResult = withContext(ioDispatcher) {
                createPrivateShareUseCase.execute(
                    CreatePrivateShareAsyncUseCase.Params(
                        filePath,
                        shareType,
                        shareeName,
                        permissions,
                        accountName
                    )
                )
            }

            if (!useCaseResult.isSuccess) {
                _privateShareCreationStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            }
        }
    }

    private val _privateShare = MediatorLiveData<UIResult<OCShare>>()
    val privateShare: LiveData<UIResult<OCShare>> = _privateShare

    private var privateShareLiveData: LiveData<OCShare>? = null

    // Used to get a specific private share after updating it
    fun refreshPrivateShare(
        remoteId: Long
    ) {
        privateShareLiveData = getShareAsLiveDataUseCase.execute(
            GetShareAsLiveDataUseCase.Params(remoteId)
        )

        privateShareLiveData?.let {
            _privateShare.addSource(it) { privateShare ->
                _privateShare.postValue(UIResult.Success(privateShare))
            }
        }
    }

    private val _privateShareEditionStatus = MutableLiveData<UIResult<Unit>>()
    val privateShareEditionStatus: LiveData<UIResult<Unit>> = _privateShareEditionStatus

    fun updatePrivateShare(
        remoteId: Long,
        permissions: Int,
        accountName: String
    ) {
        viewModelScope.launch {
            _privateShareEditionStatus.postValue(
                UIResult.Loading()
            )

            val useCaseResult = withContext(ioDispatcher) {
                editPrivateShareUseCase.execute(
                    EditPrivateShareAsyncUseCase.Params(
                        remoteId,
                        permissions,
                        accountName
                    )
                )
            }

            if (!useCaseResult.isSuccess) {
                _privateShareEditionStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            } else {
                _privateShareEditionStatus.postValue(UIResult.Success())
            }
        }
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private val _publicShareCreationStatus = MutableLiveData<UIResult<Unit>>()
    val publicShareCreationStatus: LiveData<UIResult<Unit>> = _publicShareCreationStatus

    fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean,
        accountName: String
    ) {
        viewModelScope.launch {
            _publicShareCreationStatus.postValue(
                UIResult.Loading()
            )

            val useCaseResult = withContext(ioDispatcher) {
                createPublicShareUseCase.execute(
                    CreatePublicShareAsyncUseCase.Params(
                        filePath,
                        permissions,
                        name,
                        password,
                        expirationTimeInMillis,
                        publicUpload,
                        accountName
                    )
                )
            }

            if (!useCaseResult.isSuccess) {
                _publicShareCreationStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            } else {
                _publicShareCreationStatus.postValue(UIResult.Success())
            }
        }
    }

    private val _publicShareEditionStatus = MutableLiveData<UIResult<Unit>>()
    val publicShareEditionStatus: LiveData<UIResult<Unit>> = _publicShareEditionStatus

    fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        accountName: String
    ) {
        viewModelScope.launch {
            _publicShareEditionStatus.postValue(
                UIResult.Loading()
            )

            val useCaseResult = withContext(ioDispatcher) {
                editPublicShareUseCase.execute(
                    EditPublicShareAsyncUseCase.Params(
                        remoteId,
                        name,
                        password,
                        expirationDateInMillis,
                        permissions,
                        publicUpload,
                        accountName
                    )
                )
            }

            if (!useCaseResult.isSuccess) {
                _publicShareEditionStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            } else {
                _publicShareEditionStatus.postValue(UIResult.Success())
            }
        }
    }
}
