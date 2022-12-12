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
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportsOAuth2UseCaseTest {

    private val repository: AuthenticationRepository = spyk()
    private val useCase = SupportsOAuth2UseCase(repository)
    private val useCaseParams = SupportsOAuth2UseCase.Params(OC_ACCOUNT_NAME)

    @Test
    fun `supports OAuth2 - ko - invalid params`() {
        val invalidSupportsOAuth2UseCaseParams = useCaseParams.copy(accountName = "")

        val supportsOAuth2UseCaseResult = useCase.execute(invalidSupportsOAuth2UseCaseParams)

        assertTrue(supportsOAuth2UseCaseResult.isError)
        assertTrue(supportsOAuth2UseCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { repository.supportsOAuth2UseCase(any()) }
    }

    @Test
    fun `supports OAuth2 - ok`() {
        every { repository.supportsOAuth2UseCase(any()) } returns true

        val supportsOAuth2UseCaseResult = useCase.execute(useCaseParams)

        assertTrue(supportsOAuth2UseCaseResult.isSuccess)
        assertEquals(true, supportsOAuth2UseCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.supportsOAuth2UseCase(any()) }
    }

    @Test
    fun `supports OAuth2 - ko - another exception`() {
        every { repository.supportsOAuth2UseCase(any()) } throws Exception()

        val supportsOAuth2UseCaseResult = useCase.execute(useCaseParams)

        assertTrue(supportsOAuth2UseCaseResult.isError)
        assertTrue(supportsOAuth2UseCaseResult.getThrowableOrNull() is Exception)

        verify(exactly = 1) { repository.supportsOAuth2UseCase(any()) }
    }
}
