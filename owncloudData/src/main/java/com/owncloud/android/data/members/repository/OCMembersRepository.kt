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

package com.owncloud.android.data.members.repository

import com.owncloud.android.data.members.datasources.RemoteMembersDataSource
import com.owncloud.android.domain.members.MembersRepository
import com.owncloud.android.domain.members.model.OCMember

class OCMembersRepository(
    private val remoteMembersDataSource: RemoteMembersDataSource
): MembersRepository {

    override fun addMember(accountName: String, spaceId: String, member: OCMember, roleId: String, expirationDate: String?) {
        remoteMembersDataSource.addMember(accountName, spaceId, member, roleId, expirationDate)
    }

    override fun editMember(accountName: String, spaceId: String, memberId: String, roleId: String, expirationDate: String?) {
        remoteMembersDataSource.editMember(accountName, spaceId, memberId, roleId, expirationDate)
    }

    override fun removeMember(accountName: String, spaceId: String, memberId: String) {
        remoteMembersDataSource.removeMember(accountName, spaceId, memberId)
    }

    override fun searchMembers(accountName: String, query: String): List<OCMember> =
        remoteMembersDataSource.searchUsers(accountName, query) + remoteMembersDataSource.searchGroups(accountName, query)
}
