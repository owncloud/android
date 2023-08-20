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
package com.owncloud.android.domain.server.usecases

import com.owncloud.android.domain.exceptions.SSLErrorException
import com.owncloud.android.domain.server.ServerInfoRepository
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase.Companion.TRAILING_SLASH
import com.owncloud.android.testutil.OC_INSECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetServerInfoAsyncUseCaseTest {

    private val repository: ServerInfoRepository = spyk()
    private val useCase = GetServerInfoAsyncUseCase((repository))
    private val useCaseParams = GetServerInfoAsyncUseCase.Params(
        serverPath = "http://demo.owncloud.com",
        creatingAccount = false,
        secureConnectionEnforced = false,
    )
    private val useCaseParamsWithSlash = useCaseParams.copy(serverPath = useCaseParams.serverPath.plus(TRAILING_SLASH))

    @Test
    fun `get server info - ok`() {
        every { repository.getServerInfo(useCaseParams.serverPath, false) } returns OC_SECURE_SERVER_INFO_BASIC_AUTH

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(OC_SECURE_SERVER_INFO_BASIC_AUTH, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getServerInfo(useCaseParams.serverPath, false) }
    }

    @Test
    fun `get server info - ok - slash trimmed`() {
        every { repository.getServerInfo(useCaseParams.serverPath, false) } returns OC_SECURE_SERVER_INFO_BASIC_AUTH

        val useCaseResult = useCase(useCaseParamsWithSlash)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(OC_SECURE_SERVER_INFO_BASIC_AUTH, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getServerInfo(useCaseParams.serverPath, false) }
    }

    @Test
    fun `get server info - ko`() {
        every { repository.getServerInfo(useCaseParams.serverPath, false) } throws Exception()

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { repository.getServerInfo(useCaseParams.serverPath, false) }
    }

    @Test
    fun `Should throw SSLErrorException when secureConnectionEnforced is true and ServerInfoRepository returns ServerInfo with isSecureConnection returning false`() {
        every { repository.getServerInfo(useCaseParams.serverPath, false) } returns OC_INSECURE_SERVER_INFO_BASIC_AUTH

        val useCaseResult = useCase(useCaseParams.copy(secureConnectionEnforced = true))

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is SSLErrorException)

        verify(exactly = 1) { repository.getServerInfo(useCaseParams.serverPath, false) }
    }

    @Test
    fun `Should work correctly when secureConnectionEnforced is true and ServerInfoRepository returns ServerInfo with isSecureConnection returning true`() {
        every { repository.getServerInfo(useCaseParams.serverPath, false) } returns OC_SECURE_SERVER_INFO_BASIC_AUTH

        val useCaseResult = useCase(useCaseParams.copy(secureConnectionEnforced = true))

        assertTrue(useCaseResult.isSuccess)
        assertEquals(OC_SECURE_SERVER_INFO_BASIC_AUTH, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getServerInfo(useCaseParams.serverPath, false) }
    }

}
