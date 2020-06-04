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
import com.owncloud.android.domain.exceptions.AccountNotFoundException
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_BASE_URL
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBaseUrlUseCaseTest {
    private val authRepository: AuthenticationRepository = spyk()
    private val getBaseUrlUseCase = GetBaseUrlUseCase(authRepository)
    private val getBaseUrlUseCaseParams = GetBaseUrlUseCase.Params(
        accountName = OC_ACCOUNT_NAME
    )

    @Test
    fun invalidParams() {
        val invalidGetBaseUrlUseCaseParams = getBaseUrlUseCaseParams.copy(accountName = "")
        val getBaseUrlUseCaseResult = getBaseUrlUseCase.execute(invalidGetBaseUrlUseCaseParams)

        assertTrue(getBaseUrlUseCaseResult.isError)
        assertTrue(getBaseUrlUseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { authRepository.getBaseUrl(any()) }
    }

    @Test
    fun getBaseUrlSuccess() {
        every { authRepository.getBaseUrl(any()) } returns OC_BASE_URL
        val getBaseUrlUseCaseResult = getBaseUrlUseCase.execute(getBaseUrlUseCaseParams)

        assertTrue(getBaseUrlUseCaseResult.isSuccess)
        assertFalse(getBaseUrlUseCaseResult.isError)

        assertNull(getBaseUrlUseCaseResult.getThrowableOrNull())
        assertEquals(OC_BASE_URL, getBaseUrlUseCaseResult.getDataOrNull())

        verify(exactly = 1) { authRepository.getBaseUrl(any()) }
    }

    @Test
    fun getBaseUrlAccountNotFoundException() {
        every { authRepository.getBaseUrl(any()) } throws AccountNotFoundException()

        val getBaseUrlUseCaseResult = getBaseUrlUseCase.execute(getBaseUrlUseCaseParams)

        assertFalse(getBaseUrlUseCaseResult.isSuccess)
        assertTrue(getBaseUrlUseCaseResult.isError)

        assertNull(getBaseUrlUseCaseResult.getDataOrNull())
        assertTrue(getBaseUrlUseCaseResult.getThrowableOrNull() is AccountNotFoundException)

        verify(exactly = 1) { authRepository.getBaseUrl(any()) }
    }
}
