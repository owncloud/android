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

import com.owncloud.android.domain.server.AnonymousServerRepository
import com.owncloud.android.testutil.OC_ServerInfo
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetServerInfoAsyncUseCaseTest {
    private val anonymousServerRepository: AnonymousServerRepository = spyk()
    private val useCase = GetServerInfoAsyncUseCase((anonymousServerRepository))
    private val useCaseParams = GetServerInfoAsyncUseCase.Params(serverPath = "http://demo.owncloud.com")

    @Test
    fun getServerInfoSuccess() {
        every { anonymousServerRepository.getServerInfo(useCaseParams.serverPath) } returns OC_ServerInfo
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertFalse(useCaseResult.isError)

        assertNull(useCaseResult.getThrowableOrNull())
        assertEquals(OC_ServerInfo, useCaseResult.getDataOrNull())

        verify(exactly = 1) { anonymousServerRepository.getServerInfo(useCaseParams.serverPath) }
    }

    @Test
    fun getServerInfoWithException() {
        every { anonymousServerRepository.getServerInfo(useCaseParams.serverPath)  } throws Exception()

        val useCaseResult = useCase.execute(useCaseParams)

        assertFalse(useCaseResult.isSuccess)
        assertTrue(useCaseResult.isError)

        assertNull(useCaseResult.getDataOrNull())
        assertTrue(useCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { anonymousServerRepository.getServerInfo(useCaseParams.serverPath)}
    }
}
