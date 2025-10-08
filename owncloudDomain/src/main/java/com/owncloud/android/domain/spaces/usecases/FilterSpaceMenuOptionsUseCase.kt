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

class FilterSpaceMenuOptionsUseCase(
    private val getSpacePermissionsAsyncUseCase: GetSpacePermissionsAsyncUseCase,
) : BaseUseCase<MutableList<SpaceMenuOption>, FilterSpaceMenuOptionsUseCase.Params>() {

    override fun run(params: Params): MutableList<SpaceMenuOption> {
        val optionsToShow = mutableListOf<SpaceMenuOption>()

        val editPermission = if (params.editSpacesPermission) {
            true
        } else {
            when (val spacePermissionsResult =
                getSpacePermissionsAsyncUseCase(GetSpacePermissionsAsyncUseCase.Params(params.accountName, params.space.id))) {
                is UseCaseResult.Success -> DRIVES_MANAGE_PERMISSION in spacePermissionsResult.data
                is UseCaseResult.Error -> false
            }
        }

        if (editPermission) {
            optionsToShow.add(SpaceMenuOption.EDIT)
        }

        return optionsToShow
    }

    data class Params(
        val accountName: String,
        val space: OCSpace,
        val editSpacesPermission: Boolean
    )

    companion object {
        private const val DRIVES_MANAGE_PERMISSION = "libre.graph/driveItem/permissions/update"
    }
}
