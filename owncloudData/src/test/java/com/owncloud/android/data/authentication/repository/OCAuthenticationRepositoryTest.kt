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
package com.owncloud.android.data.authentication.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_ServerInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class OCAuthenticationRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val remoteAuthenticationDataSource = mockk<RemoteAuthenticationDataSource>(relaxed = true)
    private val ocAuthenticationRepository: OCAuthenticationRepository =
        OCAuthenticationRepository(remoteAuthenticationDataSource)

    @Test
    fun loginOk() {
        every { remoteAuthenticationDataSource.login(any(), any(), any()) } returns Unit

        ocAuthenticationRepository.login(OC_ServerInfo.baseUrl, "username", "password")

        verify(exactly = 1) {
            remoteAuthenticationDataSource.login(OC_ServerInfo.baseUrl, "username", "password")
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun loginException() {
        every {
            remoteAuthenticationDataSource.login(any(), any(), any())
        } throws NoConnectionWithServerException()

        ocAuthenticationRepository.login(OC_ServerInfo.baseUrl, "test", "test")

        verify(exactly = 1) {
            remoteAuthenticationDataSource.login(any(), any(), any())
        }
    }
}
