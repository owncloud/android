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

package com.owncloud.android.lib.resources.members.services

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.members.SearchRemoteMembersOperation
import com.owncloud.android.lib.resources.members.responses.MemberResponse

class OCMembersService(override val client: OwnCloudClient) : MembersService {
    override fun searchGroups(query: String): RemoteOperationResult<List<MemberResponse>> =
        SearchRemoteMembersOperation(query, true).execute(client)

    override fun searchUsers(query: String): RemoteOperationResult<List<MemberResponse>> =
        SearchRemoteMembersOperation(query, false).execute(client)

}
