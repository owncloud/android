/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.shares.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasource.LocalSharesDataSource
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.util.InstantAppExecutors
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.utils.mock
import com.owncloud.android.vo.Resource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class OCShareRepositoryTest {
    private lateinit var ocShareRepository: OCShareRepository

    private val localSharesDataSource = mock(LocalSharesDataSource::class.java)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun loadPublicSharesForFileFromNetwork() {
        val dbData = MutableLiveData<List<OCShare>>()

        `when`(
            localSharesDataSource.getSharesForFileAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
            )
        ).thenReturn(
            dbData
        )

        val remoteSharesForFile = arrayListOf(
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

        val remoteOperationResult =
            TestUtil.createRemoteOperationResultMock(ShareParserResult(remoteSharesForFile), true)

        val remoteSharesDataSource = RemoteSharesDataSourceTest(remoteOperationResult)

        ocShareRepository =
            OCShareRepository.create(InstantAppExecutors(), localSharesDataSource, remoteSharesDataSource)

        val data = ocShareRepository.loadSharesForFile(
            "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK), reshares = true, subfiles = false
        )

        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        dbData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesForFileAsLiveData(
            "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // Public shares are always retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).insert(
            remoteSharesForFile.map { remoteShare ->
                OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )

        // Observe changes in database livedata when there's a new public share
        val newPublicShare = listOf(
            TestUtil.createPublicShare(
                path = "/Documents/doc1",
                isFolder = false,
                name = "Doc 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        dbData.postValue(
            newPublicShare
        )

        verify(observer).onChanged(Resource.success(newPublicShare))
    }

    @Test
    fun loadEmptyPublicSharesForFileFromNetwork() {
        val dbData = MutableLiveData<List<OCShare>>()

        `when`(
            localSharesDataSource.getSharesForFileAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
            )
        ).thenReturn(
            dbData
        )

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(ShareParserResult(arrayListOf()), true)

        val remoteSharesDataSource = RemoteSharesDataSourceTest(remoteOperationResult)

        ocShareRepository =
            OCShareRepository.create(InstantAppExecutors(), localSharesDataSource, remoteSharesDataSource)

        val data = ocShareRepository.loadSharesForFile(
            "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK), true, false
        )

        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        dbData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesForFileAsLiveData(
            "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // When there's no shares in server for a specific file, delete them locally
        verify(localSharesDataSource).deleteSharesForFile("/Photos/", "admin@server")

        // Observe changes in database livedata when the list of shares is empty
        dbData.postValue(listOf())

        verify(observer).onChanged(Resource.success(listOf()))
    }

    @Test
    fun loadPublicSharesForFileFromNetworkWithError() {
        val dbData = MutableLiveData<List<OCShare>>()

        dbData.value = listOf(
            TestUtil.createPublicShare(
                path = "/Images/image1",
                isFolder = false,
                name = "Image 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        `when`(
            localSharesDataSource.getSharesForFileAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
            )
        ).thenReturn(
            dbData
        )

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.FORBIDDEN,
            exception = exception
        )

        val remoteSharesDataSource = RemoteSharesDataSourceTest(remoteOperationResult)

        ocShareRepository =
            OCShareRepository.create(InstantAppExecutors(), localSharesDataSource, remoteSharesDataSource)

        val data = ocShareRepository.loadSharesForFile(
            "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK), true, false
        )

        // Get public shares from database to observe them
        verify(localSharesDataSource).getSharesForFileAsLiveData(
            "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.FORBIDDEN, dbData.value, exception = exception
            )
        )
    }

    @Test
    fun insertPublicShareForFileOnNetwork() {
        val dbData = MutableLiveData<List<OCShare>>()

        dbData.value = listOf(
            TestUtil.createPublicShare(
                path = "/Videos/",
                isFolder = true,
                name = "Videos folder link 1",
                shareLink = "http://server:port/s/10"
            )
        )

        `when`(
            localSharesDataSource.getSharesForFileAsLiveData(
                "/Videos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
            )
        ).thenReturn(
            dbData
        )

        val justCreatedRemoteShare = arrayListOf(
            TestUtil.createRemoteShare(
                path = "/Videos/",
                isFolder = true,
                name = "Videos folder link 2",
                shareLink = "http://server:port/s/11"
            )
        )

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(justCreatedRemoteShare), true
        )

        val remoteSharesDataSource = RemoteSharesDataSourceTest(remoteOperationResult)

        ocShareRepository =
            OCShareRepository.create(InstantAppExecutors(), localSharesDataSource, remoteSharesDataSource)

        val data = ocShareRepository.insertPublicShareForFile(
            "/Videos/",
            "admin@server",
            1,
            "Videos folder link 2",
            "1234",
            -1,
            true
        )

        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // creating share on server and another one with db shares already updated with just created share)
        verify(localSharesDataSource, times(2)).getSharesForFileAsLiveData(
            "/Videos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // Public shares are always retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).insert(
            justCreatedRemoteShare.map { remoteShare ->
                OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )
    }

    @Test
    fun insertPublicShareForFileOnNetworkWithError() {
        val dbData = MutableLiveData<List<OCShare>>()

        dbData.value = listOf(
            TestUtil.createPublicShare(
                path = "/Slides/budget.pptx",
                isFolder = false,
                name = "Budget link 1",
                shareLink = "http://server:port/s/1"
            )
        )

        `when`(
            localSharesDataSource.getSharesForFileAsLiveData(
                "/Slides/budget.pptx", "user@server", listOf(ShareType.PUBLIC_LINK)
            )
        ).thenReturn(
            dbData
        )

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.SHARE_NOT_FOUND,
            exception = exception
        )

        val remoteSharesDataSource = RemoteSharesDataSourceTest(remoteOperationResult)

        ocShareRepository =
            OCShareRepository.create(InstantAppExecutors(), localSharesDataSource, remoteSharesDataSource)

        val data = ocShareRepository.insertPublicShareForFile(
            "/Slides/budget.pptx",
            "user@server",
            1,
            "Budget link 2",
            "78910",
            -1,
            false
        )

        // Get public shares from database to observe them
        verify(localSharesDataSource).getSharesForFileAsLiveData(
            "/Slides/budget.pptx", "user@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<List<OCShare>>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND, dbData.value, exception = exception
            )
        )
    }
}
