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

package com.owncloud.android.data.user.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserQuotaEntityTest {
    @Test
    fun testConstructor() {
        val item = UserQuotaEntity(
            "accountName",
            200,
            800
        )

        assertEquals("accountName", item.accountName)
        assertEquals(800, item.available)
        assertEquals(200, item.used)
    }

    @Test
    fun testEqualsOk() {
        val item1 = UserQuotaEntity(
            accountName = "accountName",
            available = 200,
            used = 800
        )

        val item2 = UserQuotaEntity(
            "accountName",
            800,
            200
        )

        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsKo() {
        val item1 = UserQuotaEntity(
            accountName = "accountName",
            available = 800,
            used = 200
        )

        val item2 = UserQuotaEntity(
            "accountName2",
            200,
            800
        )

        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }
}
