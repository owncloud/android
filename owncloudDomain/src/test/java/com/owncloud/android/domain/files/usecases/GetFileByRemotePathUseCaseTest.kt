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

class GetFileByRemotePathUseCaseTest {

    private val repository: FileRepository = spyk()
    private val useCase = GetFileByRemotePathUseCase(repository)
    private val useCaseParams = GetFileByRemotePathUseCase.Params("owner", "remotePath")

    @Test
    fun getFileByRemotePathSuccess() {
        every { repository.getFileByRemotePath(useCaseParams.remotePath, useCaseParams.owner) } returns OC_FILE

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertFalse(useCaseResult.isError)
        Assert.assertEquals(OC_FILE, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getFileByRemotePath(useCaseParams.remotePath, useCaseParams.owner) }
    }

    @Test
    fun getFileByRemotePathException() {
        every { repository.getFileByRemotePath(useCaseParams.remotePath, useCaseParams.owner) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertFalse(useCaseResult.isSuccess)
        Assert.assertTrue(useCaseResult.isError)

        Assert.assertNull(useCaseResult.getDataOrNull())
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.getFileByRemotePath(useCaseParams.remotePath, useCaseParams.owner) }
    }
}
