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

package com.owncloud.android.shares.datasources

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.utils.mock
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCRemoteSharesDataSourceTest {
    private lateinit var ocRemoteSharesDataSource: OCRemoteSharesDataSource

    private val ownCloudClient = mock(OwnCloudClient::class.java)
    private val getRemoteSharesForFileOperation = mock(GetRemoteSharesForFileOperation::class.java)
    private val remoteOperationResult = mock<RemoteOperationResult<ShareParserResult>>()

    @Before
    fun init() {
        ocRemoteSharesDataSource = OCRemoteSharesDataSource(ownCloudClient)

        `when`(remoteOperationResult.data).thenReturn(
            ShareParserResult(
                arrayListOf(
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
                ),
                "Succeed"
            )
        )

        `when`(getRemoteSharesForFileOperation.execute(ownCloudClient)).thenReturn(
            remoteOperationResult
        )
    }

    @Test
    fun readRemotePublicShares() {
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
}
