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
package com.owncloud.android.data.user.repository

import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_USER_AVATAR
import com.owncloud.android.testutil.OC_USER_INFO
import com.owncloud.android.testutil.OC_USER_QUOTA
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class OCUserRepositoryTest {

    private val remoteUserDataSource = mockk<RemoteUserDataSource>(relaxed = true)
    private val localUserDataSource = mockk<LocalUserDataSource>(relaxed = true)
    private val ocUserRepository: OCUserRepository = OCUserRepository(localUserDataSource, remoteUserDataSource)

    @Test
    fun `get user info - ok`() {
        every { remoteUserDataSource.getUserInfo(OC_ACCOUNT_NAME) } returns OC_USER_INFO

        ocUserRepository.getUserInfo(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo(OC_ACCOUNT_NAME)
        }
    }

    @Test(expected = Exception::class)
    fun `get user info - ko`() {
        every { remoteUserDataSource.getUserInfo(OC_ACCOUNT_NAME) } throws Exception()

        ocUserRepository.getUserInfo(OC_ACCOUNT_NAME)
    }

    @Test
    fun `get user quota - ok`() {
        every { remoteUserDataSource.getUserQuota(OC_ACCOUNT_NAME) } returns OC_USER_QUOTA

        ocUserRepository.getUserQuota(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteUserDataSource.getUserQuota(OC_ACCOUNT_NAME)
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA)
        }
    }

    @Test(expected = Exception::class)
    fun `get user quota - ko`() {
        every { remoteUserDataSource.getUserQuota(OC_ACCOUNT_NAME) } throws Exception()

        ocUserRepository.getUserQuota(OC_ACCOUNT_NAME)

        verify(exactly = 0) {
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA)
        }
    }

    @Test
    fun `get stored user quota - ok`() {
        every { localUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME) } returns OC_USER_QUOTA

        ocUserRepository.getStoredUserQuota(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            localUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME)
        }
        verify(exactly = 0) {
            remoteUserDataSource.getUserQuota(OC_ACCOUNT_NAME)
        }
    }

    @Test(expected = Exception::class)
    fun `get stored user quota - ko`() {
        every { localUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME) } throws Exception()

        ocUserRepository.getStoredUserQuota(OC_ACCOUNT_NAME)
    }

    @Test
    fun `get user avatar - ok`() {
        every { remoteUserDataSource.getUserAvatar(OC_ACCOUNT_NAME) } returns OC_USER_AVATAR

        ocUserRepository.getUserAvatar(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteUserDataSource.getUserAvatar(OC_ACCOUNT_NAME)
        }
    }

    @Test(expected = Exception::class)
    fun `get user avatar - ko`() {
        every { remoteUserDataSource.getUserAvatar(OC_ACCOUNT_NAME) } throws Exception()

        ocUserRepository.getUserAvatar(OC_ACCOUNT_NAME)
    }
}
