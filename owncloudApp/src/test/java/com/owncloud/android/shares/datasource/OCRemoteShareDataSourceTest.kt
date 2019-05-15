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

package com.owncloud.android.shares.datasource

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation
import com.owncloud.android.utils.TestUtil
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCRemoteShareDataSourceTest {
    private lateinit var ocRemoteSharesDataSource: OCRemoteSharesDataSource
    private val ownCloudClient = mock(OwnCloudClient::class.java)

    @Before
    fun init() {
        ocRemoteSharesDataSource = OCRemoteSharesDataSource(ownCloudClient)
    }

    @Test
    fun readRemotePublicShares() {
        val getRemoteSharesForFileOperation = mock(GetRemoteSharesForFileOperation::class.java)

        val remoteShares = arrayListOf(
            TestUtil.createRemoteShare(
                path = "/Photos/",
                isFolder = true,
                name = "Photos folder link",
                shareLink = "http://server:port/s/1"
            ),
            TestUtil.createRemoteShare(
                path = "/Photos/image1.jpg",
                isFolder = false,
                name = "Image 1 link",
                shareLink = "http://server:port/s/2"
            ),
            TestUtil.createRemoteShare(
                path = "/Photos/image2.jpg",
                isFolder = false,
                name = "Image 2 link",
                shareLink = "http://server:port/s/3"
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
        val remoteOperationResult = ocRemoteSharesDataSource.getSharesForFile(
            "/test",
            true,
            true,
            getRemoteSharesForFileOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(remoteOperationResult.data.shares.size, 3)

        val publicShare1 = remoteOperationResult.data.shares.get(0)

        assertEquals(publicShare1.path, "/Photos/")
        assertEquals(publicShare1.isFolder, true)
        assertEquals(publicShare1.name, "Photos folder link")
        assertEquals(publicShare1.shareLink, "http://server:port/s/1")

        val publicShare2 = remoteOperationResult.data.shares.get(1)

        assertEquals(publicShare2.path, "/Photos/image1.jpg")
        assertEquals(publicShare2.isFolder, false)
        assertEquals(publicShare2.name, "Image 1 link")
        assertEquals(publicShare2.shareLink, "http://server:port/s/2")

        val publicShare3 = remoteOperationResult.data.shares.get(2)

        assertEquals(publicShare3.path, "/Photos/image2.jpg")
        assertEquals(publicShare3.isFolder, false)
        assertEquals(publicShare3.name, "Image 2 link")
        assertEquals(publicShare3.shareLink, "http://server:port/s/3")
    }

    @Test
    fun insertPublicShares() {
        val createSharesForFileOperation = mock(CreateRemoteShareOperation::class.java)

        val createRemoteSharesOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    TestUtil.createRemoteShare(
                        path = "Photos/img1.png",
                        isFolder = false,
                        name = "img1 link",
                        shareLink = "http://server:port/s/112ejbhdasyd1"
                    )
                )
            ),
            true
        )

        `when`(createSharesForFileOperation.execute(ownCloudClient)).thenReturn(
            createRemoteSharesOperationResult
        )

        // Insert share on remote datasource
        val remoteOperationResult = ocRemoteSharesDataSource.insertShareForFile(
            "Photos/img1.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            "img1 link",
            "1234",
            -1,
            false,
            createSharesForFileOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(remoteOperationResult.data.shares.size, 1)

        val publicShareAdded = remoteOperationResult.data.shares.get(0)

        assertEquals(publicShareAdded.shareWith, "")
        assertEquals(publicShareAdded.permissions, 1)
        assertEquals(publicShareAdded.name, "img1 link")
        assertEquals(publicShareAdded.path, "Photos/img1.png")
        assertEquals(publicShareAdded.isFolder, false)
        assertEquals(publicShareAdded.shareLink, "http://server:port/s/112ejbhdasyd1")
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

        val remoteOperationResult = ocRemoteSharesDataSource.insertShareForFile(
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

        assertEquals(publicSharesAdded.shares.size, 0)
        assertEquals(remoteOperationResult.code, RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
        assertEquals(remoteOperationResult.httpPhrase, httpPhrase)
    }

    @Test
    fun updatePublicShare() {
        val updateRemoteShareOperation = mock(UpdateRemoteShareOperation::class.java)

        val updateRemoteShareOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    TestUtil.createRemoteShare(
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
        val remoteOperationResult = ocRemoteSharesDataSource.updateShareForFile(
            3,
            "Videos/video1.mp4",
            "1234",
            2000,
            1,
            false,
            updateRemoteShareOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(remoteOperationResult.data.shares.size, 1)

        val publicShareUpdated = remoteOperationResult.data.shares[0]

        assertEquals(publicShareUpdated.name, "video1 link updated")
        assertEquals(publicShareUpdated.path, "Videos/video1.mp4")
        assertEquals(publicShareUpdated.isFolder, false)
        assertEquals(publicShareUpdated.expirationDate, 2000)
        assertEquals(publicShareUpdated.permissions, 1)
        assertEquals(publicShareUpdated.shareLink, "http://server:port/s/1275farv")
    }
}
