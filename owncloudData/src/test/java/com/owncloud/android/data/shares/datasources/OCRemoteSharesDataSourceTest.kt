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

package com.owncloud.android.data.shares.datasources

import com.owncloud.android.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.data.utils.DataTestUtil
import com.owncloud.android.domain.sharing.shares.exceptions.CreateShareFileNotFoundException
import com.owncloud.android.domain.sharing.shares.exceptions.CreateShareForbiddenException
import com.owncloud.android.domain.sharing.shares.exceptions.CreateShareGenericException
import com.owncloud.android.domain.sharing.shares.exceptions.RemoveShareFileNotFoundException
import com.owncloud.android.domain.sharing.shares.exceptions.RemoveShareForbiddenException
import com.owncloud.android.domain.sharing.shares.exceptions.RemoveShareGenericException
import com.owncloud.android.domain.sharing.shares.exceptions.UpdateShareFileNotFoundException
import com.owncloud.android.domain.sharing.shares.exceptions.UpdateShareForbiddenException
import com.owncloud.android.domain.sharing.shares.exceptions.UpdateShareGenericException
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class OCRemoteSharesDataSourceTest {
    private lateinit var ocRemoteSharesDataSource: OCRemoteShareDataSource
    private val ownCloudClient = mockkClass(OwnCloudClient::class)
    private val remoteShareMapper = RemoteShareMapper()

    @Before
    fun init() {
        ocRemoteSharesDataSource = OCRemoteShareDataSource(ownCloudClient, remoteShareMapper)
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun insertPrivateShare() {
        val createShareOperation = mockk<CreateRemoteShareOperation>(relaxed = true)

        val createRemoteShareOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    DataTestUtil.createRemoteShare(
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

        every {
            createShareOperation.execute(ownCloudClient)
        } returns createRemoteShareOperationResult

        // Insert share on remote datasource
        val privateShareAdded = ocRemoteSharesDataSource.insertShare(
            remoteFilePath = "Photos/",
            shareType = ShareType.USER,
            shareWith = "user",
            permissions = 1,
            accountName = "user@server",
            createRemoteShareOperation = createShareOperation
        )

        assertThat(privateShareAdded, notNullValue())

        assertEquals("Photos/", privateShareAdded.path)
        assertEquals(true, privateShareAdded.isFolder)
        assertEquals("user", privateShareAdded.shareWith)
        assertEquals("User", privateShareAdded.sharedWithDisplayName)
        assertEquals(1, privateShareAdded.permissions)
    }

    @Test
    fun updatePrivateShare() {
        val updateRemoteShareOperation = mockk<UpdateRemoteShareOperation>(relaxed = true)

        val updateRemoteShareOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    DataTestUtil.createRemoteShare(
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

        every {
            updateRemoteShareOperation.execute(ownCloudClient)
        } returns updateRemoteShareOperationResult

        // Update share on remote datasource
        val privateShareUpdated = ocRemoteSharesDataSource.updateShare(
            remoteId = 3,
            permissions = 17,
            accountName = "user@server",
            updateRemoteShareOperation = updateRemoteShareOperation
        )

        assertThat(privateShareUpdated, notNullValue())

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
        val createRemoteShareOperation = mockk<CreateRemoteShareOperation>(relaxed = true)

        val createRemoteShareOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    DataTestUtil.createRemoteShare(
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

        every {
            createRemoteShareOperation.execute(ownCloudClient)
        } returns createRemoteShareOperationResult

        // Insert share on remote datasource
        val publicShareAdded = ocRemoteSharesDataSource.insertShare(
            "Photos/img1.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            accountName = "user@server",
            createRemoteShareOperation = createRemoteShareOperation
        )

        assertThat(publicShareAdded, notNullValue())

        assertEquals("", publicShareAdded.shareWith)
        assertEquals(1, publicShareAdded.permissions)
        assertEquals("img1 link", publicShareAdded.name)
        assertEquals("Photos/img1.png", publicShareAdded.path)
        assertEquals(false, publicShareAdded.isFolder)
        assertEquals("http://server:port/s/112ejbhdasyd1", publicShareAdded.shareLink)
    }

    @Test
    fun updatePublicShare() {
        val updateRemoteShareOperation = mockk<UpdateRemoteShareOperation>(relaxed = true)

        val updateRemoteShareOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(
                arrayListOf(
                    DataTestUtil.createRemoteShare(
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

        every {
            updateRemoteShareOperation.execute(ownCloudClient)
        } returns updateRemoteShareOperationResult

        // Update share on remote datasource
        val publicShareUpdated = ocRemoteSharesDataSource.updateShare(
            remoteId = 3,
            permissions = 17,
            accountName = "user@server",
            updateRemoteShareOperation = updateRemoteShareOperation
        )

        assertThat(publicShareUpdated, notNullValue())

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
        val getRemoteSharesForFileOperation = mockkClass(GetRemoteSharesForFileOperation::class)

        val remoteShares = arrayListOf(
            DataTestUtil.createRemoteShare(
                shareType = ShareType.PUBLIC_LINK.value,
                path = "/Documents/doc",
                isFolder = false,
                name = "Doc link",
                shareLink = "http://server:port/s/1"
            ),
            DataTestUtil.createRemoteShare(
                shareType = ShareType.PUBLIC_LINK.value,
                path = "/Documents/doc",
                isFolder = false,
                name = "Doc link 2",
                shareLink = "http://server:port/s/2"
            ),
            DataTestUtil.createRemoteShare(
                shareType = ShareType.USER.value,
                path = "/Documents/doc",
                isFolder = false,
                shareWith = "steve",
                sharedWithDisplayName = "Steve"
            ),
            DataTestUtil.createRemoteShare(
                shareType = ShareType.GROUP.value,
                path = "/Documents/doc",
                isFolder = false,
                shareWith = "family",
                sharedWithDisplayName = "My family"
            )
        )

        val getRemoteSharesOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(remoteShares),
            true
        )

        every {
            getRemoteSharesForFileOperation.execute(ownCloudClient)
        } returns getRemoteSharesOperationResult

        // Get shares from remote datasource
        val shares = ocRemoteSharesDataSource.getShares(
            "/Documents/doc",
            true,
            true,
            "user@server",
            getRemoteSharesForFileOperation
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
    }

    @Test(expected = CreateShareFileNotFoundException::class)
    fun insertShareFileNotFound() {
        createShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
    }

    @Test(expected = CreateShareForbiddenException::class)
    fun insertShareForbidden() {
        createShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
    }

    @Test(expected = CreateShareGenericException::class)
    fun insertShareGenericError() {
        createShareOperationWithError()
    }

    private fun createShareOperationWithError(resultCode: RemoteOperationResult.ResultCode? = null) {
        val createRemoteShareOperation = mockk<CreateRemoteShareOperation>(relaxed = true)

        val createRemoteSharesOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            null,
            resultCode
        )

        every {
            createRemoteShareOperation.execute(ownCloudClient)
        } returns createRemoteSharesOperationResult

        ocRemoteSharesDataSource.insertShare(
            "Photos/img2.png",
            ShareType.PUBLIC_LINK,
            "",
            1,
            accountName = "user@server",
            createRemoteShareOperation = createRemoteShareOperation
        )
    }

    @Test(expected = UpdateShareFileNotFoundException::class)
    fun updateShareFileNotFound() {
        updateShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
    }

    @Test(expected = UpdateShareForbiddenException::class)
    fun updateShareForbidden() {
        updateShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
    }

    @Test(expected = UpdateShareGenericException::class)
    fun updateShareGenericError() {
        updateShareOperationWithError()
    }

    private fun updateShareOperationWithError(resultCode: RemoteOperationResult.ResultCode? = null) {
        val updateRemoteShareOperation = mockk<UpdateRemoteShareOperation>(relaxed = true)

        val updateRemoteShareOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            null,
            resultCode
        )

        every {
            updateRemoteShareOperation.execute(ownCloudClient)
        } returns updateRemoteShareOperationResult

        ocRemoteSharesDataSource.updateShare(
            3,
            permissions = 17,
            accountName = "user@server",
            updateRemoteShareOperation = updateRemoteShareOperation
        )
    }

    @Test
    fun deleteShare() {
        val removeRemoteShareOperation = mockk<RemoveRemoteShareOperation>(relaxed = true)

        val removeRemoteShareOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            isSuccess = true
        )

        every {
            removeRemoteShareOperation.execute(ownCloudClient)
        } returns removeRemoteShareOperationResult

        ocRemoteSharesDataSource.deleteShare(1, removeRemoteShareOperation)
    }

    @Test(expected = RemoveShareFileNotFoundException::class)
    fun removeShareFileNotFound() {
        deleteShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
    }

    @Test(expected = RemoveShareForbiddenException::class)
    fun removeShareForbidden() {
        deleteShareOperationWithError(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
    }

    @Test(expected = RemoveShareGenericException::class)
    fun removeShareGenericError() {
        deleteShareOperationWithError()
    }

    private fun deleteShareOperationWithError(resultCode: RemoteOperationResult.ResultCode? = null) {
        val removeRemoteShareOperation = mockk<RemoveRemoteShareOperation>(relaxed = true)

        val removeRemoteShareOperationResult = DataTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            null,
            resultCode
        )

        every {
            removeRemoteShareOperation.execute(ownCloudClient)
        } returns removeRemoteShareOperationResult

        ocRemoteSharesDataSource.deleteShare(1, removeRemoteShareOperation)
    }
}
