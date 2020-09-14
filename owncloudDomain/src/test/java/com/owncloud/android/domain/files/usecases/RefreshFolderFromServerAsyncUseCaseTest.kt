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
package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.files.FileRepository
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshFolderFromServerAsyncUseCaseTest {

    private val repository: FileRepository = spyk()
    private val useCase = RefreshFolderFromServerAsyncUseCase(repository)
    private val useCaseParams = RefreshFolderFromServerAsyncUseCase.Params("/Photos")

    @Test
    fun `refresh folder from server - ok`() {
        every { repository.refreshFolder(any()) } returns Unit

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.refreshFolder(useCaseParams.remotePath) }
    }

    @Test
    fun `refresh folder from server - ko`() {
        every { repository.refreshFolder(any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.refreshFolder(useCaseParams.remotePath) }
    }
}
