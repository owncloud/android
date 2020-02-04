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
package com.owncloud.android.domain.authentication.usecases

import com.owncloud.android.domain.authentication.AuthenticationRepository
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginAsyncUseCaseTest {
    private val authRepository: AuthenticationRepository = spyk()
    private val useCase = LoginAsyncUseCase(authRepository)
    private val useCaseParams = LoginAsyncUseCase.Params(
        serverPath = "http://demo.owncloud.com",
        username = "test",
        password = "test"
    )

    @Test
    fun invalidParams() {
        var invalidUseCaseParams = useCaseParams.copy(serverPath = "")
        var useCaseResult = useCase.execute(invalidUseCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidUseCaseParams = useCaseParams.copy(username = "")
        useCaseResult = useCase.execute(invalidUseCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidUseCaseParams = useCaseParams.copy(password = "")
        useCaseResult = useCase.execute(invalidUseCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { authRepository.login(any(), any(), any()) }
    }

    @Test
    fun loginSuccess() {
        every { authRepository.login(any(), any(), any()) } returns Unit
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertFalse(useCaseResult.isError)

        assertNull(useCaseResult.getThrowableOrNull())
        assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) { authRepository.login(any(), any(), any()) }
    }

    @Test
    fun getServerInfoWithException() {
        every { authRepository.login(any(), any(), any()) } throws Exception()

        val useCaseResult = useCase.execute(useCaseParams)

        assertFalse(useCaseResult.isSuccess)
        assertTrue(useCaseResult.isError)

        assertNull(useCaseResult.getDataOrNull())
        assertTrue(useCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { authRepository.login(any(), any(), any()) }
    }
}
