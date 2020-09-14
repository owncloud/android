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

package com.owncloud.android.domain.shares.usecases

import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.sharing.shares.ShareRepository
import com.owncloud.android.domain.sharing.shares.usecases.EditPublicShareAsyncUseCase
import com.owncloud.android.testutil.OC_SHARE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditPublicShareAsyncUseCaseTest {
    private val repository: ShareRepository = spyk()
    private val useCase = EditPublicShareAsyncUseCase(repository)
    private val useCaseParams = EditPublicShareAsyncUseCase.Params(
        OC_SHARE.remoteId,
        "",
        "",
        OC_SHARE.expirationDate,
        OC_SHARE.permissions,
        false,
        OC_SHARE.accountOwner
    )

    @Test
    fun `edit public share - ok`() {
        every {
            repository.updatePublicShare(any(), any(), any(), any(), any(), any(), any())
        } returns Unit

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) {
            repository.updatePublicShare(
                OC_SHARE.remoteId,
                "",
                "",
                OC_SHARE.expirationDate,
                OC_SHARE.permissions,
                false,
                OC_SHARE.accountOwner
            )
        }
    }

    @Test
    fun `edit public share - ko`() {
        every {
            repository.updatePublicShare(any(), any(), any(), any(), any(), any(), any())
        } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) {
            repository.updatePublicShare(
                OC_SHARE.remoteId,
                "",
                "",
                OC_SHARE.expirationDate,
                OC_SHARE.permissions,
                false,
                OC_SHARE.accountOwner
            )
        }
    }
}
