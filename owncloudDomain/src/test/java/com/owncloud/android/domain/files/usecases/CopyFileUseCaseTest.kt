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
package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.exceptions.CopyIntoDescendantException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyFileUseCaseTest {
    private val repository: FileRepository = spyk()
    private val useCase = CopyFileUseCase(repository)
    private val useCaseParams = CopyFileUseCase.Params(
        listOfFilesToCopy = listOf(OC_FILE.copy(remotePath = "/video.mp4")),
        targetFolder = OC_FOLDER
    )

    @Test
    fun `copy file - ok`() {
        every { repository.copyFile(any(), any()) } returns Unit

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)

        verify(exactly = 1) { repository.copyFile(any(), any()) }
    }

    @Test
    fun `copy file - ko - empty list`() {
        val useCaseResult = useCase.execute(useCaseParams.copy(listOfFilesToCopy = listOf(), targetFolder = OC_FOLDER))

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { repository.copyFile(any(), any()) }
    }

    @Test
    fun `copy file - ko - single copy into descendant`() {
        val useCaseParams = CopyFileUseCase.Params(
            listOf(OC_FOLDER.copy(remotePath = "/Directory")),
            OC_FOLDER.copy(remotePath = "/Directory/Descendant/")
        )
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is CopyIntoDescendantException)

        verify(exactly = 0) { repository.copyFile(any(), any()) }
    }

    @Test
    fun `copy file - ko - multiple copy into descendant`() {
        val useCaseParams = CopyFileUseCase.Params(
            listOf(OC_FOLDER.copy(remotePath = "/Directory"), OC_FILE.copy(remotePath = "/Document.pdf")),
            OC_FOLDER.copy(remotePath = "/Directory/Descendant/")
        )
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)

        verify(exactly = 1) { repository.copyFile(any(), any()) }
    }

    @Test
    fun `copy file - ko - other exception`() {
        every { repository.copyFile(any(), any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.copyFile(any(), any()) }
    }
}
