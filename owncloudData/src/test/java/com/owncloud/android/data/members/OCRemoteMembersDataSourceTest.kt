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


package com.owncloud.android.data.members

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.members.datasources.RemoteMembersDataSource
import com.owncloud.android.data.members.datasources.implementation.OCRemoteMembersDataSource
import com.owncloud.android.data.members.datasources.implementation.OCRemoteMembersDataSource.Companion.toModel
import com.owncloud.android.lib.resources.members.services.OCMembersService
import com.owncloud.android.testutil.GROUP_MEMBER_RESPONSE
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.USER_MEMBER_RESPONSE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteMembersDataSourceTest {

    private lateinit var ocRemoteMembersDataSource: RemoteMembersDataSource

    private val ocMembersService: OCMembersService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)

    private val query = "dev"

    @Before
    fun setUp() {
        ocRemoteMembersDataSource = OCRemoteMembersDataSource(clientManager)
        every { clientManager.getMembersService(OC_ACCOUNT_NAME) } returns ocMembersService
    }

    @Test
    fun `searchGroups returns a list with all available groups from the server`() {
        val searchGroupsResult = createRemoteOperationResultMock(listOf(GROUP_MEMBER_RESPONSE), isSuccess = true)

        every {
            ocMembersService.searchGroups(query)
        } returns searchGroupsResult

        val groups = ocRemoteMembersDataSource.searchGroups(OC_ACCOUNT_NAME, query)
        assertEquals(listOf(GROUP_MEMBER_RESPONSE).map { it.toModel(isGroup = true) }, groups)

        verify(exactly = 1) {
            clientManager.getMembersService(OC_ACCOUNT_NAME)
            ocMembersService.searchGroups(query)
        }
    }

    @Test
    fun `searchUsers returns a list with all available users from the server`() {
        val searchUsersResult = createRemoteOperationResultMock(listOf(USER_MEMBER_RESPONSE), isSuccess = true)

        every {
            ocMembersService.searchUsers(query)
        } returns searchUsersResult

        val users = ocRemoteMembersDataSource.searchUsers(OC_ACCOUNT_NAME, query)
        assertEquals(listOf(USER_MEMBER_RESPONSE).map { it.toModel(isGroup = false) }, users)

        verify(exactly = 1) {
            clientManager.getMembersService(OC_ACCOUNT_NAME)
            ocMembersService.searchUsers(query)
        }
    }
}
