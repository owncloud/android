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

package com.owncloud.android.data.user.datasources.mapper

import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation
import com.owncloud.android.lib.resources.users.RemoteUserInfo
import com.owncloud.android.testutil.OC_USER_INFO
import com.owncloud.android.testutil.OC_USER_QUOTA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteUserQuotaMapperTest {

    private val ocRemoteUserQuotaMapper = RemoteUserQuotaMapper()

    @Test
    fun checkToModelNull() {
        assertNull(ocRemoteUserQuotaMapper.toModel(null))
    }

    @Test
    fun checkToModelNotNull() {
        val remoteUserQuota = GetRemoteUserQuotaOperation.RemoteQuota(
            free = 200_000,
            used = 80_000,
            total = 280_000,
            relative = 28.57
        )
        assertNotNull(remoteUserQuota)

        val userQuota = ocRemoteUserQuotaMapper.toModel(remoteUserQuota)
        assertNotNull(userQuota)
        assertEquals(OC_USER_QUOTA, userQuota)
    }

    @Test
    fun checkToRemoteNull() {
        assertNull(ocRemoteUserQuotaMapper.toRemote(null))
    }
}
