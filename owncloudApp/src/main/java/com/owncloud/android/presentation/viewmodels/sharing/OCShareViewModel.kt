/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch

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
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _shares = MediatorLiveData<Event<UIResult<List<OCShare>>>>()
    val shares: LiveData<Event<UIResult<List<OCShare>>>> = _shares

    private var sharesLiveData: LiveData<List<OCShare>> = getSharesAsLiveDataUseCase.execute(
        GetSharesAsLiveDataUseCase.Params(filePath = filePath, accountName = accountName)
    )

    init {
        _shares.addSource(sharesLiveData) { shares ->
            _shares.postValue(Event(UIResult.Success(shares)))
        }

        refreshSharesFromNetwork()
    }

    private fun refreshSharesFromNetwork() {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            viewModelScope.launch(coroutineDispatcherProvider.main) {
                _shares.postValue(Event(UIResult.Loading(sharesLiveData.value)))
            }

            val useCaseResult = refreshSharesFromServerAsyncUseCase.execute(
                RefreshSharesFromServerAsyncUseCase.Params(
                    filePath = filePath,
                    accountName = accountName
                )
            )

            viewModelScope.launch(coroutineDispatcherProvider.main) {
                if (!useCaseResult.isSuccess) {
                    _shares.postValue(
                        Event(UIResult.Error(useCaseResult.getThrowableOrNull(), sharesLiveData.value))
                    )
                }
            }
        }
    }

    private val _shareDeletionStatus = MutableLiveData<Event<UIResult<Unit>>>()
    val shareDeletionStatus: LiveData<Event<UIResult<Unit>>> = _shareDeletionStatus

    fun deleteShare(
        remoteId: Long
    ) {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            viewModelScope.launch(coroutineDispatcherProvider.main) {
                _shareDeletionStatus.postValue(
                    Event(UIResult.Loading())
                )
            }

            val useCaseResult = deletePublicShareUseCase.execute(
                DeleteShareAsyncUseCase.Params(
                    remoteId
                )
            )

            viewModelScope.launch(coroutineDispatcherProvider.main) {
                if (useCaseResult.isError) {
                    _shareDeletionStatus.postValue(Event(UIResult.Error(useCaseResult.getThrowableOrNull())))
                }
            }
        }
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private val _privateShareCreationStatus = MutableLiveData<Event<UIResult<Unit>>>()
    val privateShareCreationStatus: LiveData<Event<UIResult<Unit>>> = _privateShareCreationStatus

    fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String, // User or group name of the target sharee.
        permissions: Int,
        accountName: String
    ) {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            viewModelScope.launch(coroutineDispatcherProvider.main) {
                _privateShareCreationStatus.postValue(
                    Event(UIResult.Loading())
                )
            }

            val useCaseResult = createPrivateShareUseCase.execute(
                CreatePrivateShareAsyncUseCase.Params(
                    filePath,
                    shareType,
                    shareeName,
                    permissions,
                    accountName
                )
            )

            viewModelScope.launch(coroutineDispatcherProvider.main) {
                if (useCaseResult.isSuccess) {
                    _privateShareCreationStatus.postValue(Event(UIResult.Success()))
                } else {
                    _privateShareCreationStatus.postValue(
                        Event(UIResult.Error(useCaseResult.getThrowableOrNull()))
                    )
                }
            }
        }
    }

    private val _privateShare = MediatorLiveData<Event<UIResult<OCShare>>>()
    val privateShare: LiveData<Event<UIResult<OCShare>>> = _privateShare

    // Used to get a specific private share after updating it
    fun refreshPrivateShare(
        remoteId: Long
    ) {
        val privateShareLiveData = getShareAsLiveDataUseCase.execute(
            GetShareAsLiveDataUseCase.Params(remoteId)
        )

        _privateShare.addSource(privateShareLiveData) { privateShare ->
            _privateShare.postValue(Event(UIResult.Success(privateShare)))
        }
    }

    private val _privateShareEditionStatus = MutableLiveData<Event<UIResult<Unit>>>()
    val privateShareEditionStatus: LiveData<Event<UIResult<Unit>>> = _privateShareEditionStatus

    fun updatePrivateShare(
        remoteId: Long,
        permissions: Int,
        accountName: String
    ) {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            viewModelScope.launch(coroutineDispatcherProvider.main) {
                _privateShareEditionStatus.postValue(
                    Event(UIResult.Loading())
                )
            }

            val useCaseResult = editPrivateShareUseCase.execute(
                EditPrivateShareAsyncUseCase.Params(
                    remoteId,
                    permissions,
                    accountName
                )
            )

            viewModelScope.launch(coroutineDispatcherProvider.main) {
                if (useCaseResult.isError) {
                    _privateShareEditionStatus.postValue(Event(UIResult.Error(useCaseResult.getThrowableOrNull())))
                }
            }
        }
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private val _publicShareCreationStatus = MutableLiveData<Event<UIResult<Unit>>>()
    val publicShareCreationStatus: LiveData<Event<UIResult<Unit>>> = _publicShareCreationStatus

    fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean,
        accountName: String
    ) {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            viewModelScope.launch(coroutineDispatcherProvider.main) {
                _publicShareCreationStatus.postValue(
                    Event(UIResult.Loading())
                )
            }

            val useCaseResult = createPublicShareUseCase.execute(
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

            viewModelScope.launch(coroutineDispatcherProvider.main) {
                if (useCaseResult.isSuccess) {
                    _publicShareCreationStatus.postValue(Event(UIResult.Success()))
                } else {
                    _publicShareCreationStatus.postValue(Event(UIResult.Error(useCaseResult.getThrowableOrNull())))
                }
            }
        }
    }

    private val _publicShareEditionStatus = MutableLiveData<Event<UIResult<Unit>>>()
    val publicShareEditionStatus: LiveData<Event<UIResult<Unit>>> = _publicShareEditionStatus

    fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        accountName: String
    ) {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            viewModelScope.launch(coroutineDispatcherProvider.main) {
                _publicShareEditionStatus.postValue(Event(UIResult.Loading()))
            }

            val useCaseResult = editPublicShareUseCase.execute(
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

            viewModelScope.launch(coroutineDispatcherProvider.main) {
                if (useCaseResult.isSuccess) {
                    _publicShareEditionStatus.postValue(Event(UIResult.Success()))
                } else {
                    _publicShareEditionStatus.postValue(Event(UIResult.Error(useCaseResult.getThrowableOrNull())))
                }
            }
        }
    }
}
