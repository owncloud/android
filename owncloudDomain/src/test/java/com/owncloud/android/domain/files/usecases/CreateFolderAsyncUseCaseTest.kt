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
import com.owncloud.android.testutil.OC_FILE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class CreateFolderAsyncUseCaseTest {

    private val repository: FileRepository = spyk()
    private val useCase = CreateFolderAsyncUseCase(repository)
    private val useCaseParams = CreateFolderAsyncUseCase.Params("new folder", OC_FILE)

    @Test
    fun `create folder - ok`() {
        every { repository.createFolder(any(), any()) } returns Unit

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.createFolder(any(), useCaseParams.parentFile) }
    }

    @Test
    fun `create folder - ko - forbidden chars`() {
        // Folder name is blank
        var useCaseResult = useCase.execute(useCaseParams.copy(folderName = "   "))

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)

        // Parent folder is not a folder :S
        useCaseResult = useCase.execute(useCaseParams.copy(parentFile = OC_FILE.copy(mimeType = "text/plain")))

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)
    }

    @Test
    fun `create folder - ko - other exception`() {
        every { repository.createFolder(any(), any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.createFolder(any(), useCaseParams.parentFile) }
    }
}
