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
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_SERVER_INFO
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginBasicAsyncUseCaseTest {
    private val authRepository: AuthenticationRepository = spyk()
    private val loginBasicUseCase = LoginBasicAsyncUseCase(authRepository)
    private val loginBasicUseCaseParams = LoginBasicAsyncUseCase.Params(
        serverInfo = OC_SERVER_INFO,
        username = "test",
        password = "test",
        updateAccountWithUsername = null
    )

    @Test
    fun invalidParams() {
        var invalidLoginBasicUseCaseParams = loginBasicUseCaseParams.copy(serverInfo = null)
        var loginBasicUseCaseResult = loginBasicUseCase.execute(invalidLoginBasicUseCaseParams)

        assertTrue(loginBasicUseCaseResult.isError)
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidLoginBasicUseCaseParams = loginBasicUseCaseParams.copy(username = "")
        loginBasicUseCaseResult = loginBasicUseCase.execute(invalidLoginBasicUseCaseParams)

        assertTrue(loginBasicUseCaseResult.isError)
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidLoginBasicUseCaseParams = loginBasicUseCaseParams.copy(password = "")
        loginBasicUseCaseResult = loginBasicUseCase.execute(invalidLoginBasicUseCaseParams)

        assertTrue(loginBasicUseCaseResult.isError)
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { authRepository.loginBasic(any(), any(), any(), any()) }
    }

    @Test
    fun loginSuccess() {
        every { authRepository.loginBasic(any(), any(), any(), any()) } returns OC_ACCOUNT_NAME
        val loginBasicUseCaseResult = loginBasicUseCase.execute(loginBasicUseCaseParams)

        assertTrue(loginBasicUseCaseResult.isSuccess)
        assertFalse(loginBasicUseCaseResult.isError)

        assertNull(loginBasicUseCaseResult.getThrowableOrNull())
        assertEquals(OC_ACCOUNT_NAME, loginBasicUseCaseResult.getDataOrNull())

        verify(exactly = 1) { authRepository.loginBasic(any(), any(), any(), any()) }
    }

    @Test
    fun getServerInfoWithException() {
        every { authRepository.loginBasic(any(), any(), any(), any()) } throws Exception()

        val loginBasicUseCaseResult = loginBasicUseCase.execute(loginBasicUseCaseParams)

        assertFalse(loginBasicUseCaseResult.isSuccess)
        assertTrue(loginBasicUseCaseResult.isError)

        assertNull(loginBasicUseCaseResult.getDataOrNull())
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { authRepository.loginBasic(any(), any(), any(), any()) }
    }
}
