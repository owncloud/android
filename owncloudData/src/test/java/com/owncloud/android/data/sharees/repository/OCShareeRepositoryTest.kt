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

package com.owncloud.android.data.sharees.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.repository.OCShareeRepository
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class OCShareeRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val remoteShareeDataSource = mockk<RemoteShareeDataSource>(relaxed = true)
    private val oCShareeRepository: OCShareeRepository = OCShareeRepository((remoteShareeDataSource))

    @Test
    fun readShareesFromNetworkOk() {
        every { remoteShareeDataSource.getSharees(any(), any(), any()) } returns arrayListOf()

        oCShareeRepository.getSharees("user", 1, 5)

        verify(exactly = 1) {
            remoteShareeDataSource.getSharees("user", 1, 5)
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun readShareesFromNetworkNoConnection() {
        every { remoteShareeDataSource.getSharees(any(), any(), any()) } throws NoConnectionWithServerException()

        oCShareeRepository.getSharees("user", 1, 5)

        verify(exactly = 1) {
            remoteShareeDataSource.getSharees("user", 1, 5)
        }
    }
}
