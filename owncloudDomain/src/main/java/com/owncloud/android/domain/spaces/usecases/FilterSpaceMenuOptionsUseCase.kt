/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.domain.spaces.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMenuOption
import com.owncloud.android.domain.user.model.UserPermissions

class FilterSpaceMenuOptionsUseCase(
    private val getSpacePermissionsAsyncUseCase: GetSpacePermissionsAsyncUseCase,
) : BaseUseCase<MutableList<SpaceMenuOption>, FilterSpaceMenuOptionsUseCase.Params>() {

    override fun run(params: Params): MutableList<SpaceMenuOption> {
        val optionsToShow = mutableListOf<SpaceMenuOption>()
        val currentSpace = params.space
        val isSpaceManager = currentSpace.root.role == DRIVES_MANAGER_ROLE

        val spacePermissionsResult = getSpacePermissionsAsyncUseCase(GetSpacePermissionsAsyncUseCase.Params(params.accountName, params.space.id))

        val editPermission =
            (UserPermissions.CAN_EDIT_SPACES in params.userPermissions || hasSpacePermission(spacePermissionsResult, DRIVES_MANAGE_PERMISSION))

        val deletePermission =
            (UserPermissions.CAN_DELETE_SPACES in params.userPermissions || hasSpacePermission(spacePermissionsResult, DRIVES_DELETE_PERMISSION))

        val editImagePermission = hasSpacePermission(spacePermissionsResult, DRIVES_MANAGE_PERMISSION)

        if (editPermission || isSpaceManager) {
            optionsToShow.add(SpaceMenuOption.EDIT)
        }

        if (editImagePermission) {
            optionsToShow.add(SpaceMenuOption.EDIT_IMAGE)
        }

        if (!currentSpace.isDisabled && deletePermission) {
            optionsToShow.add(SpaceMenuOption.DISABLE)
        }

        if (currentSpace.isDisabled) {
            optionsToShow.add(SpaceMenuOption.ENABLE)
            optionsToShow.add(SpaceMenuOption.DELETE)
        }

        return optionsToShow
    }

    private fun hasSpacePermission(spacePermissions: UseCaseResult<List<String>>, requiredPermission: String) =
        when (spacePermissions) {
            is UseCaseResult.Success -> requiredPermission in spacePermissions.data
            is UseCaseResult.Error -> false
        }

    data class Params(
        val accountName: String,
        val space: OCSpace,
        val userPermissions: Set<UserPermissions>
    )

    companion object {
        private const val DRIVES_MANAGE_PERMISSION = "libre.graph/driveItem/permissions/update"
        private const val DRIVES_DELETE_PERMISSION = "libre.graph/driveItem/permissions/delete"
        private const val DRIVES_MANAGER_ROLE = "manager"
    }
}
