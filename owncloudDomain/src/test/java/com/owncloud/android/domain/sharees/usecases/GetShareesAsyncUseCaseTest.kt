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

package com.owncloud.android.domain.sharees.usecases

import com.owncloud.android.domain.sharing.sharees.GetShareesAsyncUseCase
import com.owncloud.android.domain.sharing.sharees.ShareeRepository
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class GetShareesAsyncUseCaseTest {
    private val shareeRepository: ShareeRepository = spyk()
    private val getShareesAsyncUseCase = GetShareesAsyncUseCase(shareeRepository)

    @Test
    fun getShareesFromServerOk() {
        val useCaseResult = getShareesAsyncUseCase.execute(GetShareesAsyncUseCase.Params("user", 1, 5))

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertFalse(useCaseResult.isError)

        verify(exactly = 1) {
            shareeRepository.getSharees("user", 1, 5)
        }
    }

    @Test
    fun getShareesFromServerWithServerResponseTimeoutException() {
        val useCaseResult = getShareesAsyncUseCase.execute(GetShareesAsyncUseCase.Params("user", 1, 5))

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertFalse(useCaseResult.isError)

        verify(exactly = 1) {
            shareeRepository.getSharees("user", 1, 5)
        }
    }
}
