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

package com.owncloud.android.domain.shares

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.owncloud.android.data.Resource
import com.owncloud.android.data.sharing.shares.datasources.LocalSharesDataSource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.utils.DomainTestUtil
import com.owncloud.android.domain.sharing.shares.OCShareRepository
import com.owncloud.android.domain.utils.InstantExecutors
import com.owncloud.android.domain.utils.mock
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
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
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val filePath = "/Photos/"

    private val localSharesDataSource = mock(LocalSharesDataSource::class.java)

    private val remoteShares = arrayListOf(
        DomainTestUtil.createRemoteShare(
            shareType = ShareType.PUBLIC_LINK.value, // Public share
            path = filePath,
            isFolder = true,
            name = "Photos folder link",
            shareLink = "http://server:port/s/1"
        ),
        DomainTestUtil.createRemoteShare(
            shareType = ShareType.PUBLIC_LINK.value, // Public share
            path = "${filePath}img",
            isFolder = true,
            name = "Photos folder link 1",
            shareLink = "http://server:port/s/2"
        ),
        DomainTestUtil.createRemoteShare(
            shareType = ShareType.PUBLIC_LINK.value, // Public share
            path = filePath,
            isFolder = true,
            name = "Photos folder link 2",
            shareLink = "http://server:port/s/3"
        ),
        DomainTestUtil.createRemoteShare(
            shareType = ShareType.USER.value, // Private share
            path = filePath,
            permissions = 1,
            isFolder = true,
            shareWith = "username",
            sharedWithDisplayName = "John"
        ),
        DomainTestUtil.createRemoteShare(
            shareType = ShareType.GROUP.value, // Private share
            path = filePath,
            permissions = 3,
            isFolder = true,
            shareWith = "username2",
            sharedWithDisplayName = "Sophie"
        )
    )

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private val privateShare = listOf(
        DomainTestUtil.createPrivateShare(
            path = filePath,
            isFolder = true,
            shareWith = "username2",
            sharedWithDisplayName = "Sophie"
        )
    )

    private val privateShareTypes = listOf(
        ShareType.USER, ShareType.GROUP, ShareType.FEDERATED
    )

    @Test
    fun loadPrivateSharesFromNetwork() {
        val localData = MutableLiveData<List<OCShareEntity>>() // Local shares

        val remoteOperationResult =
            DomainTestUtil.createRemoteOperationResultMock(ShareParserResult(remoteShares), true) // Remote shares

        val privateSharesAsLiveData = loadPrivateSharesAsLiveData(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<List<OCShareEntity>>>>()
        privateSharesAsLiveData.observeForever(observer)

        localData.postValue(null)

        // Get private shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesAsLiveData(
            filePath, "admin@server", privateShareTypes
        )

        // Retrieving shares from server...

        // Public shares are retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).replaceShares(
            remoteShares.map { remoteShare ->
                OCShareEntity.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )

        // Observe changes in database livedata when there's a new public share
        localData.postValue(
            privateShare
        )

        verify(observer).onChanged(Resource.success(privateShare))
    }

    @Test
    fun loadEmptyPrivateSharesFromNetwork() {
        val localData = MutableLiveData<List<OCShareEntity>>()

        val remoteOperationResult =
            DomainTestUtil.createRemoteOperationResultMock(ShareParserResult(arrayListOf()), true)

        val data = loadPrivateSharesAsLiveData(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<List<OCShareEntity>>>>()
        data.observeForever(observer)

        localData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesAsLiveData(
            filePath, "admin@server", privateShareTypes
        )

        // Retrieving public shares from server...

        // When there's no shares in server for a specific file, delete them locally
        verify(localSharesDataSource).deleteSharesForFile(filePath, "admin@server")

        // Observe changes in database livedata when the list of shares is empty
        localData.postValue(listOf())

        verify(observer).onChanged(Resource.success(listOf()))
    }

    @Test
    fun loadPrivateSharesFromNetworkWithError() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = privateShare

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.FORBIDDEN,
            exception = exception
        )

        val data = loadPrivateSharesAsLiveData(localData, remoteOperationResult)

        // Get public shares from database to observe them
        verify(localSharesDataSource).getSharesAsLiveData(
            filePath, "admin@server", privateShareTypes
        )

        // Retrieving public shares from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<List<OCShareEntity>>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.FORBIDDEN, localData.value, exception = exception
            )
        )
    }

    @Test
    fun insertPrivateShareOnNetwork() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = privateShare

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf(remoteShares[3])), true
        )

        val data = insertPrivateShare(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Public shares are retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).insert(
            arrayListOf(remoteShares[3]).map { remoteShare ->
                OCShareEntity.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )
    }

    @Test
    fun insertPrivateShareOnNetworkWithError() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = privateShare

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE,
            exception = exception
        )

        val data = insertPrivateShare(localData, remoteOperationResult)

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE, exception = exception
            )
        )
    }

    @Test
    fun updatePrivateShareOnNetwork() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = privateShare

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf(remoteShares[3])), true
        )

        val data = updatePrivateShare(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Public shares are retrieved from server and updated in database
        verify(localSharesDataSource).update(
            OCShare.fromRemoteShare(remoteShares[3]).also { it.accountOwner = "admin@server" }
        )
    }

    @Test
    fun deletePrivateShareOnNetwork() {
        val localData = MutableLiveData<List<OCShare>>()
        localData.value = privateShare

        val remoteOperationResult = TestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()), true
        )

        val data = deleteShare(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Retrieving public shares from server...
        verify(localSharesDataSource).deleteShare(
            1
        )
    }

    private fun loadPrivateSharesAsLiveData(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<List<OCShare>>> {
        val ocShareRepository = createShareRepositoryWithPrivateData(localData, remoteOperationResult)
        return ocShareRepository.getPrivateShares(filePath)
    }

    private fun createShareRepositoryWithPrivateData(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): OCShareRepository =
        createShareRepositoryWithDataSources(
            localData, remoteOperationResult, privateShareTypes
        )

    private fun insertPrivateShare(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)

        return ocShareRepository.insertPrivateShare(
            filePath,
            ShareType.GROUP,
            "user",
            1
        )
    }

    private fun updatePrivateShare(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPrivateData(localData, remoteOperationResult)

        return ocShareRepository.updatePrivateShare(
            1,
            17
        )
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private val publicShare = listOf(
        DomainTestUtil.createPublicShare(
            path = filePath,
            isFolder = true,
            name = "Photos folder link",
            shareLink = "http://server:port/s/1"
        )
    )

    @Test
    fun loadPublicSharesFromNetworkSuccessfully() {
        val localData = MutableLiveData<List<OCShareEntity>>()

        val remoteOperationResult =
            DomainTestUtil.createRemoteOperationResultMock(ShareParserResult(remoteShares), true)

        val data = loadPublicSharesAsLiveData(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<List<OCShareEntity>>>>()
        data.observeForever(observer)

        localData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesAsLiveData(
            filePath, "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving shares from server...

        // Public shares are retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).replaceShares(
            remoteShares.map { remoteShare ->
                OCShareEntity.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )

        // Observe changes in database livedata when there's a new public share
        localData.postValue(
            publicShare
        )

        verify(observer).onChanged(Resource.success(publicShare))
    }

    @Test
    fun loadEmptyPublicSharesFromNetwork() {
        val localData = MutableLiveData<List<OCShareEntity>>()

        val remoteOperationResult =
            DomainTestUtil.createRemoteOperationResultMock(ShareParserResult(arrayListOf()), true)

        val data = loadPublicSharesAsLiveData(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<List<OCShareEntity>>>>()
        data.observeForever(observer)

        localData.postValue(null)

        // Get public shares from database to observe them, is called twice (one showing current db shares while
        // getting shares from server and another one with db shares already updated with server ones)
        verify(localSharesDataSource, times(2)).getSharesAsLiveData(
            filePath, "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // When there's no shares in server for a specific file, delete them locally
        verify(localSharesDataSource).deleteSharesForFile(filePath, "admin@server")

        // Observe changes in database livedata when the list of shares is empty
        localData.postValue(listOf())

        verify(observer).onChanged(Resource.success(listOf()))
    }

    @Test
    fun loadPublicSharesFromNetworkWithError() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = publicShare

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.FORBIDDEN,
            exception = exception
        )

        val data = loadPublicSharesAsLiveData(localData, remoteOperationResult)

        // Get public shares from database to observe them
        verify(localSharesDataSource).getSharesAsLiveData(
            filePath, "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        // Retrieving public shares from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<List<OCShareEntity>>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.FORBIDDEN, localData.value, exception = exception
            )
        )
    }

    @Test
    fun insertPublicShareOnNetwork() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = publicShare

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf(remoteShares[1])), true
        )

        val data = insertPublicShare(localData, remoteOperationResult)
        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Public shares are retrieved from server and inserted in database if not empty list
        verify(localSharesDataSource).insert(
            arrayListOf(remoteShares[1]).map { remoteShare ->
                OCShareEntity.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
            }
        )
    }

    @Test
    fun insertPublicShareOnNetworkWithError() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = publicShare

        val exception = Exception("Error when retrieving shares")

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()),
            false,
            resultCode = RemoteOperationResult.ResultCode.SHARE_NOT_FOUND,
            exception = exception
        )

        val data = insertPublicShare(localData, remoteOperationResult)

        // Observe changes in database livedata when there's an error from server
        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND, exception = exception
            )
        )
    }

    @Test
    fun updatePublicShareOnNetwork() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = publicShare

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf(remoteShares[2])), true
        )

        val data = updatePublicShare(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Public shares are retrieved from server and updated in database
        verify(localSharesDataSource).update(
            OCShareEntity.fromRemoteShare(remoteShares[2]).also { it.accountOwner = "admin@server" }
        )
    }

    @Test
    fun deletePublicShareOnNetwork() {
        val localData = MutableLiveData<List<OCShareEntity>>()
        localData.value = publicShare

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            ShareParserResult(arrayListOf()), true
        )

        val data = deleteShare(localData, remoteOperationResult)

        val observer = mock<Observer<Resource<Unit>>>()
        data.observeForever(observer)

        // Retrieving public shares from server...
        verify(localSharesDataSource).deleteShare(
            1
        )
    }

    private fun loadPublicSharesAsLiveData(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<List<OCShareEntity>>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)
        return ocShareRepository.getPublicShares(filePath)
    }

    private fun insertPublicShare(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)

        return ocShareRepository.insertPublicShare(
            filePath,
            1,
            "Photos folder link 3",
            "1234",
            -1,
            true
        )
    }

    private fun updatePublicShare(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)

        return ocShareRepository.updatePublicShare(
            1,
            "Photos folder link updated",
            "123456",
            2000,
            1,
            false
        )
    }

    private fun deletePublicShare(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)
        return ocShareRepository.deletePublicShare(
            1
        )
    }

    private fun createShareRepositoryWithPublicData(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): OCShareRepository =
        createShareRepositoryWithDataSources(localData, remoteOperationResult, listOf(ShareType.PUBLIC_LINK))

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    private fun createShareRepositoryWithDataSources(
        localData: MutableLiveData<List<OCShareEntity>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>,
        shareTypes: List<ShareType>
    ): OCShareRepository {
        `when`(
            localSharesDataSource.getSharesAsLiveData(
                filePath, "admin@server", shareTypes
            )
        ).thenReturn(
            localData
        )

        val remoteSharesDataSource = RemoteShareDataSourceTest(remoteOperationResult)

        return OCShareRepository(
            InstantExecutors(),
            localSharesDataSource,
            remoteSharesDataSource,
            "admin@server"
        )
    }

    private fun deleteShare(
        localData: MutableLiveData<List<OCShare>>,
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ): LiveData<Resource<Unit>> {
        val ocShareRepository = createShareRepositoryWithPublicData(localData, remoteOperationResult)
        return ocShareRepository.deleteShare(
            1
        )
    }
}
