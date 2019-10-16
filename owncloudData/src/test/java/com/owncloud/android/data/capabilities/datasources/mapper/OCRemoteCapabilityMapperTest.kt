/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.data.capabilities.datasources.mapper

import com.owncloud.android.data.utils.DataTestUtil.DUMMY_CAPABILITY
import com.owncloud.android.data.utils.DataTestUtil.DUMMY_REMOTE_CAPABILITY
import org.junit.Assert
import org.junit.Test

class OCRemoteCapabilityMapperTest {

    private val ocRemoteCapabilityMapper = RemoteCapabilityMapper()

    @Test
    fun checkToModelNull() {
        Assert.assertNull(ocRemoteCapabilityMapper.toModel(null))
    }

    @Test
    fun checkToModelNotNull() {

        Assert.assertNotNull(DUMMY_REMOTE_CAPABILITY)

        val capability = ocRemoteCapabilityMapper.toModel(DUMMY_REMOTE_CAPABILITY)
        Assert.assertNotNull(capability)
        Assert.assertEquals(capability, DUMMY_CAPABILITY)
    }
}
