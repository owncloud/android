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

package com.owncloud.android.data.user.datasources

import com.owncloud.android.data.user.datasources.implementation.OCRemoteUserDataSource
import com.owncloud.android.data.user.datasources.mapper.RemoteUserInfoMapper
import com.owncloud.android.lib.resources.users.services.implementation.OCUserService
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.users.RemoteUserInfo
import com.owncloud.android.testutil.OC_USER_INFO
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OCRemoteUserDataSourceTest {
    private lateinit var ocRemoteUserDataSource: OCRemoteUserDataSource

    private val ocUserService: OCUserService = mockk()
    private val remoteUserInfoMapper = RemoteUserInfoMapper()

    private val remoteUserInfo = RemoteUserInfo(
        id = OC_USER_INFO.id,
        displayName = OC_USER_INFO.displayName,
        email = OC_USER_INFO.email
    )

    @Before
    fun init() {
        ocRemoteUserDataSource = OCRemoteUserDataSource(ocUserService, remoteUserInfoMapper)
    }

    @Test
    fun getUserInfoOk() {
        val getUserInfoResult: RemoteOperationResult<RemoteUserInfo> =
            createRemoteOperationResultMock(data = remoteUserInfo, isSuccess = true)

        every {
            ocUserService.getUserInfo()
        } returns getUserInfoResult

        val userInfo = ocRemoteUserDataSource.getUserInfo()

        assertNotNull(userInfo)
        assertEquals(OC_USER_INFO, userInfo)
    }

    @Test(expected = Exception::class)
    fun getUserInfoException() {

        every {
            ocUserService.getUserInfo()
        } throws Exception()

        ocRemoteUserDataSource.getUserInfo()
    }
}
