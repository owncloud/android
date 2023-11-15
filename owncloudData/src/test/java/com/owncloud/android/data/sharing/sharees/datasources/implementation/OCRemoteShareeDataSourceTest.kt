/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.data.sharing.sharees.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.sharing.sharees.datasources.mapper.RemoteShareeMapper
import com.owncloud.android.domain.sharing.sharees.model.OCSharee
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.resources.shares.responses.ExactSharees
import com.owncloud.android.lib.resources.shares.responses.ShareeItem
import com.owncloud.android.lib.resources.shares.responses.ShareeOcsResponse
import com.owncloud.android.lib.resources.shares.responses.ShareeValue
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareeService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OCRemoteShareeDataSourceTest {
    private lateinit var ocRemoteShareesDataSource: OCRemoteShareeDataSource
    private val ocShareeService: OCShareeService = mockk()
    private val clientManager: ClientManager = mockk()
    private lateinit var sharees: List<OCSharee>

    @Before
    fun setUp() {
        every { clientManager.getShareeService(OC_ACCOUNT_NAME) } returns ocShareeService
        ocRemoteShareesDataSource =
            OCRemoteShareeDataSource(clientManager, RemoteShareeMapper())

        val getRemoteShareesOperationResult = createRemoteOperationResultMock(
            REMOTE_SHAREES,
            true
        )

        every {
            ocShareeService.getSharees(searchString = "user", page = 1, perPage = 30)
        } returns getRemoteShareesOperationResult
    }

    @Test
    fun `getSharees returns a list of OCSharee entered as remote sharees`() {
        sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30,
            OC_ACCOUNT_NAME,
        )
        assertNotNull(sharees)
        assertEquals(5, sharees.size)

        verify(exactly = 1) {
            clientManager.getShareeService(OC_ACCOUNT_NAME)
            ocShareeService.getSharees(searchString = "user", page = 1, perPage = 30)
        }
    }

    @Test
    fun `getSharees returns a list of OCSharee when contains exact user match`() {
        sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30,
            OC_ACCOUNT_NAME,
        )

        val sharee = sharees[0]
        assertEquals(sharee.label, "User")
        assertEquals(sharee.shareType, ShareType.USER)
        assertEquals(sharee.shareWith, "user")
        assertEquals(sharee.additionalInfo, "user@exact.com")
        assertTrue(sharee.isExactMatch)

        verify(exactly = 1) {
            clientManager.getShareeService(OC_ACCOUNT_NAME)
            ocShareeService.getSharees(searchString = "user", page = 1, perPage = 30)
        }
    }

    @Test
    fun `getSharees returns a list of OCSharee when contains one user not exactly matched`() {
        sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30,
            OC_ACCOUNT_NAME,
        )

        val sharee = sharees[1]
        assertEquals("User 1", sharee.label)
        assertEquals(ShareType.USER, sharee.shareType)
        assertEquals("user1", sharee.shareWith)
        assertEquals("user1@mail.com", sharee.additionalInfo)
        assertFalse(sharee.isExactMatch)

        verify(exactly = 1) {
            clientManager.getShareeService(OC_ACCOUNT_NAME)
            ocShareeService.getSharees(searchString = "user", page = 1, perPage = 30)
        }
    }

    @Test
    fun `getSharees returns a list of OCSharee when contains one user without additional info`() {
        sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30,
            OC_ACCOUNT_NAME,
        )
        val sharee = sharees[2]
        assertEquals("User 2", sharee.label)
        assertEquals(ShareType.USER, sharee.shareType)
        assertEquals("user2", sharee.shareWith)
        assertEquals("", sharee.additionalInfo)
        assertFalse(sharee.isExactMatch)

        verify(exactly = 1) {
            clientManager.getShareeService(OC_ACCOUNT_NAME)
            ocShareeService.getSharees(searchString = "user", page = 1, perPage = 30)
        }
    }

    @Test
    fun `getSharees returns a list of OCSharee when contains one remote user`() {
        sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30,
            OC_ACCOUNT_NAME,
        )
        val sharee = sharees[3]
        assertEquals("Remoteuser 1", sharee.label)
        assertEquals(ShareType.FEDERATED, sharee.shareType)
        assertEquals("remoteuser1", sharee.shareWith)
        assertEquals("user1@remote.com", sharee.additionalInfo)
        assertFalse(sharee.isExactMatch)

        verify(exactly = 1) {
            clientManager.getShareeService(OC_ACCOUNT_NAME)
            ocShareeService.getSharees(searchString = "user", page = 1, perPage = 30)
        }
    }

    @Test
    fun `getSharees returns a list of OCSharee when contains one group`() {
        sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30,
            OC_ACCOUNT_NAME,
        )

        val sharee = sharees[4]
        assertEquals("Group 1", sharee.label)
        assertEquals(ShareType.GROUP, sharee.shareType)
        assertEquals("group1", sharee.shareWith)
        assertEquals("group@group.com", sharee.additionalInfo)
        assertFalse(sharee.isExactMatch)

        verify(exactly = 1) {
            clientManager.getShareeService(OC_ACCOUNT_NAME)
            ocShareeService.getSharees(searchString = "user", page = 1, perPage = 30)
        }
    }

    @Test
    fun `getSharees returns a list of OCSharee when handle empty response`() {

        val getRemoteShareesOperationResult = createRemoteOperationResultMock(
            EMPTY_REMOTE_SHAREES,
            true
        )

        every {
            ocShareeService.getSharees(searchString = "user2", page = 2, perPage = 32)
        } returns getRemoteShareesOperationResult

        val emptySharees = ocRemoteShareesDataSource.getSharees(
            "user2",
            2,
            32,
            OC_ACCOUNT_NAME,
        )

        assertTrue(emptySharees.isEmpty())

        verify(exactly = 1) {
            clientManager.getShareeService(OC_ACCOUNT_NAME)
            ocShareeService.getSharees(searchString = "user2", page = 2, perPage = 32)
        }
    }

    companion object {
        val REMOTE_SHAREES = ShareeOcsResponse(
            ExactSharees(
                arrayListOf(), arrayListOf(), arrayListOf(
                    ShareeItem("User", ShareeValue(0, "user", "user@exact.com"))
                )
            ),
            arrayListOf(
                ShareeItem("Group 1", ShareeValue(1, "group1", "group@group.com"))
            ),
            arrayListOf(
                ShareeItem("Remoteuser 1", ShareeValue(6, "remoteuser1", "user1@remote.com"))
            ),
            arrayListOf(
                ShareeItem("User 1", ShareeValue(0, "user1", "user1@mail.com")),
                ShareeItem("User 2", ShareeValue(0, "user2", null))
            )
        )

        val EMPTY_REMOTE_SHAREES = ShareeOcsResponse(
            ExactSharees(arrayListOf(), arrayListOf(), arrayListOf()),
            arrayListOf(), arrayListOf(), arrayListOf()
        )

    }
}
