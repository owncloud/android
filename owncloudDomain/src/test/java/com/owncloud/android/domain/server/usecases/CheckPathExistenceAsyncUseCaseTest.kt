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

import com.owncloud.android.domain.server.ServerRepository
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckPathExistenceAsyncUseCaseTest {
    private val serverRepository: ServerRepository = spyk()
    private val useCase = CheckPathExistenceAsyncUseCase(serverRepository)
    private val useCaseParams = CheckPathExistenceAsyncUseCase.Params(
        remotePath = "http://demo.owncloud.com",
        isUserLogged = false
    )

    @Test
    fun checkPathExistence_exists() {
        every { serverRepository.checkPathExistence(any(), any()) } returns true
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertFalse(useCaseResult.isError)

        assertNull(useCaseResult.getThrowableOrNull())
        assertEquals(true, useCaseResult.getDataOrNull())

        verify(exactly = 1) { serverRepository.checkPathExistence(any(), any()) }
    }

    @Test
    fun checkPathExistence_notExists() {
        every { serverRepository.checkPathExistence(any(), any()) } returns false
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertFalse(useCaseResult.isError)

        assertNull(useCaseResult.getThrowableOrNull())
        assertEquals(false, useCaseResult.getDataOrNull())

        verify(exactly = 1) { serverRepository.checkPathExistence(any(), any()) }
    }

    @Test
    fun getServerInfoWithException() {
        every { serverRepository.checkPathExistence(any(), any()) } throws Exception()

        val useCaseResult = useCase.execute(useCaseParams)

        assertFalse(useCaseResult.isSuccess)
        assertTrue(useCaseResult.isError)

        assertNull(useCaseResult.getDataOrNull())
        assertTrue(useCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { serverRepository.checkPathExistence(any(), any()) }
    }
}
