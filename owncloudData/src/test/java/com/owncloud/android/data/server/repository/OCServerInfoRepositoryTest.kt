/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

import com.owncloud.android.data.oauth.datasources.RemoteOAuthDataSource
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.data.webfinger.datasources.RemoteWebFingerDataSource
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OCServerInfoRepositoryTest {

    private val remoteServerInfoDataSource = mockk<RemoteServerInfoDataSource>(relaxed = true)
    private val webFingerDataSource = mockk<RemoteWebFingerDataSource>(relaxed = true)
    private val oidcRemoteOAuthDataSource = mockk<RemoteOAuthDataSource>(relaxed = true)
    private val ocServerInfoRepository: OCServerInfoRepository = OCServerInfoRepository(remoteServerInfoDataSource, webFingerDataSource, oidcRemoteOAuthDataSource)

    @Test
    fun getServerInfoSuccess() {
        every { remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl) } returns OC_SECURE_SERVER_INFO_BASIC_AUTH

        val currentValue = ocServerInfoRepository.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false)
        assertEquals(OC_SECURE_SERVER_INFO_BASIC_AUTH, currentValue)

        verify { remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl) }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun getServerInfoNoConnection() {
        every { remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl) } throws NoConnectionWithServerException()

        ocServerInfoRepository.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false)
    }
}
