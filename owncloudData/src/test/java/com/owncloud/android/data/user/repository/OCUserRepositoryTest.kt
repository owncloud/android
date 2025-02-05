/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OCUserRepositoryTest {
    private val remoteUserDataSource = mockk<RemoteUserDataSource>()
    private val localUserDataSource = mockk<LocalUserDataSource>(relaxUnitFun = true)
    private val ocUserRepository = OCUserRepository(localUserDataSource, remoteUserDataSource)

    @Test
    fun `getUserInfo returns an UserInfo`() {
        every {
            remoteUserDataSource.getUserInfo(OC_ACCOUNT_NAME)
        } returns OC_USER_INFO

        val userInfo = ocUserRepository.getUserInfo(OC_ACCOUNT_NAME)
        assertEquals(OC_USER_INFO, userInfo)

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getUserQuota returns an UserQuota`() {
        every {
            remoteUserDataSource.getUserQuota(OC_ACCOUNT_NAME)
        } returns OC_USER_QUOTA

        val userQuota = ocUserRepository.getUserQuota(OC_ACCOUNT_NAME)
        assertEquals(OC_USER_QUOTA, userQuota)

        verify(exactly = 1) {
            remoteUserDataSource.getUserQuota(OC_ACCOUNT_NAME)
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA)
        }
    }

    @Test
    fun `getStoredUserQuota returns an UserQuota`() {
        every {
            localUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME)
        } returns OC_USER_QUOTA

        val storedQuota = ocUserRepository.getStoredUserQuota(OC_ACCOUNT_NAME)
        assertEquals(OC_USER_QUOTA, storedQuota)

        verify(exactly = 1) {
            localUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getStoredUserQuota returns null when local datasource returns null`() {
        every {
            localUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME)
        } returns null

        val storedQuota = ocUserRepository.getStoredUserQuota(OC_ACCOUNT_NAME)
        assertNull(storedQuota)

        verify(exactly = 1) {
            localUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getStoredUserQuotaAsFlow returns a Flow with an UserQuota`() = runTest {
        every {
            localUserDataSource.getQuotaForAccountAsFlow(OC_ACCOUNT_NAME)
        } returns flowOf(OC_USER_QUOTA)

        val userQuota = ocUserRepository.getStoredUserQuotaAsFlow(OC_ACCOUNT_NAME).first()
        assertEquals(OC_USER_QUOTA, userQuota)

        verify(exactly = 1) {
            localUserDataSource.getQuotaForAccountAsFlow(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getAllUserQuotas returns a list of UserQuota`() {
        every {
            localUserDataSource.getAllUserQuotas()
        } returns listOf(OC_USER_QUOTA)

        val listOfUserQuotas = ocUserRepository.getAllUserQuotas()
        assertEquals(listOf(OC_USER_QUOTA), listOfUserQuotas)

        verify(exactly = 1) {
            localUserDataSource.getAllUserQuotas()
        }
    }

    @Test
    fun `getAllUserQuotasAsFlow returns a Flow with a list of UserQuota`() = runTest {
        every {
            localUserDataSource.getAllUserQuotasAsFlow()
        } returns flowOf(listOf(OC_USER_QUOTA))

        val listOfUserQuotas = ocUserRepository.getAllUserQuotasAsFlow().first()
        assertEquals(listOf(OC_USER_QUOTA), listOfUserQuotas)

        verify(exactly = 1) {
            localUserDataSource.getAllUserQuotasAsFlow()
        }
    }

    @Test
    fun `getUserAvatar returns an UserAvatar`() {
        every {
            remoteUserDataSource.getUserAvatar(OC_ACCOUNT_NAME)
        } returns OC_USER_AVATAR

        val userAvatar = ocUserRepository.getUserAvatar(OC_ACCOUNT_NAME)
        assertEquals(OC_USER_AVATAR, userAvatar)

        verify(exactly = 1) {
            remoteUserDataSource.getUserAvatar(OC_ACCOUNT_NAME)
        }
    }

}
