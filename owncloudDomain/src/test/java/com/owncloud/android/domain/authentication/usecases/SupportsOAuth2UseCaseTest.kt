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
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportsOAuth2UseCaseTest {
    private val authRepository: AuthenticationRepository = spyk()
    private val supportsOAuth2UseCase = SupportsOAuth2UseCase(authRepository)
    private val supportsOAuth2UseCaseParams = SupportsOAuth2UseCase.Params(
        accountName = OC_ACCOUNT_NAME
    )

    @Test
    fun invalidParams() {
        val invalidSupportsOAuth2UseCaseParams = supportsOAuth2UseCaseParams.copy(accountName = "")
        val supportsOAuth2UseCaseResult = supportsOAuth2UseCase.execute(invalidSupportsOAuth2UseCaseParams)

        assertTrue(supportsOAuth2UseCaseResult.isError)
        assertTrue(supportsOAuth2UseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { authRepository.supportsOAuth2UseCase(any()) }
    }

    @Test
    fun supportsOAuth2Success() {
        every { authRepository.supportsOAuth2UseCase(any()) } returns true
        val supportsOAuth2UseCaseResult = supportsOAuth2UseCase.execute(supportsOAuth2UseCaseParams)

        assertTrue(supportsOAuth2UseCaseResult.isSuccess)
        assertFalse(supportsOAuth2UseCaseResult.isError)

        assertNull(supportsOAuth2UseCaseResult.getThrowableOrNull())
        assertEquals(true, supportsOAuth2UseCaseResult.getDataOrNull())

        verify(exactly = 1) { authRepository.supportsOAuth2UseCase(any()) }
    }

    @Test
    fun supportsOAuth2AccountNotFoundException() {
        every { authRepository.supportsOAuth2UseCase(any()) } throws Exception()

        val supportsOAuth2UseCaseResult = supportsOAuth2UseCase.execute(supportsOAuth2UseCaseParams)

        assertFalse(supportsOAuth2UseCaseResult.isSuccess)
        assertTrue(supportsOAuth2UseCaseResult.isError)

        assertNull(supportsOAuth2UseCaseResult.getDataOrNull())
        assertTrue(supportsOAuth2UseCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { authRepository.supportsOAuth2UseCase(any()) }
    }
}
