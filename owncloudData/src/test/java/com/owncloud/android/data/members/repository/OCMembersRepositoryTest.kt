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
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_GROUP_MEMBER
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.testutil.OC_USER_MEMBER
import com.owncloud.android.testutil.SPACE_MEMBERS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Test

class OCMembersRepositoryTest {

    private val remoteMembersDataSource = mockk<RemoteMembersDataSource>()
    private val ocMembersRepository = OCMembersRepository(remoteMembersDataSource)

    private val query = "dev"

    @Test
    fun `addMember adds a member to a space correctly`() {
        every {
            remoteMembersDataSource.addMember(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id, OC_USER_MEMBER, SPACE_MEMBERS.roles[0].id, null)
        } returns Unit

        ocMembersRepository.addMember(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id, OC_USER_MEMBER, SPACE_MEMBERS.roles[0].id, null)

        verify(exactly = 1) {
            remoteMembersDataSource.addMember(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id, OC_USER_MEMBER, SPACE_MEMBERS.roles[0].id, null)
        }
    }

    @Test
    fun `removeMember removes a member from a space correctly`() {
        every {
            remoteMembersDataSource.removeMember(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id, SPACE_MEMBERS.members[0].id)
        } returns Unit

        ocMembersRepository.removeMember(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id, SPACE_MEMBERS.members[0].id)

        verify(exactly = 1) {
            remoteMembersDataSource.removeMember(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id, SPACE_MEMBERS.members[0].id)
        }
    }

    @Test
    fun `searchMembers returns a list of OCMember`() {
        every {
            remoteMembersDataSource.searchUsers(OC_ACCOUNT_NAME, query)
        } returns listOf(OC_USER_MEMBER)

        every {
            remoteMembersDataSource.searchGroups(OC_ACCOUNT_NAME, query)
        } returns listOf(OC_GROUP_MEMBER)

        val listOfMembers = ocMembersRepository.searchMembers(OC_ACCOUNT_NAME, query)
        assertEquals(listOf(OC_USER_MEMBER, OC_GROUP_MEMBER), listOfMembers)

        verify(exactly = 1) {
            remoteMembersDataSource.searchUsers(OC_ACCOUNT_NAME, query)
            remoteMembersDataSource.searchGroups(OC_ACCOUNT_NAME, query)
        }
    }
}
