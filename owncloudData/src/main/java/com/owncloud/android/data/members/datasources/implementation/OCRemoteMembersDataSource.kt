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
import com.owncloud.android.domain.members.model.OCMemberType
import com.owncloud.android.lib.resources.members.responses.MemberResponse

class OCRemoteMembersDataSource (
    private val clientManager: ClientManager
): RemoteMembersDataSource {
    override fun addMember(accountName: String, spaceId: String, member: OCMember, roleId: String, expirationDate: String?) {
        val memberType = OCMemberType.toString(member.type).lowercase()
        executeRemoteOperation { clientManager.getMembersService(accountName).addMember(spaceId, member.id, memberType, roleId, expirationDate) }
    }

    override fun searchGroups(accountName: String, query: String): List<OCMember> {
        val groupsResponse = executeRemoteOperation { clientManager.getMembersService(accountName).searchGroups(query) }
        return groupsResponse.map { it.toModel(isGroup = true) }
    }

    override fun searchUsers(accountName: String, query: String): List<OCMember> {
        val usersResponse = executeRemoteOperation { clientManager.getMembersService(accountName).searchUsers(query) }
        return usersResponse.map { it.toModel(isGroup = false) }
    }

    companion object {

        @VisibleForTesting
        fun MemberResponse.toModel(isGroup: Boolean): OCMember {
            val surname = surname ?: if (isGroup) OCMemberType.GROUP_TYPE_STRING else OCMemberType.USER_TYPE_STRING
            return OCMember(
                id = id,
                displayName = displayName,
                surname = surname,
                type = OCMemberType.parseFromString(surname)
            )
        }
    }
}
