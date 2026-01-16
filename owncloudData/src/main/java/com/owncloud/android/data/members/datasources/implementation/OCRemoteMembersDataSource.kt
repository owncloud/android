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

package com.owncloud.android.data.members.datasources.implementation

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.members.datasources.RemoteMembersDataSource
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.lib.resources.members.responses.MemberResponse

class OCRemoteMembersDataSource (
    private val clientManager: ClientManager
): RemoteMembersDataSource {

    override fun searchUsers(accountName: String, query: String): List<OCMember> {
        val usersResponse = executeRemoteOperation { clientManager.getMembersService(accountName).searchUsers(query) }
        return usersResponse.map { it.toModel() }
    }

    companion object {
        private const val USER_SURNAME = "User"

        @VisibleForTesting
        fun MemberResponse.toModel(): OCMember = OCMember(id = id, displayName = displayName, surname = surname ?: USER_SURNAME)

    }
}
