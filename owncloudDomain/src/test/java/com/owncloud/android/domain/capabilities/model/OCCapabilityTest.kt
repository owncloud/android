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
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.FALSE
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.TRUE
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.UNKNOWN
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.Companion.fromValue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OCCapabilityTest {
    @Test
    fun testConstructor() {
        val item = OCCapability(
            123,
            "user@server",
            2,
            1,
            0,
            "1.0.0",
            "1.0.0",
            0,
            TRUE,
            3,
            TRUE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            0,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE
        )

        assertEquals(123, item.id)
        assertEquals("user@server", item.accountName)
        assertEquals(2, item.versionMayor)
        assertEquals(1, item.versionMinor)
        assertEquals(0, item.versionMicro)
        assertEquals("1.0.0", item.versionString)
        assertEquals("1.0.0", item.versionEdition)
        assertEquals(0, item.corePollInterval)
        assertEquals(TRUE, item.filesSharingApiEnabled)
        assertEquals(3, item.filesSharingSearchMinLength)
        assertEquals(TRUE, item.filesSharingPublicEnabled)
        assertEquals(FALSE, item.filesSharingPublicPasswordEnforced)
        assertEquals(FALSE, item.filesSharingPublicPasswordEnforcedReadOnly)
        assertEquals(FALSE, item.filesSharingPublicPasswordEnforcedReadWrite)
        assertEquals(FALSE, item.filesSharingPublicPasswordEnforcedUploadOnly)
        assertEquals(FALSE, item.filesSharingPublicExpireDateEnabled)
        assertEquals(0, item.filesSharingPublicExpireDateDays)
        assertEquals(FALSE, item.filesSharingPublicExpireDateEnforced)
        assertEquals(FALSE, item.filesSharingPublicSendMail)
        assertEquals(FALSE, item.filesSharingPublicUpload)
        assertEquals(FALSE, item.filesSharingPublicMultiple)
        assertEquals(FALSE, item.filesSharingPublicSupportsUploadOnly)
        assertEquals(FALSE, item.filesSharingUserSendMail)
        assertEquals(FALSE, item.filesSharingResharing)
        assertEquals(FALSE, item.filesSharingFederationOutgoing)
        assertEquals(FALSE, item.filesSharingFederationIncoming)
        assertEquals(FALSE, item.filesBigFileChunking)
        assertEquals(FALSE, item.filesUndelete)
        assertEquals(FALSE, item.filesVersioning)
    }

    @Test
    fun testEqualsOk() {
        val item1 = OCCapability(
            id = 123,
            accountName = "user@server",
            versionMayor = 2,
            versionMinor = 1,
            versionMicro = 0,
            versionString = "1.0.0",
            versionEdition = "1.0.0",
            corePollInterval = 0,
            filesSharingApiEnabled = TRUE,
            filesSharingSearchMinLength = 3,
            filesSharingPublicEnabled = TRUE,
            filesSharingPublicPasswordEnforced = FALSE,
            filesSharingPublicPasswordEnforcedReadOnly = FALSE,
            filesSharingPublicPasswordEnforcedReadWrite = FALSE,
            filesSharingPublicPasswordEnforcedUploadOnly = FALSE,
            filesSharingPublicExpireDateEnabled = FALSE,
            filesSharingPublicExpireDateDays = 0,
            filesSharingPublicExpireDateEnforced = FALSE,
            filesSharingPublicSendMail = FALSE,
            filesSharingPublicUpload = FALSE,
            filesSharingPublicMultiple = FALSE,
            filesSharingPublicSupportsUploadOnly = FALSE,
            filesSharingUserSendMail = FALSE,
            filesSharingResharing = FALSE,
            filesSharingFederationOutgoing = FALSE,
            filesSharingFederationIncoming = FALSE,
            filesBigFileChunking = FALSE,
            filesUndelete = FALSE,
            filesVersioning = FALSE
        )

        val item2 = OCCapability(
            123,
            "user@server",
            2,
            1,
            0,
            "1.0.0",
            "1.0.0",
            0,
            TRUE,
            3,
            TRUE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            0,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE
        )

        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsKo() {
        val item1 = OCCapability(
            id = 123,
            accountName = "admin@server",
            versionMayor = 2,
            versionMinor = 1,
            versionMicro = 0,
            versionString = "1.0.0",
            versionEdition = "1.0.0",
            corePollInterval = 0,
            filesSharingApiEnabled = TRUE,
            filesSharingSearchMinLength = 3,
            filesSharingPublicEnabled = TRUE,
            filesSharingPublicPasswordEnforced = FALSE,
            filesSharingPublicPasswordEnforcedReadOnly = FALSE,
            filesSharingPublicPasswordEnforcedReadWrite = FALSE,
            filesSharingPublicPasswordEnforcedUploadOnly = FALSE,
            filesSharingPublicExpireDateEnabled = FALSE,
            filesSharingPublicExpireDateDays = 0,
            filesSharingPublicExpireDateEnforced = FALSE,
            filesSharingPublicSendMail = FALSE,
            filesSharingPublicUpload = FALSE,
            filesSharingPublicMultiple = FALSE,
            filesSharingPublicSupportsUploadOnly = FALSE,
            filesSharingUserSendMail = FALSE,
            filesSharingResharing = FALSE,
            filesSharingFederationOutgoing = FALSE,
            filesSharingFederationIncoming = FALSE,
            filesBigFileChunking = FALSE,
            filesUndelete = FALSE,
            filesVersioning = FALSE
        )

        val item2 = OCCapability(
            123,
            "user@server",
            2,
            1,
            0,
            "1.0.0",
            "1.0.0",
            0,
            TRUE,
            3,
            TRUE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            0,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE
        )

        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }

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
}
