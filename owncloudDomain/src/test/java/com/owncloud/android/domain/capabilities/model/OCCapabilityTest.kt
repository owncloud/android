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

package com.owncloud.android.domain.capabilities.model

import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.Companion.fromBooleanValue
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.Companion.fromValue
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.FALSE
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.TRUE
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.UNKNOWN
import com.owncloud.android.testutil.OC_CAPABILITY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OCCapabilityTest {

    @Test
    fun testCapabilityBooleanType() {
        val fromValueUnknownType = fromValue(-1)
        val fromValueFalseType = fromValue(0)
        val fromValueTrueType = fromValue(1)
        val fromValueDifferentValue = fromValue(2)
        val fromBooleanTrue = fromBooleanValue(true)
        val fromBooleanFalse = fromBooleanValue(false)
        val capabilityUnknown = UNKNOWN
        val capabilityFalse = FALSE
        val capabilityTrue = TRUE

        assertEquals(UNKNOWN, fromValueUnknownType)
        assertEquals(FALSE, fromValueFalseType)
        assertEquals(TRUE, fromValueTrueType)
        assertEquals(UNKNOWN, fromValueDifferentValue)
        assertEquals(TRUE, fromBooleanTrue)
        assertEquals(FALSE, fromBooleanFalse)
        assertEquals(true, capabilityUnknown.isUnknown)
        assertEquals(false, capabilityUnknown.isTrue)
        assertEquals(true, capabilityFalse.isFalse)
        assertEquals(true, capabilityTrue.isTrue)
    }

    @Test
    fun isChunkingAvailable() {
        val item1 = OC_CAPABILITY.copy(davChunkingVersion = "", filesBigFileChunking = TRUE)
        assertEquals(false, item1.isChunkingAllowed())

        val item2 = OC_CAPABILITY.copy(davChunkingVersion = "0", filesBigFileChunking = TRUE)
        assertEquals(false, item2.isChunkingAllowed())

        val item3 = OC_CAPABILITY.copy(davChunkingVersion = "notADouble", filesBigFileChunking = TRUE)
        assertEquals(false, item3.isChunkingAllowed())

        val item4 = OC_CAPABILITY.copy(davChunkingVersion = "1.0", filesBigFileChunking = TRUE)
        assertEquals(true, item4.isChunkingAllowed())

        val item5 = OC_CAPABILITY.copy(davChunkingVersion = "1.0", filesBigFileChunking = FALSE)
        assertEquals(false, item5.isChunkingAllowed())
    }

    @Test
    fun isOpenInWebAllowed() {
        val item1 = OC_CAPABILITY.copy(filesAppProviders = OCCapability.AppProviders(true, "", null, null, "/open-with-web", null))
        assertTrue(item1.isOpenInWebAllowed())

        val item2 = OC_CAPABILITY.copy(filesAppProviders = null)
        assertFalse(item2.isOpenInWebAllowed())
    }
}
