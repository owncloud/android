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
package com.owncloud.android.domain.authentication.usecases

import com.owncloud.android.domain.authentication.AuthenticationRepository
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_AUTH_TOKEN_TYPE
import com.owncloud.android.testutil.OC_REFRESH_TOKEN
import com.owncloud.android.testutil.OC_SCOPE
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginOAuthAsyncUseCaseTest {
    private val authRepository: AuthenticationRepository = spyk()
    private val loginOAuthAsyncUseCase = LoginOAuthAsyncUseCase(authRepository)
    private val loginOAuthUseCaseParams = LoginOAuthAsyncUseCase.Params(
        serverInfo = OC_SERVER_INFO,
        username = "test",
        authTokenType = OC_AUTH_TOKEN_TYPE,
        accessToken = OC_ACCESS_TOKEN,
        refreshToken = OC_REFRESH_TOKEN,
        scope = OC_SCOPE,
        updateAccountWithUsername = null,
        clientRegistrationInfo = OC_CLIENT_REGISTRATION
    )

    @Test
    fun invalidParams() {
        var invalidLoginOAuthUseCaseParams = loginOAuthUseCaseParams.copy(serverInfo = null)
        var loginOAuthUseCaseResult = loginOAuthAsyncUseCase.execute(invalidLoginOAuthUseCaseParams)

        assertTrue(loginOAuthUseCaseResult.isError)
        assertTrue(loginOAuthUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidLoginOAuthUseCaseParams = loginOAuthUseCaseParams.copy(authTokenType = "")
        loginOAuthUseCaseResult = loginOAuthAsyncUseCase.execute(invalidLoginOAuthUseCaseParams)

        assertTrue(loginOAuthUseCaseResult.isError)
        assertTrue(loginOAuthUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidLoginOAuthUseCaseParams = loginOAuthUseCaseParams.copy(accessToken = "")
        loginOAuthUseCaseResult = loginOAuthAsyncUseCase.execute(invalidLoginOAuthUseCaseParams)

        assertTrue(loginOAuthUseCaseResult.isError)
        assertTrue(loginOAuthUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidLoginOAuthUseCaseParams = loginOAuthUseCaseParams.copy(refreshToken = "")
        loginOAuthUseCaseResult = loginOAuthAsyncUseCase.execute(invalidLoginOAuthUseCaseParams)

        assertTrue(loginOAuthUseCaseResult.isError)
        assertTrue(loginOAuthUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { authRepository.loginOAuth(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun loginSuccess() {
        every {
            authRepository.loginOAuth(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns OC_ACCOUNT_NAME
        val loginOAuthUseCaseResult = loginOAuthAsyncUseCase.execute(loginOAuthUseCaseParams)

        assertTrue(loginOAuthUseCaseResult.isSuccess)
        assertFalse(loginOAuthUseCaseResult.isError)

        assertNull(loginOAuthUseCaseResult.getThrowableOrNull())
        assertEquals(OC_ACCOUNT_NAME, loginOAuthUseCaseResult.getDataOrNull())

        verify(exactly = 1) { authRepository.loginOAuth(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun getServerInfoWithException() {
        every { authRepository.loginOAuth(any(), any(), any(), any(), any(), any(), any(), any()) } throws Exception()

        val loginOAuthUseCaseResult = loginOAuthAsyncUseCase.execute(loginOAuthUseCaseParams)

        assertFalse(loginOAuthUseCaseResult.isSuccess)
        assertTrue(loginOAuthUseCaseResult.isError)

        assertNull(loginOAuthUseCaseResult.getDataOrNull())
        assertTrue(loginOAuthUseCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { authRepository.loginOAuth(any(), any(), any(), any(), any(), any(), any(), any()) }
    }
}
