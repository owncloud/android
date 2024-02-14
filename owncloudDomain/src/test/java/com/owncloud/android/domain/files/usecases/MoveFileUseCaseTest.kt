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

import com.owncloud.android.domain.exceptions.MoveIntoDescendantException
import com.owncloud.android.domain.exceptions.MoveIntoSameFolderException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveFileUseCaseTest {
    private val repository: FileRepository = spyk()
    private val setLastUsageFileUseCase: SetLastUsageFileUseCase = mockk(relaxed = true)
    private val useCase = MoveFileUseCase(repository, setLastUsageFileUseCase)
    private val useCaseParams = MoveFileUseCase.Params(
        listOfFilesToMove = listOf(OC_FILE.copy(remotePath = "/video.mp4", parentId = 123)),
        targetFolder = OC_FOLDER.copy(id = 100),
        isUserLogged = true,
    )

    @Test
    fun `move file - ok`() {
        every { repository.moveFile(any(), any(), any(), any()) } returns emptyList()

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isSuccess)

        verify(exactly = 1) { repository.moveFile(any(), any(), any(), any()) }
    }

    @Test
    fun `move file - ko - empty list`() {
        val useCaseResult = useCase(useCaseParams.copy(listOfFilesToMove = listOf(), targetFolder = OC_FOLDER))

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is IllegalArgumentException)

        verify(exactly = 0) { repository.moveFile(any(), any(), any(), any()) }
    }

    @Test
    fun `move file - ko - single move into descendant`() {
        val useCaseParams = MoveFileUseCase.Params(
            listOfFilesToMove = listOf(OC_FOLDER.copy(remotePath = "/Directory")),
            targetFolder = OC_FOLDER.copy(remotePath = "/Directory/Descendant/"),
            isUserLogged = true,
        )
        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is MoveIntoDescendantException)

        verify(exactly = 0) { repository.moveFile(any(), any(), any(), any()) }
    }

    @Test
    fun `move file - ko - multiple move into descendant`() {
        val useCaseParams = MoveFileUseCase.Params(
            listOfFilesToMove = listOf(
                OC_FOLDER.copy(remotePath = "/Directory", parentId = 1),
                OC_FILE.copy(remotePath = "/Document.pdf", parentId = 1),
            ),
            targetFolder = OC_FOLDER.copy(remotePath = "/Directory/Descendant/", id = 100),
            isUserLogged = true,
        )
        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)

        verify(exactly = 0) { repository.moveFile(any(), any(), any(), any()) }
    }

    @Test
    fun `move file - ko - single move into same folder`() {
        val useCaseParams = MoveFileUseCase.Params(
            listOfFilesToMove = listOf(element = OC_FOLDER.copy(remotePath = "/Photos/", parentId = 100)),
            targetFolder = OC_FOLDER.copy(remotePath = "/Directory/Descendant/", id = 100),
            isUserLogged = true,
        )
        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is MoveIntoSameFolderException)

        verify(exactly = 0) { repository.moveFile(any(), any(), any(), any()) }
    }

    @Test
    fun `move file - ko - other exception`() {
        every { repository.moveFile(any(), any(), any(), any()) } throws UnauthorizedException()

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.moveFile(any(), any(), any(), any()) }
    }

    @Test
    fun ` move file - ok - return list files`() {
        val filesList = listOf(OC_FILE, OC_FILE)
        every { repository.moveFile(any(), any(), any(), any()) } returns filesList

        val useCaseResult = useCase(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(filesList, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.moveFile(any(), any(), any(), any()) }
    }

    @Test
    fun `mov file - ok - passing replace`() {
        val replace = listOf(true, false)
        every { repository.moveFile(any(), any(), replace, any()) } returns emptyList()

        val useCaseResult = useCase(useCaseParams.copy(replace = replace))

        assertTrue(useCaseResult.isSuccess)

        verify(exactly = 1) { repository.moveFile(any(), any(), replace, any()) }
    }

}
