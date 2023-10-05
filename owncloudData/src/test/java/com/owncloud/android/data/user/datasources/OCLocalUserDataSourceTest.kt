/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
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

package com.owncloud.android.data.user.datasources

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.user.datasources.implementation.OCLocalUserDataSource
import com.owncloud.android.data.user.datasources.implementation.OCLocalUserDataSource.Companion.toEntity
import com.owncloud.android.data.user.datasources.implementation.OCLocalUserDataSource.Companion.toModel
import com.owncloud.android.data.user.db.UserDao
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_USER_QUOTA
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class OCLocalUserDataSourceTest {
    private lateinit var ocLocalUserDataSource: OCLocalUserDataSource
    private val ocUserQuotaDao = mockk<UserDao>(relaxUnitFun = true)

    private val userQuotaEntity = OC_USER_QUOTA.toEntity()

    @Rule
    @JvmField
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        ocLocalUserDataSource = OCLocalUserDataSource(ocUserQuotaDao)
    }

    @Test
    fun saveQuotaForAccount() {

        ocLocalUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA)

        verify(exactly = 1) {
            ocUserQuotaDao.insertOrReplace(userQuotaEntity)
        }
    }

    @Test
    fun `getQuotaForAccount returns a UserQuota`() {
        every { ocUserQuotaDao.getQuotaForAccount(any()) } returns userQuotaEntity

        val userQuota = ocLocalUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME)

        assertEquals(OC_USER_QUOTA, userQuota)

        verify(exactly = 1) {
            ocUserQuotaDao.getQuotaForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getQuotaForAccount return null when dao receive null`() {
        every { ocUserQuotaDao.getQuotaForAccount(any()) } returns null

        val quotaEntity = ocLocalUserDataSource.getQuotaForAccount(OC_ACCOUNT_NAME)

        assertNull(quotaEntity)

        verify(exactly = 1) {
            ocUserQuotaDao.getQuotaForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun deleteQuotaForAccount() {

        ocLocalUserDataSource.deleteQuotaForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            ocUserQuotaDao.deleteQuotaForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getAllUserQuotas returns a UserQuote List`() {

        every { ocUserQuotaDao.getAllUserQuotas() } returns listOf(userQuotaEntity)


        val resultActual = ocLocalUserDataSource.getAllUserQuotas()

        assertEquals(listOf(userQuotaEntity.toModel()), resultActual)

        verify(exactly = 1) {
            ocUserQuotaDao.getAllUserQuotas()
        }
    }
}
