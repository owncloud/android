/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.data.shares.datasources

import com.owncloud.android.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.datasources.mapper.RemoteShareeMapper
import com.owncloud.android.domain.sharing.sharees.model.OCSharee
import com.owncloud.android.domain.sharing.sharees.model.ShareeType
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareeService
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.responses.ExactSharees
import com.owncloud.android.lib.resources.shares.responses.ShareeItem
import com.owncloud.android.lib.resources.shares.responses.ShareeOcsResponse
import com.owncloud.android.lib.resources.shares.responses.ShareeValue
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Needs to be instrumented since JSONObject is tied to the Android platform
class OCRemoteShareesDataSourceTest {
    private lateinit var ocRemoteShareesDataSource: OCRemoteShareeDataSource
    private val ocShareeService: OCShareeService = mockk()
    private lateinit var sharees: List<OCSharee>;

    @Before
    fun init() {
        ocRemoteShareesDataSource =
            OCRemoteShareeDataSource(ocShareeService, RemoteShareeMapper())

        val getRemoteShareesOperationResult = createRemoteOperationResultMock(
            REMOTE_SHAREES,
            true
        )

        every {
            ocShareeService.getSharees("user", 1, 30)
        } returns getRemoteShareesOperationResult

        // Get sharees from remote datasource
        sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30
        )
    }

    @Test
    fun `OCSharees List - ok - contains sharees entered as remote sharees`(){
        assertNotNull(sharees)
        assertEquals(5, sharees.size)
    }

    @Test
    fun `OCSharees List - ok - contains exact user match`() {
        val sharee = sharees[0]
        assertEquals(sharee.label, "User");
        assertEquals(sharee.shareType, ShareType.USER)
        assertEquals(sharee.shareWith, "user")
        assertEquals(sharee.additionalInfo, "user1@exact.com")
        assertTrue(sharee.isExactMatch)
    }

    @Test
    fun `OCSharees List - ok - contains one user not exactly matched`() {
        val sharee = sharees[1]
        assertEquals(sharee.label, "User 1");
        assertEquals(sharee.shareType, ShareType.USER)
        assertEquals(sharee.shareWith, "user2")
        assertEquals(sharee.additionalInfo, "user1@mail.com")
        assertFalse(sharee.isExactMatch)
    }

    @Test
    fun `OCShares List - ok - contains one user without additional info`() {
        val sharee = sharees[2]
        assertEquals(sharee.label, "User 2");
        assertEquals(sharee.shareType, ShareType.USER)
        assertEquals(sharee.shareWith, "user")
        assertEquals(sharee.additionalInfo, "")
        assertFalse(sharee.isExactMatch)
    }

    @Test
    fun `OCShares List - ok - contains one remote user`() {
        val sharee = sharees[3]
        assertEquals(sharee.label, "Remoteuser 1");
        assertEquals(sharee.shareType, ShareeType.REMOTE)
        assertEquals(sharee.shareWith, "remoteuser1")
        assertEquals(sharee.additionalInfo, "user1@remote.com")
        assertFalse(sharee.isExactMatch)
    }

    @Test
    fun `OCShares List - ok - contains one group`() {
        val sharee = sharees[4]
        assertEquals(sharee.label, "Group 1");
        assertEquals(sharee.shareType, ShareeType.GROUP)
        assertEquals(sharee.shareWith, "group1")
        assertEquals(sharee.additionalInfo, "group@group.com")
        assertFalse(sharee.isExactMatch)
    }

    companion object {
        val REMOTE_SHAREES = ShareeOcsResponse(
            ExactSharees(arrayListOf(), arrayListOf(), arrayListOf(
                ShareeItem("User", ShareeValue(0, "user", "user@exact.com"))
            )),
            arrayListOf(
                ShareeItem("Group 1", ShareeValue(1, "group1", "group@group.com"))),
            arrayListOf(
                ShareeItem("Remoteuser 1", ShareeValue(6, "remoteuser1", "user1@remote.com"))
            ),
            arrayListOf(
                ShareeItem("User 1", ShareeValue(0, "user1", "user1@mail.com")),
                ShareeItem("User 2", ShareeValue(0, "user2", null))
            )
        )
    }
}
