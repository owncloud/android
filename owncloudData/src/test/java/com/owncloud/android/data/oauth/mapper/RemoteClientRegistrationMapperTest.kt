/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.data.oauth.mapper

import com.owncloud.android.data.oauth.OC_REMOTE_CLIENT_REGISTRATION_RESPONSE
import com.owncloud.android.data.oauth.OC_REMOTE_OIDC_DISCOVERY_RESPONSE
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteClientRegistrationMapperTest {

    private val remoteClientRegistrationInfoMapper = RemoteClientRegistrationInfoMapper()

    @Test
    fun `to model - ok - null`() {
        assertNull(remoteClientRegistrationInfoMapper.toModel(null))
    }

    @Test
    fun `to model - ok`() {
        assertNotNull(OC_REMOTE_CLIENT_REGISTRATION_RESPONSE)

        val clientRegistrationInfo = remoteClientRegistrationInfoMapper.toModel(OC_REMOTE_CLIENT_REGISTRATION_RESPONSE)

        assertNotNull(clientRegistrationInfo)
        assertEquals(OC_CLIENT_REGISTRATION, clientRegistrationInfo)
    }

    @Test
    fun `to remote - ok - null`() {
        assertNull(remoteClientRegistrationInfoMapper.toRemote(null))
    }
}
