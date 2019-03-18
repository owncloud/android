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

package com.owncloud.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.util.InstantAppExecutors
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.utils.mock
import com.owncloud.android.vo.Resource
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class NetworkBoundResourceTest {
    private val dbData = MutableLiveData<List<OCShare>>()

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun basicFromNetwork() {
        val saved = AtomicReference<List<OCShare>>()

        val fetchedDbValue = listOf(
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = true,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        val networkResult = arrayListOf(
            TestUtil.createRemoteShare(
                path = "/Photos/image.jpg",
                isFolder = true,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        var networkBoundResource =
            object : NetworkBoundResource<List<OCShare>, ShareParserResult>(InstantAppExecutors()) {
                override fun saveCallResult(shareParserResult: ShareParserResult) {
                    saved.set(shareParserResult.shares.map { remoteShare ->
                        OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
                    })
                    dbData.value = fetchedDbValue
                }

                override fun loadFromDb(): LiveData<List<OCShare>> {
                    return dbData
                }

                override fun createCall(): RemoteOperationResult<ShareParserResult> {
                    val remoteOperationResult = mock<RemoteOperationResult<ShareParserResult>>()

                    `when`(remoteOperationResult.data).thenReturn(
                        ShareParserResult(networkResult)
                    )

                    `when`(remoteOperationResult.isSuccess).thenReturn(true)

                    return remoteOperationResult
                }
            }

        val observer = mock<Observer<Resource<List<OCShare>>>>()
        networkBoundResource.asLiveData().observeForever(observer)

        assertThat(saved.get(), `is`(networkResult.map { remoteShare ->
            OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
        }))

        verify(observer).onChanged(Resource.success(fetchedDbValue))
    }

    @Test
    fun failureFromNetwork() {
        val saved = AtomicBoolean(false)

        val fetchedDbValue = listOf(
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = true,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        val networkResult: ArrayList<RemoteShare> = arrayListOf()

        var networkBoundResource =
            object : NetworkBoundResource<List<OCShare>, ShareParserResult>(InstantAppExecutors()) {
                override fun saveCallResult(shareParserResult: ShareParserResult) {
                    saved.set(true)
                }

                override fun loadFromDb(): LiveData<List<OCShare>> {
                    dbData.value = fetchedDbValue
                    return dbData
                }

                override fun createCall(): RemoteOperationResult<ShareParserResult> {
                    val remoteOperationResult = mock<RemoteOperationResult<ShareParserResult>>()

                    `when`(remoteOperationResult.data).thenReturn(
                        ShareParserResult(networkResult)
                    )

                    `when`(remoteOperationResult.isSuccess).thenReturn(false)

                    `when`(remoteOperationResult.code).thenReturn(RemoteOperationResult.ResultCode.UNAUTHORIZED)

                    return remoteOperationResult
                }
            }

        val observer = mock<Observer<Resource<List<OCShare>>>>()
        networkBoundResource.asLiveData().observeForever(observer)

        assertThat(saved.get(), `is`(false))

        verify(observer).onChanged(
            Resource.error(
                RemoteOperationResult.ResultCode.UNAUTHORIZED,
                data = dbData.value
            )
        )
    }

    @Test
    fun dbSuccessWithoutNetwork() {
        val saved = AtomicBoolean(false)

        var networkBoundResource =
            object : NetworkBoundResource<List<OCShare>, ShareParserResult>(InstantAppExecutors()) {
                override fun saveCallResult(shareParserResult: ShareParserResult) {
                    saved.set(true)
                }

                override fun loadFromDb(): LiveData<List<OCShare>> {
                    return dbData
                }

                override fun createCall(): RemoteOperationResult<ShareParserResult> {
                    return mock()
                }
            }

        val observer = mock<Observer<Resource<List<OCShare>>>>()
        networkBoundResource.asLiveData().observeForever(observer)

        val dbPublicShares = listOf(
            TestUtil.createPublicShare(
                path = "/Documents/document.jpg",
                isFolder = true,
                name = "Documents 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        dbData.value = dbPublicShares
        verify(observer).onChanged(
            Resource.error(
                data = dbPublicShares
            )
        )
    }
}
