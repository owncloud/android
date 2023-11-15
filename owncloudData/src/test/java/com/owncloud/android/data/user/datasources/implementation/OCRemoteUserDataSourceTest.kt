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

package com.owncloud.android.data.user.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation
import com.owncloud.android.lib.resources.users.RemoteAvatarData
import com.owncloud.android.lib.resources.users.RemoteUserInfo
import com.owncloud.android.lib.resources.users.services.implementation.OCUserService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_USER_AVATAR
import com.owncloud.android.testutil.OC_USER_INFO
import com.owncloud.android.testutil.OC_USER_QUOTA
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OCRemoteUserDataSourceTest {
    private lateinit var ocRemoteUserDataSource: OCRemoteUserDataSource

    private val clientManager: ClientManager = mockk(relaxed = true)
    private val ocUserService: OCUserService = mockk()

    private val avatarDimension = 128

    private val remoteUserInfo = RemoteUserInfo(
        id = OC_USER_INFO.id,
        displayName = OC_USER_INFO.displayName,
        email = OC_USER_INFO.email
    )
    private val remoteQuota = GetRemoteUserQuotaOperation.RemoteQuota(
        used = OC_USER_QUOTA.used,
        free = OC_USER_QUOTA.available,
        relative = OC_USER_QUOTA.getRelative(),
        total = OC_USER_QUOTA.getTotal()
    )
    private val remoteAvatar = RemoteAvatarData(
        avatarData = OC_USER_AVATAR.avatarData,
        eTag = OC_USER_AVATAR.eTag,
        mimeType = OC_USER_AVATAR.mimeType
    )

    @Before
    fun setUp() {
        every { clientManager.getUserService(any()) } returns ocUserService

        ocRemoteUserDataSource = OCRemoteUserDataSource(
            clientManager,
            avatarDimension
        )
    }

    @Test
    fun `getUserInfo returns UserInfo`() {
        val getUserInfoResult: RemoteOperationResult<RemoteUserInfo> =
            createRemoteOperationResultMock(data = remoteUserInfo, isSuccess = true)

        every {
            ocUserService.getUserInfo()
        } returns getUserInfoResult

        val userInfo = ocRemoteUserDataSource.getUserInfo(OC_ACCOUNT_NAME)

        assertNotNull(userInfo)
        assertEquals(OC_USER_INFO, userInfo)

        verify(exactly = 1) { ocUserService.getUserInfo() }
    }

    @Test
    fun `getUserQuota returns UserQuota`() {
        val getUserQuotaResult: RemoteOperationResult<GetRemoteUserQuotaOperation.RemoteQuota> =
            createRemoteOperationResultMock(data = remoteQuota, isSuccess = true)

        every {
            ocUserService.getUserQuota()
        } returns getUserQuotaResult

        val userQuota = ocRemoteUserDataSource.getUserQuota(OC_ACCOUNT_NAME)

        assertNotNull(userQuota)
        assertEquals(OC_USER_QUOTA, userQuota)

        verify(exactly = 1) { ocUserService.getUserQuota() }
    }

    @Test
    fun `getUserAvatar returns UserAvatar`() {
        val getUserAvatarResult: RemoteOperationResult<RemoteAvatarData> =
            createRemoteOperationResultMock(data = remoteAvatar, isSuccess = true)

        every {
            ocUserService.getUserAvatar(avatarDimension)
        } returns getUserAvatarResult

        val userAvatar = ocRemoteUserDataSource.getUserAvatar(OC_ACCOUNT_NAME)

        assertNotNull(userAvatar)
        assertEquals(OC_USER_AVATAR, userAvatar)

        verify(exactly = 1) { ocUserService.getUserAvatar(avatarDimension) }
    }
}

