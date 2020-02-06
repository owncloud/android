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
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.domain.sharing.shares.usecases.CreatePrivateShareAsyncUseCase
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePrivateShareAsyncUseCaseTest {
    private val shareRepository: ShareRepository = spyk()
    private val useCase = CreatePrivateShareAsyncUseCase((shareRepository))
    private val useCaseParams = CreatePrivateShareAsyncUseCase.Params("", ShareType.USER, "", 1, "")

    @Test
    fun createPrivateShareOk() {
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertFalse(useCaseResult.isError)
        assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) { shareRepository.insertPrivateShare("", ShareType.USER, "", 1, "") }
    }

    @Test
    fun createPrivateShareWithUnauthorizedException() {
        every { shareRepository.insertPrivateShare(any(), any(), any(), any(), any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertFalse(useCaseResult.isSuccess)
        assertTrue(useCaseResult.isError)

        assertNull(useCaseResult.getDataOrNull())
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { shareRepository.insertPrivateShare("", ShareType.USER, "", 1, "") }
    }

    @Test
    fun createPrivateShareWithNotValidShareTypeException() {
        val useCaseParamsNotValid1 = useCaseParams.copy(shareType = null)
        val useCaseResult1 = useCase.execute(useCaseParamsNotValid1)

        assertFalse(useCaseResult1.isSuccess)
        assertTrue(useCaseResult1.isError)

        assertNull(useCaseResult1.getDataOrNull())
        assertTrue(useCaseResult1.getThrowableOrNull() is IllegalArgumentException)

        val useCaseParamsNotValid2 = useCaseParams.copy(shareType = ShareType.CONTACT)
        val useCaseResult2 = useCase.execute(useCaseParamsNotValid2)

        assertFalse(useCaseResult2.isSuccess)
        assertTrue(useCaseResult2.isError)

        assertNull(useCaseResult2.getDataOrNull())
        assertTrue(useCaseResult2.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { shareRepository.insertPrivateShare(any(), any(), any(), any(), any()) }
    }
}
