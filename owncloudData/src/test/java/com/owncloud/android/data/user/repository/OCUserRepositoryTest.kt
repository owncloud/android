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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_USER_INFO
import com.owncloud.android.testutil.OC_USER_QUOTA
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class OCUserRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val remoteUserDataSource = mockk<RemoteUserDataSource>(relaxed = true)
    private val localUserDataSource = mockk<LocalUserDataSource>(relaxed = true)
    private val ocUserRepository: OCUserRepository = OCUserRepository(localUserDataSource, remoteUserDataSource)

    @Test
    fun getUserInfo() {
        every { remoteUserDataSource.getUserInfo() } returns OC_USER_INFO

        ocUserRepository.getUserInfo()

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo()
        }
    }

    @Test(expected = Exception::class)
    fun getUserInfoException() {
        every { remoteUserDataSource.getUserInfo() }  throws Exception()

        ocUserRepository.getUserInfo()

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo()
        }
    }

    @Test
    fun getUserQuota() {
        every { remoteUserDataSource.getUserQuota() } returns OC_USER_QUOTA

        ocUserRepository.getUserQuota(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteUserDataSource.getUserQuota()
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA)
        }
    }

    @Test(expected = Exception::class)
    fun getUserQuotaException() {
        every { remoteUserDataSource.getUserQuota() }  throws Exception()

        ocUserRepository.getUserQuota(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteUserDataSource.getUserQuota()
        }
    }
}
