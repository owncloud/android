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
package com.owncloud.android.domain.user.usecases

import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.user.UserRepository
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_USER_AVATAR
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetUserAvatarAsyncUseCaseTest {

    private val repository: UserRepository = spyk()
    private val useCase = GetUserAvatarAsyncUseCase(repository)
    private val useCaseParams = GetUserAvatarAsyncUseCase.Params(OC_ACCOUNT_NAME)

    @Test
    fun `get user avatar - ok`() {
        every { repository.getUserAvatar(OC_ACCOUNT_NAME) } returns OC_USER_AVATAR

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertEquals(OC_USER_AVATAR, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.getUserAvatar(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `get user avatar - ko`() {
        every { repository.getUserAvatar(OC_ACCOUNT_NAME) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isError)
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.getUserAvatar(OC_ACCOUNT_NAME) }
    }
}
