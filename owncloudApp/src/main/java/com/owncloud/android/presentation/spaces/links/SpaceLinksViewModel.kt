/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.presentation.spaces.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.links.model.OCLinkType
import com.owncloud.android.domain.links.usecases.AddLinkUseCase
import com.owncloud.android.domain.links.usecases.EditLinkUseCase
import com.owncloud.android.domain.links.usecases.RemoveLinkUseCase
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpaceLinksViewModel(
    private val addLinkUseCase: AddLinkUseCase,
    private val editLinkUseCase: EditLinkUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val removeLinkUseCase: RemoveLinkUseCase,
    private val accountName: String,
    private val space: OCSpace,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
): ViewModel() {

    private val _addPublicLinkUIState = MutableStateFlow<AddPublicLinkUIState?>(null)
    val addPublicLinkUIState: StateFlow<AddPublicLinkUIState?> = _addPublicLinkUIState

    private val _addLinkResultFlow = MutableStateFlow<Event<UIResult<Unit>>?>(null)
    val addLinkResultFlow: StateFlow<Event<UIResult<Unit>>?> = _addLinkResultFlow

    private val _removeLinkResultFlow = MutableSharedFlow<UIResult<Unit>>()
    val removeLinkResultFlow: SharedFlow<UIResult<Unit>> = _removeLinkResultFlow

    private val _editLinkResultFlow = MutableStateFlow<Event<UIResult<Unit>>?>(null)
    val editLinkResultFlow: StateFlow<Event<UIResult<Unit>>?> = _editLinkResultFlow

    private var capabilities: OCCapability? = null

    init {
        _addPublicLinkUIState.value = AddPublicLinkUIState()
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            capabilities = getStoredCapabilitiesUseCase(GetStoredCapabilitiesUseCase.Params(accountName))
        }
    }

    fun onPermissionSelected(permission: OCLinkType) {
        _addPublicLinkUIState.update { it?.copy(selectedPermission = permission) }
    }

    fun onExpirationDateSelected(expirationDate: String?) {
        _addPublicLinkUIState.update { it?.copy(selectedExpirationDate = expirationDate) }
    }

    fun onPasswordSelected(password: String?, hasPassword: Boolean) {
        _addPublicLinkUIState.update { it?.copy(selectedPassword = password, hasPassword = hasPassword) }
    }

    fun createPublicLink(displayName: String) {
        _addPublicLinkUIState.value?.selectedPermission?.let {
            runUseCaseWithResult(
                coroutineDispatcher = coroutineDispatcherProvider.io,
                flow = _addLinkResultFlow,
                useCase = addLinkUseCase,
                useCaseParams = AddLinkUseCase.Params(
                    accountName = accountName,
                    spaceId = space.id,
                    displayName = displayName,
                    type = it,
                    expirationDate = _addPublicLinkUIState.value?.selectedExpirationDate,
                    password = _addPublicLinkUIState.value?.selectedPassword
                )
            )
        }
    }

    fun removePublicLink(linkId: String) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            sharedFlow = _removeLinkResultFlow,
            useCase = removeLinkUseCase,
            useCaseParams = RemoveLinkUseCase.Params(
                accountName = accountName,
                spaceId = space.id,
                linkId = linkId
            )
        )
    }

    fun editPublicLink(linkId: String, displayName: String) {
        _addPublicLinkUIState.value?.selectedPermission?.let {
            runUseCaseWithResult(
                coroutineDispatcher = coroutineDispatcherProvider.io,
                flow = _editLinkResultFlow,
                useCase = editLinkUseCase,
                useCaseParams = EditLinkUseCase.Params(
                    accountName = accountName,
                    spaceId = space.id,
                    linkId = linkId,
                    displayName = displayName,
                    type = it,
                    expirationDate = _addPublicLinkUIState.value?.selectedExpirationDate,
                )
            )
        }
    }

    fun checkPasswordEnforced(selectedPermission: OCLinkType) =
        when(selectedPermission) {
            OCLinkType.CAN_VIEW -> capabilities?.filesSharingPublicPasswordEnforcedReadOnly == CapabilityBooleanType.TRUE
            OCLinkType.CAN_EDIT -> capabilities?.filesSharingPublicPasswordEnforcedReadWrite == CapabilityBooleanType.TRUE
            OCLinkType.CREATE_ONLY -> capabilities?.filesSharingPublicPasswordEnforcedUploadOnly == CapabilityBooleanType.TRUE
            else -> true
        }

    fun resetViewModel() {
        _addLinkResultFlow.value = null
        _addPublicLinkUIState.value = AddPublicLinkUIState()
        _editLinkResultFlow.value = null
    }

    data class AddPublicLinkUIState(
        val selectedPermission: OCLinkType? = null,
        val selectedExpirationDate: String? = null,
        val selectedPassword: String? = null,
        val hasPassword: Boolean = false
    )
}
