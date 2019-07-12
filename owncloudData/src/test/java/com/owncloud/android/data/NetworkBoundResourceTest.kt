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

package com.owncloud.android.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.data.utils.DataTestUtil
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.utils.InstantExecutors
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class NetworkBoundResourceTest {
    private val dbData = MutableLiveData<List<OCShareEntity>>()

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun basicFromNetwork() {
        val saved = AtomicReference<List<OCShareEntity>>()

        val fetchedDbValue = listOf(
            DataTestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = true,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        val networkResult = arrayListOf(
            DataTestUtil.createRemoteShare(
                shareType = ShareType.PUBLIC_LINK.value,
                path = "/Photos/image.jpg",
                isFolder = true,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        val networkBoundResource =
            object : NetworkBoundResource<List<OCShareEntity>, ShareParserResult>(InstantExecutors()) {
                override fun saveCallResult(item: ShareParserResult) {
                    saved.set(item.shares.map { remoteShare ->
                        OCShareEntity.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
                    })
                    dbData.value = fetchedDbValue
                }

                override fun shouldFetchFromNetwork(data: List<OCShareEntity>?) = true

                override fun loadFromDb(): LiveData<List<OCShareEntity>> = dbData

                override fun createCall(): RemoteOperationResult<ShareParserResult> {
                    val remoteOperationResult = mockk<RemoteOperationResult<ShareParserResult>>(relaxed = true)
                    every { remoteOperationResult.data } returns ShareParserResult(networkResult)
                    every { remoteOperationResult.isSuccess } returns true
                    return remoteOperationResult
                }
            }

        val observer = mockk<Observer<DataResult<List<OCShareEntity>>>>(relaxed = true)
        networkBoundResource.asLiveData().observeForever(observer)

        dbData.postValue(null)

        assertThat(saved.get(), `is`(networkResult.map { remoteShare ->
            OCShareEntity.fromRemoteShare(remoteShare).also { it.accountOwner = "admin@server" }
        }))

        verify { observer.onChanged(DataResult.success(fetchedDbValue)) }
    }

    @Test
    fun failureFromNetwork() {
        val saved = AtomicBoolean(false)

        val fetchedDbValue = listOf(
            DataTestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = true,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            )
        )

        val networkResult: ArrayList<RemoteShare> = arrayListOf()

        val networkBoundResource =
            object : NetworkBoundResource<List<OCShareEntity>, ShareParserResult>(InstantExecutors()) {
                override fun saveCallResult(item: ShareParserResult) {
                    saved.set(true)
                }

                override fun shouldFetchFromNetwork(data: List<OCShareEntity>?) = true

                override fun loadFromDb(): LiveData<List<OCShareEntity>> = dbData.apply { value = fetchedDbValue }

                override fun createCall(): RemoteOperationResult<ShareParserResult> {
                    val remoteOperationResult = mockk<RemoteOperationResult<ShareParserResult>>(relaxed = true)

                    every {
                        remoteOperationResult.isSuccess
                    } returns false

                    every {
                        remoteOperationResult.code
                    } returns RemoteOperationResult.ResultCode.UNAUTHORIZED

                    every {
                        remoteOperationResult.data
                    } returns ShareParserResult(networkResult)

                    every {
                        remoteOperationResult.httpPhrase
                    } returns null

                    every {
                        remoteOperationResult.exception
                    } returns null

                    return remoteOperationResult
                }
            }

        val observer = mockk<Observer<DataResult<List<OCShareEntity>>>>(relaxed = true)
        networkBoundResource.asLiveData().observeForever(observer)

        assertThat(saved.get(), `is`(false))

        verify {
            observer.onChanged(
                DataResult.error(
                    RemoteOperationResult.ResultCode.UNAUTHORIZED,
                    data = dbData.value
                )
            )
        }
    }
}
