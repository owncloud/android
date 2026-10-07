/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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
import com.owncloud.android.domain.sharing.shares.usecases.CreatePrivateOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.CreatePublicOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.DeleteOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPrivateOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPublicOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetOcsShareAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetOcsSharesAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshOcsSharesFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResultAndUseCachedData
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
class OcsShareViewModel(
    private val filePath: String,
    private val accountName: String,
    getSharesAsLiveDataUseCase: GetOcsSharesAsLiveDataUseCase,
    private val getShareAsLiveDataUseCase: GetOcsShareAsLiveDataUseCase,
    private val refreshSharesFromServerAsyncUseCase: RefreshOcsSharesFromServerAsyncUseCase,
    private val createPrivateShareUseCase: CreatePrivateOcsShareAsyncUseCase,
    private val editPrivateShareUseCase: EditPrivateOcsShareAsyncUseCase,
    private val createPublicShareUseCase: CreatePublicOcsShareAsyncUseCase,
    private val editPublicShareUseCase: EditPublicOcsShareAsyncUseCase,
    private val deletePublicShareUseCase: DeleteOcsShareAsyncUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _shares = MediatorLiveData<Event<UIResult<List<OCShare>>>>()
    val shares: LiveData<Event<UIResult<List<OCShare>>>> = _shares

    private var sharesLiveData: LiveData<List<OCShare>> = getSharesAsLiveDataUseCase(
        GetOcsSharesAsLiveDataUseCase.Params(filePath = filePath, accountName = accountName)
    )

    private var capabilities: OCCapability? = null

    private val _shareDeletionStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val shareDeletionStatus: LiveData<Event<UIResult<Unit>>> = _shareDeletionStatus

    private val _privateShareCreationStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val privateShareCreationStatus: LiveData<Event<UIResult<Unit>>> = _privateShareCreationStatus

    private val _privateShare = MediatorLiveData<Event<UIResult<OCShare>>>()
    val privateShare: LiveData<Event<UIResult<OCShare>>> = _privateShare

    private val _privateShareEditionStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val privateShareEditionStatus: LiveData<Event<UIResult<Unit>>> = _privateShareEditionStatus

    private val _publicShareCreationStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val publicShareCreationStatus: LiveData<Event<UIResult<Unit>>> = _publicShareCreationStatus

    private val _publicShareEditionStatus = MediatorLiveData<Event<UIResult<Unit>>>()
    val publicShareEditionStatus: LiveData<Event<UIResult<Unit>>> = _publicShareEditionStatus



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
        useCaseParams = RefreshOcsSharesFromServerAsyncUseCase.Params(
            filePath = filePath,
            accountName = accountName
        )
    )

    fun deleteShare(
        remoteId: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        liveData = _shareDeletionStatus,
        useCase = deletePublicShareUseCase,
        useCaseParams = DeleteOcsShareAsyncUseCase.Params(
            remoteId = remoteId,
            accountName = accountName,
        ),
        postSuccess = false
    )

    fun isResharingAllowed() = capabilities?.filesSharingResharing?.isTrue ?: false

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

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
        useCaseParams = CreatePrivateOcsShareAsyncUseCase.Params(
            filePath,
            shareType,
            shareeName,
            permissions,
            accountName
        ),
        postSuccessWithData = false
    )

    // Used to get a specific private share after updating it
    fun refreshPrivateShare(
        remoteId: String
    ) {
        val privateShareLiveData = getShareAsLiveDataUseCase(
            GetOcsShareAsLiveDataUseCase.Params(remoteId)
        )

        _privateShare.addSource(privateShareLiveData) { privateShare ->
            _privateShare.postValue(Event(UIResult.Success(privateShare)))
        }
    }

    fun updatePrivateShare(
        remoteId: String,
        permissions: Int,
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        liveData = _privateShareEditionStatus,
        useCase = editPrivateShareUseCase,
        useCaseParams = EditPrivateOcsShareAsyncUseCase.Params(
            remoteId,
            permissions,
            accountName
        ),
        postSuccess = false
    )

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

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
        useCaseParams = CreatePublicOcsShareAsyncUseCase.Params(
            filePath,
            permissions,
            name,
            password,
            expirationTimeInMillis,
            accountName
        ),
        postSuccessWithData = false
    )

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
        useCaseParams = EditPublicOcsShareAsyncUseCase.Params(
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
