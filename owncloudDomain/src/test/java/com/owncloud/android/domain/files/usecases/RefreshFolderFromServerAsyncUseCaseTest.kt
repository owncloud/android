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
import com.owncloud.android.domain.files.RefreshFolderFromServerAsyncUseCase
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class RefreshFolderFromServerAsyncUseCaseTest {
    private val fileRepository: FileRepository = spyk()
    private val useCase = RefreshFolderFromServerAsyncUseCase(fileRepository)
    private val useCaseParams = RefreshFolderFromServerAsyncUseCase.Params("/Photos")

    @Test
    fun refreshFolderFromServerOk() {
        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertFalse(useCaseResult.isError)
        Assert.assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) { fileRepository.refreshFolder(useCaseParams.remotePath) }
    }

    @Test
    fun refreshCapabilitiesFromServerWithUnauthorizedException() {
        every { fileRepository.refreshFolder(any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertFalse(useCaseResult.isSuccess)
        Assert.assertTrue(useCaseResult.isError)

        Assert.assertNull(useCaseResult.getDataOrNull())
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { fileRepository.refreshFolder(useCaseParams.remotePath) }
    }
}