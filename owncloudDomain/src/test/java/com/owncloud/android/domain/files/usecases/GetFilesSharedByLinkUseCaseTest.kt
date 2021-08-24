/*
 * ownCloud Android client application
 *
 * Created by Fernando Sanz on 2021/8/23
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

import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.testutil.OC_EMPTY_FILES
import com.owncloud.android.testutil.OC_FILES
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class GetFilesSharedByLinkUseCaseTest {

    private val repository: FileRepository = spyk()
    private val useCase = GetFilesSharedByLinkUseCase(repository)
    private val useCaseParams = GetFilesSharedByLinkUseCase.Params(owner = "owner")

    @Test
    fun `get files shared by link - ok`() {
        every { repository.getFilesSharedByLink(useCaseParams.owner) } returns OC_FILES

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(OC_FILES, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getFilesSharedByLink(useCaseParams.owner) }
    }

    @Test
    fun `get files shared by link - ok - empty list`() {
        every { repository.getFilesSharedByLink(useCaseParams.owner) } returns OC_EMPTY_FILES

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(OC_EMPTY_FILES, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getFilesSharedByLink(useCaseParams.owner) }
    }

    @Test
    fun `get files shared by link - ko`() {
        every { repository.getFilesSharedByLink(useCaseParams.owner) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.getFilesSharedByLink(useCaseParams.owner) }
    }
}
