/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.presentation.sharing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
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
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResultAndUseCachedData
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
class ShareViewModel(
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
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _shares = MediatorLiveData<Event<UIResult<List<OCShare>>>>()
    val shares: LiveData<Event<UIResult<List<OCShare>>>> = _shares

    private var sharesLiveData: LiveData<List<OCShare>> = getSharesAsLiveDataUseCase(
        GetSharesAsLiveDataUseCase.Params(filePath = filePath, accountName = accountName)
    )

    private var capabilities: OCCapability? = null

    init {
        _shares.addSource(sharesLiveData) { shares ->
            _shares.postValue(Event(UIResult.Success(shares)))
        }

        refreshSharesFromNetwork()

        viewModelScope.launch(coroutineDispatcherProvider.io) {
            capabilities = getStoredCapabilitiesUseCase(
                GetStoredCapabilitiesUseCase.Params(
                    accountName = accountName
                )
            )
        }
    }

    fun refreshSharesFromNetwork() = runUseCaseWithResultAndUseCachedData(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        cachedData = sharesLiveData.value,
        liveData = _shares,
        useCase = refreshSharesFromServerAsyncUseCase,
        useCaseParams = RefreshSharesFromServerAsyncUseCase.Params(
            filePath = filePath,
            accountName = accountName
        )
    )

    private val _shareDeletionStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val shareDeletionStatus: LiveData<Event<UIResult<Unit>>> = _shareDeletionStatus

    fun deleteShare(
        remoteId: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        liveData = _shareDeletionStatus,
        useCase = deletePublicShareUseCase,
        useCaseParams = DeleteShareAsyncUseCase.Params(
            remoteId = remoteId,
            accountName = accountName,
        ),
        postSuccess = false
    )

    fun isResharingAllowed() = capabilities?.filesSharingResharing?.isTrue ?: false

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private val _privateShareCreationStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val privateShareCreationStatus: LiveData<Event<UIResult<Unit>>> = _privateShareCreationStatus

    fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String, // User or group name of the target sharee.
        permissions: Int,
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        liveData = _privateShareCreationStatus,
        useCase = createPrivateShareUseCase,
        useCaseParams = CreatePrivateShareAsyncUseCase.Params(
            filePath,
            shareType,
            shareeName,
            permissions,
            accountName
        ),
        postSuccessWithData = false
    )

    private val _privateShare = MediatorLiveData<Event<UIResult<OCShare>>>()
    val privateShare: LiveData<Event<UIResult<OCShare>>> = _privateShare

    // Used to get a specific private share after updating it
    fun refreshPrivateShare(
        remoteId: String
    ) {
        val privateShareLiveData = getShareAsLiveDataUseCase(
            GetShareAsLiveDataUseCase.Params(remoteId)
        )

        _privateShare.addSource(privateShareLiveData) { privateShare ->
            _privateShare.postValue(Event(UIResult.Success(privateShare)))
        }
    }

    private val _privateShareEditionStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val privateShareEditionStatus: LiveData<Event<UIResult<Unit>>> = _privateShareEditionStatus

    fun updatePrivateShare(
        remoteId: String,
        permissions: Int,
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        liveData = _privateShareEditionStatus,
        useCase = editPrivateShareUseCase,
        useCaseParams = EditPrivateShareAsyncUseCase.Params(
            remoteId,
            permissions,
            accountName
        ),
        postSuccess = false
    )

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private val _publicShareCreationStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val publicShareCreationStatus: LiveData<Event<UIResult<Unit>>> = _publicShareCreationStatus

    fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        liveData = _publicShareCreationStatus,
        useCase = createPublicShareUseCase,
        useCaseParams = CreatePublicShareAsyncUseCase.Params(
            filePath,
            permissions,
            name,
            password,
            expirationTimeInMillis,
            accountName
        ),
        postSuccessWithData = false
    )

    private val _publicShareEditionStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val publicShareEditionStatus: LiveData<Event<UIResult<Unit>>> = _publicShareEditionStatus

    fun updatePublicShare(
        remoteId: String,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        liveData = _publicShareEditionStatus,
        useCase = editPublicShareUseCase,
        useCaseParams = EditPublicShareAsyncUseCase.Params(
            remoteId,
            name,
            password,
            expirationDateInMillis,
            permissions,
            accountName
        ),
        postSuccessWithData = false
    )
}
