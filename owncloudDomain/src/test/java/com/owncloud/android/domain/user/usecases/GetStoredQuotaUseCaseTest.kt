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
package com.owncloud.android.domain.user.usecases

import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.user.UserRepository
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_USER_QUOTA
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetStoredQuotaUseCaseTest {

    private val repository: UserRepository = spyk()
    private val useCase = GetStoredQuotaUseCase(repository)
    private val useCaseParams = GetStoredQuotaUseCase.Params(OC_ACCOUNT_NAME)

    @Test
    fun `get stored quota - ok`() {
        every { repository.getStoredUserQuota(OC_ACCOUNT_NAME) } returns OC_USER_QUOTA

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(OC_USER_QUOTA, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getStoredUserQuota(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `get stored quota - ko`() {
        every { repository.getStoredUserQuota(OC_ACCOUNT_NAME) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.getStoredUserQuota(OC_ACCOUNT_NAME) }
    }
}
