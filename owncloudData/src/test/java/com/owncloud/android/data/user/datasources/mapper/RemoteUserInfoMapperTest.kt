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

import com.owncloud.android.lib.resources.users.RemoteUserInfo
import com.owncloud.android.testutil.OC_USER_INFO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteUserInfoMapperTest {

    private val ocRemoteUserInfoMapper = RemoteUserInfoMapper()

    @Test
    fun checkToModelNull() {
        assertNull(ocRemoteUserInfoMapper.toModel(null))
    }

    @Test
    fun checkToModelNotNull() {
        val remoteUserInfo = RemoteUserInfo(
            id = OC_USER_INFO.id,
            displayName = OC_USER_INFO.displayName,
            email = OC_USER_INFO.email
        )
        assertNotNull(remoteUserInfo)

        val userInfo = ocRemoteUserInfoMapper.toModel(remoteUserInfo)
        assertNotNull(userInfo)
        assertEquals(OC_USER_INFO, userInfo)
    }

    @Test
    fun checkToRemoteNull() {
        assertNull(ocRemoteUserInfoMapper.toRemote(null))
    }
}
