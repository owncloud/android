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

package com.owncloud.android.data.shares.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.repository.OCShareRepository
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.testutil.OC_PRIVATE_SHARE
import com.owncloud.android.testutil.OC_PUBLIC_SHARE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class OCShareRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val localShareDataSource = mockk<LocalShareDataSource>(relaxed = true)
    private val remoteShareDataSource = mockk<RemoteShareDataSource>(relaxed = true)
    private val ocShareRepository: OCShareRepository =
        OCShareRepository(localShareDataSource, remoteShareDataSource)

    private val shares = arrayListOf(
        OC_PUBLIC_SHARE,
        OC_PRIVATE_SHARE,
        OC_PRIVATE_SHARE,
        OC_PUBLIC_SHARE
    )

    private val filePath = "/Images"
    private val accountName = "admin@server"

    @Test
    fun refreshSharesFromNetworkOk() {
        every { remoteShareDataSource.getShares(any(), any(), any(), any()) } returns shares

        ocShareRepository.refreshSharesFromNetwork(filePath, accountName)

        verify(exactly = 1) {
            remoteShareDataSource.getShares(filePath, true, false, accountName)
        }

        verify(exactly = 1) {
            localShareDataSource.replaceShares(shares)
        }
    }

    @Test
    fun refreshSharesFromNetworkEmptyShares() {
        every { remoteShareDataSource.getShares(any(), any(), any(), any()) } returns listOf()

        ocShareRepository.refreshSharesFromNetwork(filePath, accountName)

        verify(exactly = 1) {
            remoteShareDataSource.getShares(filePath, true, false, accountName)
        }

        verify(exactly = 1) {
            localShareDataSource.deleteSharesForFile(filePath, accountName)
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun refreshSharesFromNetworkNoConnection() {
        every { remoteShareDataSource.getShares(any(), any(), any(), any()) } throws NoConnectionWithServerException()

        ocShareRepository.refreshSharesFromNetwork(filePath, accountName)

        verify(exactly = 1) {
            remoteShareDataSource.getShares(filePath, true, false, accountName)
        }

        verify(exactly = 1) {
            localShareDataSource.replaceShares(shares)
        }
    }

    @Test
    fun getSharesAsLiveDataOk() {
        val sharesLiveData = MutableLiveData<List<OCShare>>()

        every {
            localShareDataSource.getSharesAsLiveData(any(), any(), any())
        } returns sharesLiveData

        val sharesEmitted = mutableListOf<List<OCShare>>()
        ocShareRepository.getSharesAsLiveData(filePath, accountName).observeForever {
            sharesEmitted.add(it!!)
        }

        val sharesToEmit = listOf(shares)
        sharesToEmit.forEach {
            sharesLiveData.postValue(it)
        }

        Assert.assertEquals(sharesToEmit, sharesEmitted)
    }

    @Test(expected = Exception::class)
    fun getSharesAsLiveDataException() {
        val sharesLiveData = MutableLiveData<List<OCShare>>()

        every {
            localShareDataSource.getSharesAsLiveData(any(), any(), any())
        } throws Exception()

        val sharesEmitted = mutableListOf<List<OCShare>>()
        ocShareRepository.getSharesAsLiveData(filePath, accountName)

        val sharesToEmit = listOf(shares)
        sharesToEmit.forEach {
            sharesLiveData.postValue(it)
        }

        Assert.assertEquals(sharesToEmit, sharesEmitted)
    }

    @Test
    fun getShareAsLiveDataOk() {
        val shareLiveData = MutableLiveData<OCShare>()

        every {
            localShareDataSource.getShareAsLiveData(any())
        } returns shareLiveData

        val sharesEmitted = mutableListOf<OCShare>()
        ocShareRepository.getShareAsLiveData(1).observeForever {
            sharesEmitted.add(it!!)
        }

        val sharesToEmit = listOf(shares.first())

        sharesToEmit.forEach {
            shareLiveData.postValue(it)
        }

        Assert.assertEquals(sharesToEmit, sharesEmitted)
    }

    @Test(expected = Exception::class)
    fun getShareAsLiveDataException() {
        val shareLiveData = MutableLiveData<OCShare>()

        every {
            localShareDataSource.getShareAsLiveData(any())
        } throws Exception()

        val sharesEmitted = mutableListOf<OCShare>()
        ocShareRepository.getShareAsLiveData(1)

        val sharesToEmit = listOf(shares.first())

        sharesToEmit.forEach {
            shareLiveData.postValue(it)
        }

        Assert.assertEquals(sharesToEmit, sharesEmitted)
    }

    @Test
    fun insertPublicShareOk() {
        every {
            remoteShareDataSource.insertShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns shares.first()

        ocShareRepository.insertPublicShare(
            filePath,
            -1,
            "Docs link",
            "password",
            2000,
            false,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.insertShare(
                filePath,
                ShareType.PUBLIC_LINK,
                "",
                -1,
                "Docs link",
                "password",
                2000,
                false,
                accountName
            )
        }

        verify(exactly = 1) { localShareDataSource.insert(shares.first()) }
    }

    @Test(expected = FileNotFoundException::class)
    fun insertPublicShareFileNotFoundException() {
        every {
            remoteShareDataSource.insertShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws FileNotFoundException()

        ocShareRepository.insertPublicShare(
            filePath,
            -1,
            "Docs link",
            "password",
            2000,
            false,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.insertShare(
                filePath,
                ShareType.PUBLIC_LINK,
                "",
                -1,
                "Docs link",
                "password",
                2000,
                false,
                accountName
            )
        }

        verify(exactly = 0) {
            localShareDataSource.insert(shares.first())
        }
    }

    @Test
    fun updatePublicShareOk() {
        every {
            remoteShareDataSource.updateShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns shares.first()

        ocShareRepository.updatePublicShare(
            1,
            "Docs link",
            "password",
            2000,
            -1,
            false,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                1,
                "Docs link",
                "password",
                2000,
                -1,
                false,
                accountName
            )
        }

        verify(exactly = 1) { localShareDataSource.update(shares.first()) }
    }

    @Test(expected = FileNotFoundException::class)
    fun updatePublicShareFileNotFoundException() {
        every {
            remoteShareDataSource.updateShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws FileNotFoundException()

        ocShareRepository.updatePublicShare(
            1,
            "Docs link",
            "password",
            2000,
            -1,
            false,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                1,
                "Docs link",
                "password",
                2000,
                -1,
                false,
                accountName
            )
        }

        verify(exactly = 0) {
            localShareDataSource.update(shares.first())
        }
    }

    @Test
    fun insertPrivateShareOk() {
        every {
            remoteShareDataSource.insertShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns shares[2]

        ocShareRepository.insertPrivateShare(
            filePath,
            ShareType.GROUP,
            "whoever",
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.insertShare(
                filePath,
                ShareType.GROUP,
                "whoever",
                -1,
                accountName = accountName
            )
        }

        verify(exactly = 1) { localShareDataSource.insert(shares[2]) }
    }

    @Test(expected = FileNotFoundException::class)
    fun insertPrivateShareFileNotFoundException() {
        every {
            remoteShareDataSource.insertShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws FileNotFoundException()

        ocShareRepository.insertPrivateShare(
            filePath,
            ShareType.GROUP,
            "whoever",
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.insertShare(
                filePath,
                ShareType.GROUP,
                "whoever",
                -1,
                accountName = accountName
            )
        }

        verify(exactly = 0) {
            localShareDataSource.insert(shares[2])
        }
    }

    @Test
    fun updatePrivateShareOk() {
        every {
            remoteShareDataSource.updateShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns shares[2]

        ocShareRepository.updatePrivateShare(
            1,
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                1,
                permissions = -1,
                accountName = accountName
            )
        }

        verify(exactly = 1) { localShareDataSource.update(shares[2]) }
    }

    @Test(expected = FileNotFoundException::class)
    fun updatePrivateShareFileNotFoundException() {
        every {
            remoteShareDataSource.updateShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws FileNotFoundException()

        ocShareRepository.updatePrivateShare(
            1,
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                1,
                permissions = -1,
                accountName = accountName
            )
        }

        verify(exactly = 0) { localShareDataSource.update(shares[2]) }
    }

    @Test
    fun removeShare() {
        val shareId = 1L
        ocShareRepository.deleteShare(shareId)

        verify(exactly = 1) { remoteShareDataSource.deleteShare(shareId) }
        verify(exactly = 1) { localShareDataSource.deleteShare(shareId) }
    }

    @Test(expected = FileNotFoundException::class)
    fun removeShareFileNotFoundException() {
        val shareId = 1L

        every {
            remoteShareDataSource.deleteShare(shareId)
        } throws FileNotFoundException()

        ocShareRepository.deleteShare(shareId)

        verify(exactly = 1) { remoteShareDataSource.deleteShare(shareId) }
        verify(exactly = 0) { localShareDataSource.deleteShare(shareId) }
    }
}
