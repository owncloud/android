/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesús Recio
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

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.domain.exceptions.ShareForbiddenException
import com.owncloud.android.domain.exceptions.ShareNotFoundException
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.ShareResponse
import com.owncloud.android.lib.resources.shares.services.implementation.OCShareService
import com.owncloud.android.testutil.OC_SHARE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteShareDataSourceTest {
    private lateinit var ocRemoteShareDataSource: OCRemoteShareDataSource

    private val ocShareService: OCShareService = mockk()
    private val remoteShareMapper = RemoteShareMapper()
    private val clientManager: ClientManager = mockk(relaxed = true)

    @Before
    fun init() {
        every { clientManager.getShareService(any()) } returns ocShareService

        ocRemoteShareDataSource = OCRemoteShareDataSource(clientManager, remoteShareMapper)
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun `insert private share returns OCShare`() {
        val createRemoteShareOperationResult = createRemoteOperationResultMock(
            ShareResponse(
                listOf(
                    remoteShareMapper.toRemote(
                        OC_SHARE.copy(
                            shareType = ShareType.USER,
                            path = "Photos/",
                            isFolder = true,
                            shareWith = "user",
                            sharedWithDisplayName = "User"
                        )
                    )!!
                )
            ),
            true
        )

        every {
            ocShareService.insertShare(any(), any(), any(), any(), any(), any(), any())
        } returns createRemoteShareOperationResult

        // Insert share on remote datasource
        val privateShareAdded = ocRemoteShareDataSource.insert(
            remoteFilePath = "Photos/",
            shareType = ShareType.USER,
            shareWith = "user",
            permissions = 1,
            accountName = "user@server"
        )

        assertThat(privateShareAdded, notNullValue())

        assertEquals("Photos/", privateShareAdded.path)
        assertEquals(true, privateShareAdded.isFolder)
        assertEquals("user", privateShareAdded.shareWith)
        assertEquals("User", privateShareAdded.sharedWithDisplayName)
        assertEquals(1, privateShareAdded.permissions)

        verify(exactly = 1) {
            ocShareService.insertShare(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test(expected = Exception::class)
    fun `insert private share returns an exception when service receive an exception`() {

        every {
            ocShareService.insertShare(any(), any(), any(), any(), any(), any(), any())
        } throws Exception()

        ocRemoteShareDataSource.insert(
            remoteFilePath = "Photos/",
            shareType = ShareType.USER,
            shareWith = "user",
            permissions = 1,
            accountName = "user@server"
        )
    }

    @Test
    fun `updateShare update a private share returns OCShare`() {
        val updateRemoteShareOperationResult = createRemoteOperationResultMock(
            ShareResponse(
                listOf(
                    remoteShareMapper.toRemote(
                        OC_SHARE.copy(
                            shareType = ShareType.USER,
                            path = "Images/image_1.mp4",
                            shareWith = "user",
                            sharedWithDisplayName = "User",
                            permissions = 17,
                            remoteId = "3"
                        )
                    )!!
                )
            ),
            true
        )

        every {
            ocShareService.updateShare(any(), any(), any(), any(), any())
        } returns updateRemoteShareOperationResult

        // Update share on remote datasource
        val privateShareUpdated = ocRemoteShareDataSource.updateShare(
            remoteId = "3",
            permissions = 17,
            accountName = "user@server"
        )

        assertThat(privateShareUpdated, notNullValue())

        assertEquals("Images/image_1.mp4", privateShareUpdated.path)
        assertEquals("user", privateShareUpdated.shareWith)
        assertEquals("User", privateShareUpdated.sharedWithDisplayName)
        assertEquals(17, privateShareUpdated.permissions)
        assertEquals(false, privateShareUpdated.isFolder)

        verify(exactly = 1) {
            ocShareService.updateShare(any(), any(), any(), any(), any())
        }
    }

    @Test(expected = Exception::class)
    fun `updateShare update a private share returns an exception when service receive an exception`() {

        every {
            ocShareService.updateShare(any(), any(), any(), any(), any())
        } throws Exception()

        ocRemoteShareDataSource.updateShare(
            remoteId = "3",
            permissions = 17,
            accountName = "user@server"
        )
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun `insert public share returns OCShare`() {
        val createRemoteShareOperationResult = createRemoteOperationResultMock(
            ShareResponse(
                listOf(
                    remoteShareMapper.toRemote(
                        OC_SHARE.copy(
                            shareType = ShareType.PUBLIC_LINK,
                            path = "Photos/img1.png",
                            name = "img1 link",
                            shareLink = "http://server:port/s/112ejbhdasyd1"
                        )
                    )!!
                )
            ),
            true
        )

        every {
            ocShareService.insertShare(any(), any(), any(), any(), any(), any(), any())
        } returns createRemoteShareOperationResult

        // Insert share on remote datasource
        val publicShareAdded = ocRemoteShareDataSource.insert(
            "Photos/img1.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            accountName = "user@server"
        )

        assertThat(publicShareAdded, notNullValue())

        assertEquals("", publicShareAdded.shareWith)
        assertEquals(1, publicShareAdded.permissions)
        assertEquals("img1 link", publicShareAdded.name)
        assertEquals("Photos/img1.png", publicShareAdded.path)
        assertEquals(false, publicShareAdded.isFolder)
        assertEquals("http://server:port/s/112ejbhdasyd1", publicShareAdded.shareLink)

        verify(exactly = 1) {
            ocShareService.insertShare(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test(expected = Exception::class)
    fun `insert public share returns an exception when service receive an exception`() {
        every {
            ocShareService.insertShare(any(), any(), any(), any(), any(), any(), any())
        } throws Exception()

        ocRemoteShareDataSource.insert(
            "Photos/img1.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            accountName = "user@server"
        )
    }

    @Test
    fun `updateShare update a public share returns OCShare`() {
        val updateRemoteShareOperationResult = createRemoteOperationResultMock(
            ShareResponse(
                listOf(
                    remoteShareMapper.toRemote(
                        OC_SHARE.copy(
                            shareType = ShareType.PUBLIC_LINK,
                            path = "Videos/video1.mp4",
                            expirationDate = 2000,
                            remoteId = "3",
                            name = "video1 link updated",
                            shareLink = "http://server:port/s/1275farv"
                        )
                    )!!
                )
            ),
            true
        )

        every {
            ocShareService.updateShare(any(), any(), any(), any(), any())
        } returns updateRemoteShareOperationResult

        // Update share on remote datasource
        val publicShareUpdated = ocRemoteShareDataSource.updateShare(
            remoteId = "3",
            permissions = 17,
            accountName = "user@server"
        )

        assertThat(publicShareUpdated, notNullValue())

        assertEquals("video1 link updated", publicShareUpdated.name)
        assertEquals("Videos/video1.mp4", publicShareUpdated.path)
        assertEquals(false, publicShareUpdated.isFolder)
        assertEquals(2000, publicShareUpdated.expirationDate)
        assertEquals(1, publicShareUpdated.permissions)
        assertEquals("http://server:port/s/1275farv", publicShareUpdated.shareLink)

        verify(exactly = 1) {
            ocShareService.updateShare(any(), any(), any(), any(), any())
        }
    }

    @Test(expected = Exception::class)
    fun `updateShare update a public share returns an exception when service receive an exception`() {

        every {
            ocShareService.updateShare(any(), any(), any(), any(), any())
        } throws Exception()

        ocRemoteShareDataSource.updateShare(
            remoteId = "3",
            permissions = 17,
            accountName = "user@server"
        )

    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun `getShares returns a list of OCShare`() {
        val remoteShares = listOf(
            remoteShareMapper.toRemote(
                OC_SHARE.copy(
                    shareType = ShareType.PUBLIC_LINK,
                    path = "/Documents/doc",
                    name = "Doc link",
                    shareLink = "http://server:port/s/1"
                )
            )!!,
            remoteShareMapper.toRemote(
                OC_SHARE.copy(
                    shareType = ShareType.PUBLIC_LINK,
                    path = "/Documents/doc",
                    name = "Doc link 2",
                    shareLink = "http://server:port/s/2"
                )
            )!!,
            remoteShareMapper.toRemote(
                OC_SHARE.copy(
                    shareType = ShareType.USER,
                    path = "/Documents/doc",
                    shareWith = "steve",
                    sharedWithDisplayName = "Steve"
                )
            )!!,
            remoteShareMapper.toRemote(
                OC_SHARE.copy(
                    shareType = ShareType.GROUP,
                    path = "/Documents/doc",
                    shareWith = "family",
                    sharedWithDisplayName = "My family"
                )
            )!!
        )

        val getRemoteSharesOperationResult = createRemoteOperationResultMock(
            ShareResponse(remoteShares),
            true
        )

        every {
            ocShareService.getShares(any(), any(), any())
        } returns getRemoteSharesOperationResult

        // Get shares from remote datasource
        val shares = ocRemoteShareDataSource.getShares(
            remoteFilePath = "/Documents/doc",
            reshares = true,
            subfiles = true,
            accountName = "user@server"
        )

        assertEquals(4, shares.size)

        val publicShare1 = shares[0]
        assertEquals(ShareType.PUBLIC_LINK, publicShare1.shareType)
        assertEquals("/Documents/doc", publicShare1.path)
        assertEquals(false, publicShare1.isFolder)
        assertEquals("Doc link", publicShare1.name)
        assertEquals("http://server:port/s/1", publicShare1.shareLink)

        val publicShare2 = shares[1]
        assertEquals(ShareType.PUBLIC_LINK, publicShare2.shareType)
        assertEquals("/Documents/doc", publicShare2.path)
        assertEquals(false, publicShare2.isFolder)
        assertEquals("Doc link 2", publicShare2.name)
        assertEquals("http://server:port/s/2", publicShare2.shareLink)

        val userShare = shares[2]
        assertEquals(ShareType.USER, userShare.shareType)
        assertEquals("/Documents/doc", userShare.path)
        assertEquals(false, userShare.isFolder)
        assertEquals("steve", userShare.shareWith)
        assertEquals("Steve", userShare.sharedWithDisplayName)

        val groupShare = shares[3]
        assertEquals(ShareType.GROUP, groupShare.shareType)
        assertEquals("/Documents/doc", groupShare.path)
        assertEquals(false, groupShare.isFolder)
        assertEquals("family", groupShare.shareWith)
        assertEquals("My family", groupShare.sharedWithDisplayName)

        verify(exactly = 1) {
            ocShareService.getShares(
                remoteFilePath = "/Documents/doc",
                reshares = true,
                subfiles = true,
            )
        }
    }

    @Test(expected = Exception::class)
    fun `getShares returns an exception when service receive an exception`() {

        every {
            ocShareService.getShares(any(), any(), any())
        } throws Exception()

        // Get shares from remote datasource
        ocRemoteShareDataSource.getShares(
            remoteFilePath = "/Documents/doc",
            reshares = true,
            subfiles = true,
            accountName = "user@server"
        )

    }

    @Test(expected = ShareNotFoundException::class)
    fun `insert share file not found`() {
        createShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
    }

    @Test(expected = ShareForbiddenException::class)
    fun `insert share forbidden`() {
        createShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
    }

    private fun createShareOperationWithError(resultCode: RemoteOperationResult.ResultCode? = null) {
        val createRemoteSharesOperationResult = createRemoteOperationResultMock(
            ShareResponse(arrayListOf()),
            false,
            null,
            resultCode
        )

        every {
            ocShareService.insertShare(any(), any(), any(), any(), any(), any(), any())
        } returns createRemoteSharesOperationResult

        ocRemoteShareDataSource.insert(
            "Photos/img2.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            accountName = "user@server"
        )
    }

    @Test(expected = ShareNotFoundException::class)
    fun `update share file not found`() {
        updateShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
    }

    @Test(expected = ShareForbiddenException::class)
    fun `update share forbidden`() {
        updateShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
    }

    private fun updateShareOperationWithError(resultCode: RemoteOperationResult.ResultCode? = null) {
        val updateRemoteShareOperationResult = createRemoteOperationResultMock(
            ShareResponse(arrayListOf()),
            false,
            null,
            resultCode
        )

        every {
            ocShareService.updateShare(any(), any(), any(), any(), any())
        } returns updateRemoteShareOperationResult

        ocRemoteShareDataSource.updateShare(
            "3",
            permissions = 17,
            accountName = "user@server"
        )
    }

    @Test
    fun `deleteShare delete a share returns unit`() {
        val removeRemoteShareOperationResult = createRemoteOperationResultMock(
            Unit,
            isSuccess = true
        )

        every {
            ocShareService.deleteShare(any())
        } returns removeRemoteShareOperationResult

        ocRemoteShareDataSource.deleteShare(remoteId = "3", accountName = "user@server")

        verify(exactly = 1) {
            ocShareService.deleteShare(any())
        }
    }

    @Test(expected = ShareNotFoundException::class)
    fun `remove share file not found`() {
        deleteShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
    }

    @Test(expected = ShareForbiddenException::class)
    fun `remove share forbidden`() {
        deleteShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
    }

    private fun deleteShareOperationWithError(resultCode: RemoteOperationResult.ResultCode? = null) {
        val removeRemoteShareOperationResult = createRemoteOperationResultMock(
            Unit,
            false,
            null,
            resultCode
        )

        every {
            ocShareService.deleteShare(any())
        } returns removeRemoteShareOperationResult

        ocRemoteShareDataSource.deleteShare(remoteId = "1", accountName = "user@server")
    }
}
