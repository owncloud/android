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

package com.owncloud.android.presentation.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.exceptions.IncompleteFileDataException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.domain.members.usecases.SearchMembersUseCase
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.roles.usecases.GetRolesAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.AddGraphShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetGraphSharesAsyncUseCase
import com.owncloud.android.domain.sharing.shares.model.OCPermissions
import com.owncloud.android.domain.user.usecases.GetUserIdAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GraphShareViewModel(
    private val addGraphShareAsyncUseCase: AddGraphShareAsyncUseCase,
    private val getRolesAsyncUseCase: GetRolesAsyncUseCase,
    private val getGraphSharesAsyncUseCase: GetGraphSharesAsyncUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val searchMembersUseCase: SearchMembersUseCase,
    private val getUserIdAsyncUseCase: GetUserIdAsyncUseCase,
    private val accountName: String,
    private val file: OCFile,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _roles = MutableStateFlow<Event<UIResult<List<OCRole>>>?>(null)
    val roles: StateFlow<Event<UIResult<List<OCRole>>>?> = _roles

    private val _shares = MutableStateFlow<Event<UIResult<OCPermissions>>?>(null)
    val shares: StateFlow<Event<UIResult<OCPermissions>>?> = _shares

    private val _userId = MutableStateFlow<Event<UIResult<String>>?>(null)
    val userId: StateFlow<Event<UIResult<String>>?> = _userId

    private val _members: MutableSharedFlow<MembersUIState> = MutableSharedFlow()
    val members: SharedFlow<MembersUIState> = _members

    private val _addShareUIState = MutableStateFlow<AddShareUIState?>(null)
    val addShareUIState: StateFlow<AddShareUIState?> = _addShareUIState

    private val _addShareResultFlow = MutableStateFlow<Event<UIResult<Unit>>?>(null)
    val addShareResultFlow: StateFlow<Event<UIResult<Unit>>?> = _addShareResultFlow

    private var searchJob: Job? = null
    var capabilities: OCCapability? = null

    init {
        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            flow = _roles,
            useCase = getRolesAsyncUseCase,
            useCaseParams = GetRolesAsyncUseCase.Params(accountName = accountName),
        )
        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            showLoading = false,
            flow = _userId,
            useCase = getUserIdAsyncUseCase,
            useCaseParams = GetUserIdAsyncUseCase.Params(accountName = accountName)
        )
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            capabilities = getStoredCapabilitiesUseCase(GetStoredCapabilitiesUseCase.Params(accountName))
        }
    }

    fun getGraphShares() {
        val spaceId = file.spaceId
        val itemId = file.remoteId
        if (spaceId == null || itemId == null) {
            _shares.update { Event(UIResult.Error(error = IncompleteFileDataException())) }
            return
        }

        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            showLoading = true,
            flow = _shares,
            useCase = getGraphSharesAsyncUseCase,
            useCaseParams = GetGraphSharesAsyncUseCase.Params(
                accountName = accountName,
                spaceId = spaceId,
                itemId = itemId
            )
        )
    }

    fun addGraphShare(member: OCMember, roleId: String) {
        val spaceId = file.spaceId
        val itemId = file.remoteId
        if (spaceId == null || itemId == null) {
            _addShareResultFlow.update { Event(UIResult.Error(error = IncompleteFileDataException())) }
            return
        }

        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            flow = _addShareResultFlow,
            useCase = addGraphShareAsyncUseCase,
            useCaseParams = AddGraphShareAsyncUseCase.Params(
                accountName = accountName,
                spaceId = spaceId,
                itemId = itemId,
                member = member,
                roleId = roleId,
                expirationDate = _addShareUIState.value?.selectedExpirationDate
            )
        )
    }

    fun searchMembers(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(coroutineDispatcherProvider.io) {
            delay(SEARCH_DELAY_MS)
            _members.emit(MembersUIState(members = emptyList(), isLoading = true, error = null))
            when (val result = searchMembersUseCase(SearchMembersUseCase.Params(accountName, query))) {
                is UseCaseResult.Success -> _members.emit(MembersUIState(members = result.data, isLoading = false, error = null))
                is UseCaseResult.Error -> _members.emit(MembersUIState(members = emptyList(), isLoading = false, error = result.getThrowableOrNull()))
            }
        }
    }

    fun clearSearch() {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            _members.emit(MembersUIState(members = emptyList(), isLoading = false, error = null))
        }
    }

    fun onMemberSelected(member: OCMember) {
        _addShareUIState.value = AddShareUIState(selectedMember = member)
    }

    fun onRoleSelected(role: OCRole) {
        _addShareUIState.update { it?.copy(selectedRole = role) }
    }

    fun onExpirationDateSelected(expirationDate: String?) {
        _addShareUIState.update { it?.copy(selectedExpirationDate = expirationDate) }
    }

    fun resetViewModel() {
        _addShareUIState.value = null
        _addShareResultFlow.value = null
    }

    data class MembersUIState(
        val members: List<OCMember>,
        val isLoading: Boolean,
        val error: Throwable?
    )

    data class AddShareUIState(
        val selectedMember: OCMember? = null,
        val selectedRole: OCRole? = null,
        val selectedExpirationDate: String? = null
    )

    companion object {
        private const val SEARCH_DELAY_MS = 500L
    }
}
