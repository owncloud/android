/**
 * ownCloud Android client application
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

import org.junit.Assert
import org.junit.Test

class UserAvatarTest {
    @Test
    fun testConstructor() {
        val item = UserAvatar(
            byteArrayOf(1, 2, 3),
            "image/png",
            "edcdc7d39dc218d197c269c8f75ab0f4"
        )

        Assert.assertArrayEquals(byteArrayOf(1, 2, 3), item.avatarData)
        Assert.assertEquals("image/png", item.mimeType)
        Assert.assertEquals("edcdc7d39dc218d197c269c8f75ab0f4", item.eTag)
    }

    @Test
    fun testEqualsOk() {
        val item1 = UserAvatar(
            byteArrayOf(1, 2, 3),
            "image/png",
            "edcdc7d39dc218d197c269c8f75ab0f4"
        )

        val item2 = UserAvatar(
            avatarData = byteArrayOf(1, 2, 3),
            mimeType = "image/png",
            eTag = "edcdc7d39dc218d197c269c8f75ab0f4"
        )

        Assert.assertTrue(item1 == item2)
        Assert.assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsKo() {
        val item1 = UserAvatar(
            byteArrayOf(1, 3, 2),
            "image/png",
            "edcdc7d39dc218d197c269c8f75ab0f4"
        )

        val item2 = UserAvatar(
            avatarData = byteArrayOf(1, 2, 3),
            mimeType = "image/png",
            eTag = "edcdc7d39dc218d197c269c8f75ab0f4"
        )

        Assert.assertFalse(item1 == item2)
        Assert.assertFalse(item1 === item2)
    }
}
