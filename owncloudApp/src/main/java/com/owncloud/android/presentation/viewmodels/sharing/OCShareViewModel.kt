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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
    private val deletePublicShareUseCase: DeleteShareAsyncUseCase
) : ViewModel() {

    private val _shares = MutableLiveData<UIResult<List<OCShare>>>()
    val shares: LiveData<UIResult<List<OCShare>>> = _shares

    private var sharesLiveData: LiveData<List<OCShare>?> = getSharesAsLiveDataUseCase.execute(
        GetSharesAsLiveDataUseCase.Params(
            filePath = filePath,
            accountName = accountName
        )
    )

    // To detect changes in shares
    private val sharesObserver: Observer<List<OCShare>?> = Observer { shares ->
        _shares.postValue(UIResult.Success(shares))
    }

    init {
        sharesLiveData.observeForever(sharesObserver)
    }

    fun refreshSharesFromNetwork() {
        viewModelScope.launch {
            _shares.postValue(
                UIResult.Loading(sharesLiveData.value)
            )

            val useCaseResult = withContext(Dispatchers.IO) {
                refreshSharesFromServerAsyncUseCase.execute(
                    RefreshSharesFromServerAsyncUseCase.Params(
                        filePath = filePath,
                        accountName = accountName
                    )
                )
            }

            if (!useCaseResult.isSuccess) {
                _shares.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull(), sharesLiveData.value)
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

            val useCaseResult = withContext(Dispatchers.IO) {
                deletePublicShareUseCase.execute(
                    DeleteShareAsyncUseCase.Params(
                        remoteId
                    )
                )
            }

            if (useCaseResult.isSuccess) {
                _shareDeletionStatus.postValue(UIResult.Success())
            } else {
                _shareDeletionStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
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

            val useCaseResult = withContext(Dispatchers.IO) {
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

            if (useCaseResult.isSuccess) {
                _privateShareCreationStatus.postValue(
                    UIResult.Success()
                )
            } else {
                _privateShareCreationStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            }
        }
    }

    private val _privateShare = MutableLiveData<UIResult<OCShare>>()
    val privateShare: LiveData<UIResult<OCShare>> = _privateShare

    private var privateShareLiveData: LiveData<OCShare>? = null
    private lateinit var privateShareObserver: Observer<OCShare>

    // Used to get a specific private share after updating it
    fun refreshPrivateShare(
        remoteId: Long
    ) {
        privateShareLiveData = getShareAsLiveDataUseCase.execute(
            GetShareAsLiveDataUseCase.Params(remoteId)
        )

        privateShareObserver = Observer { privateShare ->
            _privateShare.postValue(UIResult.Success(privateShare))
        }

        privateShareLiveData?.observeForever(privateShareObserver)
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

            val useCaseResult = withContext(Dispatchers.IO) {
                editPrivateShareUseCase.execute(
                    EditPrivateShareAsyncUseCase.Params(
                        remoteId,
                        permissions,
                        accountName
                    )
                )
            }

            if (useCaseResult.isSuccess) {
                _privateShareEditionStatus.postValue(UIResult.Success())
            } else {
                _privateShareEditionStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
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

            val useCaseResult = withContext(Dispatchers.IO) {
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

            if (useCaseResult.isSuccess) {
                _publicShareCreationStatus.postValue(UIResult.Success())
            } else {
                _publicShareCreationStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
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

            val useCaseResult = withContext(Dispatchers.IO) {
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

            if (useCaseResult.isSuccess) {
                _publicShareEditionStatus.postValue(UIResult.Success())
            } else {
                _publicShareEditionStatus.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull())
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sharesLiveData.removeObserver(sharesObserver)
        privateShareLiveData?.removeObserver(privateShareObserver)
    }
}
