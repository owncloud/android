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

package com.owncloud.android.data.shares.datasources.mapper

import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.testutil.OC_SHARE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OCRemoteShareMapperTest {

    private val ocRemoteShareMapper = RemoteShareMapper()

    @Test
    fun checkToModelNull() {
        assertNull(ocRemoteShareMapper.toModel(null))
    }

    @Test
    fun checkToModelNotNull() {
        val remoteShare = ocRemoteShareMapper.toRemote(OC_SHARE)
        assertNotNull(remoteShare)

        val share = ocRemoteShareMapper.toModel(remoteShare)
        assertNotNull(share)
        assertEquals(OC_SHARE, share)
    }
}
