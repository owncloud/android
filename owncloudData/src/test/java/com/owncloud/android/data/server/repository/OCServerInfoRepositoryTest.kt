/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.data.server.repository

import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_SERVER_INFO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OCServerInfoRepositoryTest {

    private val remoteServerInfoDataSource = mockk<RemoteServerInfoDataSource>(relaxed = true)
    private val ocServerInfoRepository: OCServerInfoRepository = OCServerInfoRepository(remoteServerInfoDataSource)

    @Test
    fun getServerInfoSuccess() {
        every { remoteServerInfoDataSource.getServerInfo(OC_SERVER_INFO.baseUrl) } returns OC_SERVER_INFO

        val currentValue = ocServerInfoRepository.getServerInfo(OC_SERVER_INFO.baseUrl)
        assertEquals(OC_SERVER_INFO, currentValue)

        verify { remoteServerInfoDataSource.getServerInfo(OC_SERVER_INFO.baseUrl) }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun getServerInfoNoConnection() {
        every { remoteServerInfoDataSource.getServerInfo(OC_SERVER_INFO.baseUrl) } throws NoConnectionWithServerException()

        ocServerInfoRepository.getServerInfo(OC_SERVER_INFO.baseUrl)
    }
}
