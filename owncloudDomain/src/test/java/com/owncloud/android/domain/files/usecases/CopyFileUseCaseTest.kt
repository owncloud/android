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
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyFileUseCaseTest {
    private val repository: FileRepository = spyk()
    private val useCase = CopyFileUseCase(repository)
    private val useCaseParams = CopyFileUseCase.Params(
        listOfFilesToCopy = listOf(OC_FILE.copy(remotePath = "/video.mp4", parentId = 101)),
        targetFolder = OC_FOLDER.copy(id = 100),
        isUserLogged = true,
    )

    @Test
    fun `copy file - ok`() {
        every { repository.copyFile(any(), any(), any(), any()) } returns emptyList()

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(useCaseResult.getDataOrNull(), emptyList<OCFile>())

        verify(exactly = 1) { repository.copyFile(any(), any(), any(), any()) }
    }

    @Test
    fun `copy file - ok - single copy into same folder`() {
        val useCaseParams = CopyFileUseCase.Params(
            listOfFilesToCopy = listOf(element = OC_FOLDER.copy(remotePath = "/Photos/", parentId = 100)),
            targetFolder = OC_FOLDER.copy(remotePath = "/Directory/Descendant/", id = 100),
            isUserLogged = true,
        )
        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isSuccess)

        verify(exactly = 1) { repository.copyFile(any(), any(), any(), any()) }
    }

    @Test
    fun `copy file - ko - empty list`() {
        val useCaseResult = useCase(useCaseParams.copy(listOfFilesToCopy = listOf(), targetFolder = OC_FOLDER))

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { repository.copyFile(any(), any(), any(), any()) }
    }

    @Test
    fun `copy file - ko - single copy into descendant`() {
        val useCaseParams = CopyFileUseCase.Params(
            listOfFilesToCopy = listOf(OC_FOLDER.copy(remotePath = "/Directory")),
            targetFolder = OC_FOLDER.copy(remotePath = "/Directory/Descendant/"),
            isUserLogged = true
        )
        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is CopyIntoDescendantException)

        verify(exactly = 0) { repository.copyFile(any(), any(), any(), any()) }
    }

    @Test
    fun `copy file - ko - multiple copy into descendant`() {
        val useCaseParams = CopyFileUseCase.Params(
            listOfFilesToCopy = listOf(OC_FOLDER.copy(remotePath = "/Directory"), OC_FILE.copy(remotePath = "/Document.pdf")),
            targetFolder = OC_FOLDER.copy(remotePath = "/Directory/Descendant/"),
            isUserLogged = true
        )
        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)

        verify(exactly = 0) { repository.copyFile(any(), any(), any(), any()) }
    }

    @Test
    fun `copy file - ko - other exception`() {
        every { repository.copyFile(any(), any(), any(), any()) } throws UnauthorizedException()

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.copyFile(any(), any(), any(), any()) }
    }

    @Test
    fun `copy file - ok - return list files`() {
        val filesList = listOf(OC_FILE, OC_FILE)
        every { repository.copyFile(any(), any(), any(), any()) } returns filesList

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(filesList, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.copyFile(any(), any(), any(), any()) }
    }

    @Test
    fun `copy file - ok - passing replace`() {
        val replace = listOf(true, false)
        every { repository.copyFile(any(), any(), replace, any()) } returns emptyList()

        val useCaseResult = useCase(useCaseParams.copy(replace = replace))

        assertTrue(useCaseResult.isSuccess)

        verify(exactly = 1) { repository.copyFile(any(), any(), replace, any()) }
    }

}
