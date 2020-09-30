/**
 * ownCloud Android client application
 *
 * @author Christian Schabesberger
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
import com.owncloud.android.testutil.OC_FOLDER
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class SaveFileOrFolderUseCaseTest {
    private val fileRepository: FileRepository = spyk()
    private val useCase = SaveFileOrFolderUseCase(fileRepository)
    private val file = OC_FILE
    private val folder = OC_FOLDER
    private val useCaseParamsFile = SaveFileOrFolderUseCase.Params(file)
    private val useCaseParamsFolder = SaveFileOrFolderUseCase.Params(folder)

    private fun testOkWithParams(params: SaveFileOrFolderUseCase.Params) {
        val useCaseResult = useCase.execute(params)
        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertFalse(useCaseResult.isError)
        Assert.assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) {fileRepository.saveFile(params.fileToUpload)}
    }

    @Test
    fun `execute - ok - OC_FILE`() {
        testOkWithParams(useCaseParamsFile)
    }

    @Test
    fun `execute - ok - OC_FOLDER`() {
        testOkWithParams(useCaseParamsFolder)
    }

    @Test
    fun `execute - ko - OC_FILE`() {
        every { fileRepository.saveFile(any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParamsFile)

        Assert.assertFalse(useCaseResult.isSuccess)
        Assert.assertTrue(useCaseResult.isError)

        Assert.assertNull(useCaseResult.getDataOrNull())
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { fileRepository.saveFile(useCaseParamsFile.fileToUpload) }
    }
}
