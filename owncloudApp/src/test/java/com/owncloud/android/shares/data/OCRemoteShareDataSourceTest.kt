/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesús Recio
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.shares.data

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation
import com.owncloud.android.shares.data.datasources.OCRemoteShareDataSource
import com.owncloud.android.utils.TestUtil
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCRemoteShareDataSourceTest {
    private lateinit var ocRemoteSharesDataSource: OCRemoteShareDataSource
    private val ownCloudClient = mock(OwnCloudClient::class.java)

    @Before
    fun init() {
        ocRemoteSharesDataSource = OCRemoteShareDataSource(ownCloudClient)
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun insertPrivateShare() {
        val createShareOperation = mock(CreateRemoteShareOperation::class.java)

        val createRemoteShareOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    TestUtil.createRemoteShare(
                        shareType = ShareType.USER.value,
                        path = "Photos/",
                        isFolder = true,
                        shareWith = "user",
                        sharedWithDisplayName = "User"
                    )
                )
            ),
            true
        )

        `when`(createShareOperation.execute(ownCloudClient)).thenReturn(
            createRemoteShareOperationResult
        )

        // Insert share on remote datasource
        val remoteOperationResult = ocRemoteSharesDataSource.insertShare(
            remoteFilePath = "Photos/",
            shareType = ShareType.USER,
            shareWith = "user",
            permissions = 1,
            createRemoteShareOperation = createShareOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(1, remoteOperationResult.data.shares.size)

        val privateShareAdded = remoteOperationResult.data.shares[0]

        assertEquals("Photos/", privateShareAdded.path)
        assertEquals(true, privateShareAdded.isFolder)
        assertEquals("user", privateShareAdded.shareWith)
        assertEquals("User", privateShareAdded.sharedWithDisplayName)
        assertEquals(1, privateShareAdded.permissions)
    }

    @Test
    fun updatePrivateShare() {
        val updateRemoteShareOperation = mock(UpdateRemoteShareOperation::class.java)

        val updateRemoteShareOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    TestUtil.createRemoteShare(
                        shareType = ShareType.USER.value,
                        path = "Images/image_1.mp4",
                        shareWith = "user",
                        sharedWithDisplayName = "User",
                        permissions = 17,
                        isFolder = false,
                        remoteId = 3
                    )
                )
            ),
            true
        )

        `when`(updateRemoteShareOperation.execute(ownCloudClient)).thenReturn(
            updateRemoteShareOperationResult
        )

        // Update share on remote datasource
        val remoteOperationResult = ocRemoteSharesDataSource.updateShare(
            remoteId = 3,
            permissions = 17,
            updateRemoteShareOperation = updateRemoteShareOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(1, remoteOperationResult.data.shares.size)

        val privateShareUpdated = remoteOperationResult.data.shares[0]

        assertEquals("Images/image_1.mp4", privateShareUpdated.path)
        assertEquals("user", privateShareUpdated.shareWith)
        assertEquals("User", privateShareUpdated.sharedWithDisplayName)
        assertEquals(17, privateShareUpdated.permissions)
        assertEquals(false, privateShareUpdated.isFolder)
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun insertPublicShare() {
        val createShareOperation = mock(CreateRemoteShareOperation::class.java)

        val createRemoteShareOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    TestUtil.createRemoteShare(
                        shareType = ShareType.PUBLIC_LINK.value,
                        path = "Photos/img1.png",
                        isFolder = false,
                        name = "img1 link",
                        shareLink = "http://server:port/s/112ejbhdasyd1"
                    )
                )
            ),
            true
        )

        `when`(createShareOperation.execute(ownCloudClient)).thenReturn(
            createRemoteShareOperationResult
        )

        // Insert share on remote datasource
        val remoteOperationResult = ocRemoteSharesDataSource.insertShare(
            "Photos/img1.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            "img1 link",
            "1234",
            -1,
            false,
            createShareOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(1, remoteOperationResult.data.shares.size)

        val publicShareAdded = remoteOperationResult.data.shares[0]

        assertEquals("", publicShareAdded.shareWith)
        assertEquals(1, publicShareAdded.permissions)
        assertEquals("img1 link", publicShareAdded.name)
        assertEquals("Photos/img1.png", publicShareAdded.path)
        assertEquals(false, publicShareAdded.isFolder)
        assertEquals("http://server:port/s/112ejbhdasyd1", publicShareAdded.shareLink)
    }

    @Test
    fun insertPublicShareNoFile() {
        val createRemoteShareOperation = mock(CreateRemoteShareOperation::class.java)

        val httpPhrase = "Wrong path, file/folder doesn't exist"
        val createRemoteSharesOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            httpPhrase,
            RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
        )

        `when`(createRemoteShareOperation.execute(ownCloudClient)).thenReturn(
            createRemoteSharesOperationResult
        )

        val remoteOperationResult = ocRemoteSharesDataSource.insertShare(
            "Photos/img2.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            "img2 link",
            "5678",
            -1,
            false,
            createRemoteShareOperation
        )

        val publicSharesAdded = remoteOperationResult.data

        assertEquals(0, publicSharesAdded.shares.size)
        assertEquals(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND, remoteOperationResult.code)
        assertEquals(httpPhrase, remoteOperationResult.httpPhrase)
    }

    @Test
    fun updatePublicShare() {
        val updateRemoteShareOperation = mock(UpdateRemoteShareOperation::class.java)

        val updateRemoteShareOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    TestUtil.createRemoteShare(
                        shareType = ShareType.PUBLIC_LINK.value,
                        path = "Videos/video1.mp4",
                        expirationDate = 2000,
                        isFolder = false,
                        remoteId = 3,
                        name = "video1 link updated",
                        shareLink = "http://server:port/s/1275farv"
                    )
                )
            ),
            true
        )

        `when`(updateRemoteShareOperation.execute(ownCloudClient)).thenReturn(
            updateRemoteShareOperationResult
        )

        // Update share on remote datasource
        val remoteOperationResult = ocRemoteSharesDataSource.updateShare(
            3,
            "Videos/video1.mp4",
            "1234",
            2000,
            1,
            false,
            updateRemoteShareOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(1, remoteOperationResult.data.shares.size)

        val publicShareUpdated = remoteOperationResult.data.shares[0]

        assertEquals("video1 link updated", publicShareUpdated.name)
        assertEquals("Videos/video1.mp4", publicShareUpdated.path)
        assertEquals(false, publicShareUpdated.isFolder)
        assertEquals(2000, publicShareUpdated.expirationDate)
        assertEquals(1, publicShareUpdated.permissions)
        assertEquals("http://server:port/s/1275farv", publicShareUpdated.shareLink)
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun readRemoteShares() {
        val getRemoteSharesForFileOperation = mock(GetRemoteSharesForFileOperation::class.java)

        val remoteShares = arrayListOf(
            TestUtil.createRemoteShare(
                shareType = ShareType.PUBLIC_LINK.value,
                path = "/Documents/doc",
                isFolder = false,
                name = "Doc link",
                shareLink = "http://server:port/s/1"
            ),
            TestUtil.createRemoteShare(
                shareType = ShareType.PUBLIC_LINK.value,
                path = "/Documents/doc",
                isFolder = false,
                name = "Doc link 2",
                shareLink = "http://server:port/s/2"
            ),
            TestUtil.createRemoteShare(
                shareType = ShareType.USER.value,
                path = "/Documents/doc",
                isFolder = false,
                shareWith = "steve",
                sharedWithDisplayName = "Steve"
            ),
            TestUtil.createRemoteShare(
                shareType = ShareType.GROUP.value,
                path = "/Documents/doc",
                isFolder = false,
                shareWith = "family",
                sharedWithDisplayName = "My family"
            )
        )

        val getRemoteSharesOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(remoteShares),
            true
        )

        `when`(getRemoteSharesForFileOperation.execute(ownCloudClient)).thenReturn(
            getRemoteSharesOperationResult
        )

        // Get shares from remote datasource
        val remoteOperationResult = ocRemoteSharesDataSource.getShares(
            "/Documents/doc",
            true,
            true,
            getRemoteSharesForFileOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(4, remoteOperationResult.data.shares.size)

        val publicShare1 = remoteOperationResult.data.shares[0]
        assertEquals(ShareType.PUBLIC_LINK, publicShare1.shareType)
        assertEquals("/Documents/doc", publicShare1.path)
        assertEquals(false, publicShare1.isFolder)
        assertEquals("Doc link", publicShare1.name)
        assertEquals("http://server:port/s/1", publicShare1.shareLink)

        val publicShare2 = remoteOperationResult.data.shares[1]
        assertEquals(ShareType.PUBLIC_LINK, publicShare2.shareType)
        assertEquals("/Documents/doc", publicShare2.path)
        assertEquals(false, publicShare2.isFolder)
        assertEquals("Doc link 2", publicShare2.name)
        assertEquals("http://server:port/s/2", publicShare2.shareLink)

        val userShare = remoteOperationResult.data.shares[2]
        assertEquals(ShareType.USER, userShare.shareType)
        assertEquals("/Documents/doc", userShare.path)
        assertEquals(false, userShare.isFolder)
        assertEquals("steve", userShare.shareWith)
        assertEquals("Steve", userShare.sharedWithDisplayName)

        val groupShare = remoteOperationResult.data.shares[3]
        assertEquals(ShareType.GROUP, groupShare.shareType)
        assertEquals("/Documents/doc", groupShare.path)
        assertEquals(false, groupShare.isFolder)
        assertEquals("family", groupShare.shareWith)
        assertEquals("My family", groupShare.sharedWithDisplayName)
    }

    @Test
    fun deleteShare() {
        val removeRemoteShareOperation = mock(RemoveRemoteShareOperation::class.java)

        val removeRemoteShareOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            isSuccess = true
        )

        `when`(removeRemoteShareOperation.execute(ownCloudClient)).thenReturn(
            removeRemoteShareOperationResult
        )

        val remoteOperationResult = ocRemoteSharesDataSource.deleteShare(1, removeRemoteShareOperation)

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(true, remoteOperationResult.isSuccess)
    }
}
