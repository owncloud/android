/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.domain.authentication.oauth

import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_REFRESH
import com.owncloud.android.testutil.oauth.OC_TOKEN_RESPONSE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class RequestTokenUseCaseTest {

    private val repository: OAuthRepository = spyk()
    private val useCase = RequestTokenUseCase(repository)
    private val useCaseParams = RequestTokenUseCase.Params(OC_TOKEN_REQUEST_REFRESH)

    @Test
    fun `test request token use case - ok`() {
        every { repository.performTokenRequest(useCaseParams.tokenRequest) } returns OC_TOKEN_RESPONSE

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(OC_TOKEN_RESPONSE, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.performTokenRequest(useCaseParams.tokenRequest) }
    }

    @Test
    fun `test request token use case - ko`() {
        every { repository.performTokenRequest(useCaseParams.tokenRequest) } throws ServerNotReachableException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is ServerNotReachableException)

        verify(exactly = 1) { repository.performTokenRequest(useCaseParams.tokenRequest) }
    }
}