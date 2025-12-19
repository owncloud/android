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

package com.owncloud.android.data.roles.datasources.implementation

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.roles.datasources.RemoteRolesDataSource
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.lib.resources.roles.responses.RoleResponse

class OCRemoteRolesDataSource(
    private val clientManager: ClientManager
): RemoteRolesDataSource {

    override fun getRoles(accountName: String): List<OCRole> {
        val rolesResponse = executeRemoteOperation {
            clientManager.getRolesService(accountName).getRoles()
        }
        return rolesResponse.map { it.toModel() }
    }

    companion object {
        @VisibleForTesting
        fun RoleResponse.toModel(): OCRole = OCRole(id = id, displayName = displayName)
    }
}
