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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.testutil.OC_ServerInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OCServerInfoRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val remoteServerInfoDataSource = mockk<RemoteServerInfoDataSource>(relaxed = true)
    private val ocServerInfoRepository: OCServerInfoRepository = OCServerInfoRepository(remoteServerInfoDataSource)

    @Test
    fun getServerInfoSecureConnection() {
        every { remoteServerInfoDataSource.getRemoteStatus(OC_ServerInfo.baseUrl) } returns
                Pair(OwnCloudVersion(OC_ServerInfo.ownCloudVersion), true)

        every { remoteServerInfoDataSource.getAuthenticationMethod(OC_ServerInfo.baseUrl) } returns
                AuthenticationMethod.BASIC_HTTP_AUTH

        val serverInfo = ocServerInfoRepository.getServerInfo(OC_ServerInfo.baseUrl)
        assertEquals(OC_ServerInfo.copy(isSecureConnection = true), serverInfo)

        verify { remoteServerInfoDataSource.getRemoteStatus(OC_ServerInfo.baseUrl) }
        verify { remoteServerInfoDataSource.getAuthenticationMethod(OC_ServerInfo.baseUrl) }
    }

    @Test
    fun getServerInfoInSecureConnection() {
        every { remoteServerInfoDataSource.getRemoteStatus(OC_ServerInfo.baseUrl) } returns
                Pair(OwnCloudVersion(OC_ServerInfo.ownCloudVersion), false)

        every { remoteServerInfoDataSource.getAuthenticationMethod(OC_ServerInfo.baseUrl) } returns
                AuthenticationMethod.BASIC_HTTP_AUTH

        val serverInfo = ocServerInfoRepository.getServerInfo(OC_ServerInfo.baseUrl)
        assertEquals(OC_ServerInfo.copy(isSecureConnection = false), serverInfo)

        verify { remoteServerInfoDataSource.getRemoteStatus(OC_ServerInfo.baseUrl) }
        verify { remoteServerInfoDataSource.getAuthenticationMethod(OC_ServerInfo.baseUrl) }
    }

    @Test
    fun getServerInfoBearerAuthMethod() {
        every { remoteServerInfoDataSource.getRemoteStatus(OC_ServerInfo.baseUrl) } returns
                Pair(OwnCloudVersion(OC_ServerInfo.ownCloudVersion), false)

        every { remoteServerInfoDataSource.getAuthenticationMethod(OC_ServerInfo.baseUrl) } returns
                AuthenticationMethod.BEARER_TOKEN

        val serverInfo = ocServerInfoRepository.getServerInfo(OC_ServerInfo.baseUrl)
        assertEquals(OC_ServerInfo.copy(authenticationMethod = AuthenticationMethod.BEARER_TOKEN), serverInfo)

        verify { remoteServerInfoDataSource.getRemoteStatus(OC_ServerInfo.baseUrl) }
        verify { remoteServerInfoDataSource.getAuthenticationMethod(OC_ServerInfo.baseUrl) }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun getServerInfoNoConnection() {
        every {
            remoteServerInfoDataSource.getRemoteStatus(OC_ServerInfo.baseUrl)
        } throws NoConnectionWithServerException()

        ocServerInfoRepository.getServerInfo(OC_ServerInfo.baseUrl)
    }
}
