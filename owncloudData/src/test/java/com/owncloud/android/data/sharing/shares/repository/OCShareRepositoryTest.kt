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

package com.owncloud.android.data.sharing.shares.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_PRIVATE_SHARE
import com.owncloud.android.testutil.OC_PUBLIC_SHARE
import com.owncloud.android.testutil.OC_SHARE
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
            remoteShareDataSource.getShares(
                remoteFilePath = filePath,
                reshares = true,
                subfiles = false,
                accountName = accountName
            )
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
            remoteShareDataSource.getShares(
                remoteFilePath = filePath,
                reshares = true,
                subfiles = false,
                accountName = accountName
            )
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
            remoteShareDataSource.getShares(
                remoteFilePath = filePath,
                reshares = true,
                subfiles = false,
                accountName = accountName
            )
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
        ocShareRepository.getShareAsLiveData(OC_SHARE.remoteId).observeForever {
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
        ocShareRepository.getShareAsLiveData(OC_SHARE.remoteId)

        val sharesToEmit = listOf(shares.first())

        sharesToEmit.forEach {
            shareLiveData.postValue(it)
        }

        Assert.assertEquals(sharesToEmit, sharesEmitted)
    }

    @Test
    fun insertPublicShareOk() {
        every {
            remoteShareDataSource.insert(
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
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.insert(
                filePath,
                ShareType.PUBLIC_LINK,
                "",
                -1,
                "Docs link",
                "password",
                2000,
                accountName
            )
        }

        verify(exactly = 1) { localShareDataSource.insert(shares.first()) }
    }

    @Test(expected = FileNotFoundException::class)
    fun insertPublicShareFileNotFoundException() {
        every {
            remoteShareDataSource.insert(
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
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.insert(
                filePath,
                ShareType.PUBLIC_LINK,
                "",
                -1,
                "Docs link",
                "password",
                2000,
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
                any()
            )
        } returns shares.first()

        ocShareRepository.updatePublicShare(
            OC_SHARE.remoteId,
            "Docs link",
            "password",
            2000,
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                OC_SHARE.remoteId,
                "Docs link",
                "password",
                2000,
                -1,
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
                any()
            )
        } throws FileNotFoundException()

        ocShareRepository.updatePublicShare(
            OC_SHARE.remoteId,
            "Docs link",
            "password",
            2000,
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                OC_SHARE.remoteId,
                "Docs link",
                "password",
                2000,
                -1,
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
            remoteShareDataSource.insert(
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
            remoteShareDataSource.insert(
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
            remoteShareDataSource.insert(
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
            remoteShareDataSource.insert(
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
                any()
            )
        } returns shares[2]

        ocShareRepository.updatePrivateShare(
            OC_SHARE.remoteId,
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                OC_SHARE.remoteId,
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
                any()
            )
        } throws FileNotFoundException()

        ocShareRepository.updatePrivateShare(
            OC_SHARE.remoteId,
            -1,
            accountName
        )

        verify(exactly = 1) {
            remoteShareDataSource.updateShare(
                OC_SHARE.remoteId,
                permissions = -1,
                accountName = accountName
            )
        }

        verify(exactly = 0) { localShareDataSource.update(shares[2]) }
    }

    @Test
    fun removeShare() {
        val shareId = "fjCZxtidwFrzoCl"
        ocShareRepository.deleteShare(shareId, OC_ACCOUNT_NAME)

        verify(exactly = 1) { remoteShareDataSource.deleteShare(shareId, OC_ACCOUNT_NAME) }
        verify(exactly = 1) { localShareDataSource.deleteShare(shareId) }
    }

    @Test(expected = FileNotFoundException::class)
    fun removeShareFileNotFoundException() {
        val shareId = "fjCZxtidwFrzoCl"

        every {
            remoteShareDataSource.deleteShare(shareId, OC_ACCOUNT_NAME)
        } throws FileNotFoundException()

        ocShareRepository.deleteShare(shareId, OC_ACCOUNT_NAME)

        verify(exactly = 1) { remoteShareDataSource.deleteShare(shareId, OC_ACCOUNT_NAME) }
        verify(exactly = 0) { localShareDataSource.deleteShare(shareId) }
    }
}
