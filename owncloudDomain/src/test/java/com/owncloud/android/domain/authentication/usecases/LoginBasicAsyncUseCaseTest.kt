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
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginBasicAsyncUseCaseTest {

    private val repository: AuthenticationRepository = spyk()
    private val useCase = LoginBasicAsyncUseCase(repository)
    private val useCaseParams = LoginBasicAsyncUseCase.Params(
        serverInfo = OC_SERVER_INFO,
        username = "test",
        password = "test",
        updateAccountWithUsername = null
    )

    @Test
    fun `login basic - ko - invalid params`() {
        var invalidLoginBasicUseCaseParams = useCaseParams.copy(serverInfo = null)
        var loginBasicUseCaseResult = useCase.execute(invalidLoginBasicUseCaseParams)

        assertTrue(loginBasicUseCaseResult.isError)
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidLoginBasicUseCaseParams = useCaseParams.copy(username = "")
        loginBasicUseCaseResult = useCase.execute(invalidLoginBasicUseCaseParams)

        assertTrue(loginBasicUseCaseResult.isError)
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        invalidLoginBasicUseCaseParams = useCaseParams.copy(password = "")
        loginBasicUseCaseResult = useCase.execute(invalidLoginBasicUseCaseParams)

        assertTrue(loginBasicUseCaseResult.isError)
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { repository.loginBasic(any(), any(), any(), any()) }
    }

    @Test
    fun `login basic - ok`() {
        every { repository.loginBasic(any(), any(), any(), any()) } returns OC_ACCOUNT_NAME

        val loginBasicUseCaseResult = useCase.execute(useCaseParams)

        assertTrue(loginBasicUseCaseResult.isSuccess)
        assertEquals(OC_ACCOUNT_NAME, loginBasicUseCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.loginBasic(any(), any(), any(), any()) }
    }

    @Test
    fun `login basic - ko - another exception`() {
        every { repository.loginBasic(any(), any(), any(), any()) } throws Exception()

        val loginBasicUseCaseResult = useCase.execute(useCaseParams)

        assertTrue(loginBasicUseCaseResult.isError)
        assertTrue(loginBasicUseCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { repository.loginBasic(any(), any(), any(), any()) }
    }
}
