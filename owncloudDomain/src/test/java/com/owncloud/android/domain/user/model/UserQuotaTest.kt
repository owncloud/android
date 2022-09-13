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
package com.owncloud.android.domain.user.model

import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserQuotaTest {
    @Test
    fun testConstructor() {
        val item = UserQuota(
            "",
            800,
            200
        )

        assertEquals(800, item.available)
        assertEquals(200, item.used)
    }

    @Test
    fun testEqualsOk() {
        val item1 = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 800,
            used = 200
        )

        val item2 = UserQuota(
            OC_ACCOUNT_NAME,
            800,
            200
        )

        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsKo() {
        val item1 = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 800,
            used = 200
        )

        val item2 = UserQuota(
            OC_ACCOUNT_NAME,
            1000,
            200
        )

        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testGetTotal() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 800_000_000,
            used = 20_000_000
        )

        assertTrue(item.getTotal() == 820_000_000.toLong())
    }

    @Test
    fun testGetTotalFullQuota() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 0,
            used = 20_000_000
        )

        assertTrue(item.getTotal() == 0.toLong())
    }

    @Test
    fun testGetTotalUnlimitedQuota() {
        val item1 = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = -3,
            used = 20_000_000
        )

        assertTrue(item1.getTotal() == 0.toLong())
    }

    @Test
    fun testQuotaLimited() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 200_000,
            used = 20_000
        )

        assertTrue(item.isLimited())
    }

    @Test
    fun testQuotaUnLimited() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = -3,
            used = 20_000
        )

        assertFalse(item.isLimited())
    }

    @Test
    fun testGetRelativeUnlimited() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = -3,
            used = 20_000
        )

        assertEquals(0.0, item.getRelative(), 0.0001)
    }

    @Test
    fun testQuotaRelativeOk() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 80_000,
            used = 20_000
        )

        assertEquals(20.0, item.getRelative(), 0.0001)
    }

    @Test
    fun testQuotaRelativeTotalIs0() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 0,
            used = 0
        )

        assertEquals(0.0, item.getRelative(), 0.0001)
    }

    @Test
    fun testQuotaRelativeTwoDecimals() {
        val item = UserQuota(
            accountName = OC_ACCOUNT_NAME,
            available = 75_000,
            used = 20_000
        )

        assertEquals(21.05, item.getRelative(), 0.0001)
    }
}
