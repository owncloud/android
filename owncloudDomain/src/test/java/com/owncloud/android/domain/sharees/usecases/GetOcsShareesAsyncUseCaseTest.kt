/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.domain.sharees.usecases

import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.sharing.sharees.GetOcsShareesAsyncUseCase
import com.owncloud.android.domain.sharing.sharees.ShareeRepository
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class GetOcsShareesAsyncUseCaseTest {

    private val repository: ShareeRepository = spyk()
    private val useCase = GetOcsShareesAsyncUseCase(repository)
    private val useCaseParams = GetOcsShareesAsyncUseCase.Params("user", 1, 5, OC_ACCOUNT_NAME)

    @Test
    fun `get sharees from server - ok`() {
        every { repository.getSharees(any(), any(), any(), any()) } returns arrayListOf()

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isSuccess)

        verify(exactly = 1) {
            repository.getSharees("user", 1, 5, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `get sharees from server - ko`() {
        every { repository.getSharees(any(), any(), any(), any()) } throws NoConnectionWithServerException()

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is NoConnectionWithServerException)

        verify(exactly = 1) {
            repository.getSharees("user", 1, 5, OC_ACCOUNT_NAME)
        }
    }
}
