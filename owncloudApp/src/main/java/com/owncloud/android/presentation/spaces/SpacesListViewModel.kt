/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.presentation.spaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMenuOption
import com.owncloud.android.domain.spaces.usecases.CreateSpaceUseCase
import com.owncloud.android.domain.spaces.usecases.DisableSpaceUseCase
import com.owncloud.android.domain.spaces.usecases.EditSpaceUseCase
import com.owncloud.android.domain.spaces.usecases.EnableSpaceUseCase
import com.owncloud.android.domain.spaces.usecases.FilterSpaceMenuOptionsUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalAndProjectSpacesWithSpecialsForAccountAsStreamUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalSpacesWithSpecialsForAccountAsStreamUseCase
import com.owncloud.android.domain.spaces.usecases.GetProjectSpacesWithSpecialsForAccountAsStreamUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.domain.user.model.UserPermissions
import com.owncloud.android.domain.user.usecases.GetUserIdAsyncUseCase
import com.owncloud.android.domain.user.usecases.GetUserPermissionsAsyncUseCase
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

class SpacesListViewModel(
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase,
    private val getPersonalSpacesWithSpecialsForAccountAsStreamUseCase: GetPersonalSpacesWithSpecialsForAccountAsStreamUseCase,
    private val getPersonalAndProjectSpacesWithSpecialsForAccountAsStreamUseCase: GetPersonalAndProjectSpacesWithSpecialsForAccountAsStreamUseCase,
    private val getProjectSpacesWithSpecialsForAccountAsStreamUseCase: GetProjectSpacesWithSpecialsForAccountAsStreamUseCase,
    private val getFileByRemotePathUseCase: GetFileByRemotePathUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val getUserIdAsyncUseCase: GetUserIdAsyncUseCase,
    private val getUserPermissionsAsyncUseCase: GetUserPermissionsAsyncUseCase,
    private val createSpaceUseCase: CreateSpaceUseCase,
    private val filterSpaceMenuOptionsUseCase: FilterSpaceMenuOptionsUseCase,
    private val editSpaceUseCase: EditSpaceUseCase,
    private val disableSpaceUseCase: DisableSpaceUseCase,
    private val enableSpaceUseCase: EnableSpaceUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val accountName: String,
    private val showPersonalSpace: Boolean,
) : ViewModel() {

    private val _spacesList: MutableStateFlow<SpacesListUiState> =
        MutableStateFlow(SpacesListUiState(spaces = emptyList(), refreshing = false, error = null, searchFilter = ""))
    val spacesList: StateFlow<SpacesListUiState> = _spacesList

    private val _userId = MutableStateFlow<Event<UIResult<String>>?>(null)
    val userId: StateFlow<Event<UIResult<String>>?> = _userId

    private val _userPermissions = MutableStateFlow<Event<UIResult<List<String>>>?>(null)
    val userPermissions: StateFlow<Event<UIResult<List<String>>>?> = _userPermissions

    private val _menuOptions: MutableSharedFlow<List<SpaceMenuOption>> = MutableSharedFlow()
    val menuOptions: SharedFlow<List<SpaceMenuOption>> = _menuOptions

    private val _createSpaceFlow = MutableSharedFlow<Event<UIResult<Unit>>?>()
    val createSpaceFlow: SharedFlow<Event<UIResult<Unit>>?> = _createSpaceFlow

    private val _editSpaceFlow = MutableSharedFlow<Event<UIResult<Unit>>?>()
    val editSpaceFlow: SharedFlow<Event<UIResult<Unit>>?> = _editSpaceFlow

    private val _disableSpaceFlow = MutableSharedFlow<Event<UIResult<Unit>>?>()
    val disableSpaceFlow: SharedFlow<Event<UIResult<Unit>>?> = _disableSpaceFlow

    private val _enableSpaceFlow = MutableSharedFlow<Event<UIResult<Unit>>?>()
    val enableSpaceFlow: SharedFlow<Event<UIResult<Unit>>?> = _enableSpaceFlow

    private val _deleteSpaceFlow = MutableSharedFlow<Event<UIResult<Unit>>?>()
    val deleteSpaceFlow: SharedFlow<Event<UIResult<Unit>>?> = _deleteSpaceFlow

    init {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            refreshSpacesFromServer()
            val capabilities = getStoredCapabilitiesUseCase(GetStoredCapabilitiesUseCase.Params(accountName))
            if (capabilities?.isSpacesAllowed() == true) {
                getUserId(accountName)
            }
            val isMultiPersonal = capabilities?.spaces?.hasMultiplePersonalSpaces
            val spacesListFlow = if (isMultiPersonal == true) getPersonalSpacesWithSpecialsForAccountAsStreamUseCase(
                GetPersonalSpacesWithSpecialsForAccountAsStreamUseCase.Params(accountName = accountName)
            ) else if (showPersonalSpace) getPersonalAndProjectSpacesWithSpecialsForAccountAsStreamUseCase(
                GetPersonalAndProjectSpacesWithSpecialsForAccountAsStreamUseCase.Params(accountName = accountName)
            ) else getProjectSpacesWithSpecialsForAccountAsStreamUseCase(
                GetProjectSpacesWithSpecialsForAccountAsStreamUseCase.Params(accountName = accountName)
            )
            spacesListFlow.collect { spaces ->
                _spacesList.update { it.copy(spaces = spaces) }
            }
        }
    }

    fun refreshSpacesFromServer() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            _spacesList.update { it.copy(refreshing = true) }
            when (val result = refreshSpacesFromServerAsyncUseCase(RefreshSpacesFromServerAsyncUseCase.Params(accountName))) {
                is UseCaseResult.Success -> _spacesList.update { it.copy(refreshing = false, error = null) }
                is UseCaseResult.Error -> _spacesList.update { it.copy(refreshing = false, error = result.throwable) }
            }
        }
    }

    fun getRootFileForSpace(ocSpace: OCSpace) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = getFileByRemotePathUseCase(
                GetFileByRemotePathUseCase.Params(
                    owner = ocSpace.accountName,
                    remotePath = ROOT_PATH,
                    spaceId = ocSpace.id
                )
            )
            result.getDataOrNull()?.let { rootFolderFromSpace ->
                _spacesList.update { it.copy(rootFolderFromSelectedSpace = rootFolderFromSpace) }
            }
        }
    }

    fun updateSearchFilter(newSearchFilter: String) {
        _spacesList.update { it.copy(searchFilter = newSearchFilter) }
    }

    private fun getUserId(
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = false,
        flow = _userId,
        useCase = getUserIdAsyncUseCase,
        useCaseParams = GetUserIdAsyncUseCase.Params(accountName = accountName)
    )

    fun getUserPermissions(
        accountId: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = false,
        flow = _userPermissions,
        useCase = getUserPermissionsAsyncUseCase,
        useCaseParams = GetUserPermissionsAsyncUseCase.Params(accountName = accountName, accountId = accountId)
    )

    fun createSpace(spaceName: String, spaceSubtitle: String, spaceQuota: Long) {
        runSpaceOperation(
            flow = _createSpaceFlow,
            useCase = createSpaceUseCase,
            useCaseParams = CreateSpaceUseCase.Params(accountName, spaceName, spaceSubtitle, spaceQuota)
        )
    }

    fun editSpace(spaceId: String, spaceName: String, spaceSubtitle: String, spaceQuota: Long?) {
        runSpaceOperation(
            flow = _editSpaceFlow,
            useCase = editSpaceUseCase,
            useCaseParams = EditSpaceUseCase.Params(accountName, spaceId, spaceName, spaceSubtitle, spaceQuota)
        )
    }

    fun filterMenuOptions(space: OCSpace, userPermissions: Set<UserPermissions>) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val result = filterSpaceMenuOptionsUseCase(
                FilterSpaceMenuOptionsUseCase.Params(
                    accountName = accountName,
                    space = space,
                    userPermissions = userPermissions
                )
            )
            _menuOptions.emit(result)
        }
    }

    fun disableSpace(spaceId: String){
        runSpaceOperation(
            flow = _disableSpaceFlow,
            useCase = disableSpaceUseCase,
            useCaseParams = DisableSpaceUseCase.Params(accountName, spaceId, false)
        )
    }

    fun enableSpace(spaceId: String){
        runSpaceOperation(
            flow = _enableSpaceFlow,
            useCase = enableSpaceUseCase,
            useCaseParams = EnableSpaceUseCase.Params(accountName,spaceId)
        )
    }

    fun deleteSpace(spaceId: String){
        runSpaceOperation(
            flow = _deleteSpaceFlow,
            useCase = disableSpaceUseCase,
            useCaseParams = DisableSpaceUseCase.Params(accountName,spaceId,true)
        )
    }

    private fun <Params> runSpaceOperation(
        flow: MutableSharedFlow<Event<UIResult<Unit>>?>,
        useCase: BaseUseCaseWithResult<Unit, Params>,
        useCaseParams: Params,
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            when (val result = useCase(useCaseParams)) {
                is UseCaseResult.Success -> flow.emit(Event(UIResult.Success(result.getDataOrNull())))
                is UseCaseResult.Error -> flow.emit(Event(UIResult.Error(error = result.getThrowableOrNull())))
            }
            refreshSpacesFromServerAsyncUseCase(RefreshSpacesFromServerAsyncUseCase.Params(accountName))
        }
    }

    data class SpacesListUiState(
        val spaces: List<OCSpace>,
        val rootFolderFromSelectedSpace: OCFile? = null,
        val refreshing: Boolean,
        val error: Throwable?,
        val searchFilter: String
    )
}
