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

package com.owncloud.android.presentation.spaces.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.domain.members.usecases.AddMemberUseCase
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMembers
import com.owncloud.android.domain.spaces.usecases.GetSpaceMembersUseCase
import com.owncloud.android.domain.roles.usecases.GetRolesAsyncUseCase
import com.owncloud.android.domain.spaces.usecases.GetSpacePermissionsAsyncUseCase
import com.owncloud.android.domain.members.usecases.SearchMembersUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpaceMembersViewModel(
    private val addMemberUseCase: AddMemberUseCase,
    private val getRolesAsyncUseCase: GetRolesAsyncUseCase,
    private val getSpaceMembersUseCase: GetSpaceMembersUseCase,
    private val getSpacePermissionsAsyncUseCase: GetSpacePermissionsAsyncUseCase,
    private val searchMembersUseCase: SearchMembersUseCase,
    private val accountName: String,
    private val space: OCSpace,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
): ViewModel() {

    private val _roles = MutableStateFlow<Event<UIResult<List<OCRole>>>?>(null)
    val roles: StateFlow<Event<UIResult<List<OCRole>>>?> = _roles

    private val _spaceMembers = MutableStateFlow<Event<UIResult<SpaceMembers>>?>(null)
    val spaceMembers: StateFlow<Event<UIResult<SpaceMembers>>?> = _spaceMembers

    private val _spacePermissions = MutableStateFlow<Event<UIResult<List<String>>>?>(null)
    val spacePermissions: StateFlow<Event<UIResult<List<String>>>?> = _spacePermissions

    private val _members: MutableSharedFlow<MembersUIState> = MutableSharedFlow()
    val members: SharedFlow<MembersUIState> = _members

    private val _addMemberUIState = MutableStateFlow<AddMemberUIState?>(null)
    val addMemberUIState: StateFlow<AddMemberUIState?> = _addMemberUIState

    private val _addMemberResultFlow = MutableStateFlow<Event<UIResult<Unit>>?>(null)
    val addMemberResultFlow: StateFlow<Event<UIResult<Unit>>?> = _addMemberResultFlow

    private var searchJob: Job? = null

    init {
        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            flow = _roles,
            useCase = getRolesAsyncUseCase,
            useCaseParams = GetRolesAsyncUseCase.Params(accountName = accountName),
            showLoading = false,
            requiresConnection = true
        )
        getSpacePermissions()

    }

    fun getSpacePermissions() = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        flow = _spacePermissions,
        useCase = getSpacePermissionsAsyncUseCase,
        useCaseParams = GetSpacePermissionsAsyncUseCase.Params(accountName = accountName, spaceId = space.id),
        showLoading = false,
        requiresConnection = true
    )

    fun getSpaceMembers() = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        flow = _spaceMembers,
        useCase = getSpaceMembersUseCase,
        useCaseParams = GetSpaceMembersUseCase.Params(accountName = accountName, spaceId = space.id),
        showLoading = false,
        requiresConnection = true
    )

    fun searchMembers(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(coroutineDispatcherProvider.io) {
            _members.emit(MembersUIState(members = emptyList(), isLoading = true , error = null))
            when (val result = searchMembersUseCase(SearchMembersUseCase.Params(accountName, query))) {
                is UseCaseResult.Success -> _members.emit(MembersUIState(members = result.data, isLoading = false, error = null))
                is UseCaseResult.Error -> _members.emit(MembersUIState(members = emptyList(), isLoading = false, error = result.getThrowableOrNull()))
            }
        }
    }

    fun clearSearch() {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            _members.emit(MembersUIState(members = emptyList(), isLoading = false , error = null))
        }
    }

    fun onMemberSelected(member: OCMember) {
        _addMemberUIState.value = AddMemberUIState(selectedMember = member)
    }

    fun onRoleSelected(role: OCRole) {
        _addMemberUIState.update { it?.copy(selectedRole = role) }
    }

    fun onExpirationDateSelected(expirationDate: String?) {
        _addMemberUIState.update { it?.copy(selectedExpirationDate = expirationDate) }
    }

    fun addMember(member: OCMember, roleId: String, expirationDate: String?) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            flow = _addMemberResultFlow,
            useCase = addMemberUseCase,
            useCaseParams = AddMemberUseCase.Params(
                accountName = accountName,
                spaceId = space.id,
                member = member,
                roleId = roleId,
                expirationDate = expirationDate
            )
        )
    }

    data class MembersUIState (
        val members: List<OCMember>,
        val isLoading: Boolean,
        val error: Throwable?
    )

    data class AddMemberUIState(
        val selectedMember: OCMember? = null,
        val selectedRole: OCRole? = null,
        val selectedExpirationDate: String? = null
    )
}
