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
    private val useCaseParamsFile = SaveFileOrFolderUseCase.Params(OC_FILE)

    @Test
    fun `save file or folder - ok`() {
        val useCaseResult = useCase.execute(useCaseParamsFile)
        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertFalse(useCaseResult.isError)
        Assert.assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) {fileRepository.saveFile(useCaseParamsFile.fileToSave)}
    }

    @Test
    fun `save file or folder - ko`() {
        every { fileRepository.saveFile(any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParamsFile)

        Assert.assertFalse(useCaseResult.isSuccess)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { fileRepository.saveFile(useCaseParamsFile.fileToSave) }
    }
}
