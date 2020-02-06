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

package com.owncloud.android.data.capabilities.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OCCapabilityEntityTest {
    @Test
    fun testEqualsNamedParams() {
        val item1 = OCCapabilityEntity(
            accountName = "user@server",
            versionMayor = 2,
            versionMinor = 1,
            versionMicro = 0,
            versionString = "1.0.0",
            versionEdition = "1.0.0",
            corePollInterval = 0,
            filesSharingApiEnabled = 0,
            filesSharingSearchMinLength = 2,
            filesSharingPublicEnabled = 1,
            filesSharingPublicPasswordEnforced = 0,
            filesSharingPublicPasswordEnforcedReadOnly = 0,
            filesSharingPublicPasswordEnforcedReadWrite = 0,
            filesSharingPublicPasswordEnforcedUploadOnly = 0,
            filesSharingPublicExpireDateEnabled = 0,
            filesSharingPublicExpireDateDays = 0,
            filesSharingPublicExpireDateEnforced = 0,
            filesSharingPublicSendMail = 0,
            filesSharingPublicUpload = 0,
            filesSharingPublicMultiple = 0,
            filesSharingPublicSupportsUploadOnly = 0,
            filesSharingUserSendMail = 0,
            filesSharingResharing = 0,
            filesSharingFederationOutgoing = 0,
            filesSharingFederationIncoming = 0,
            filesBigFileChunking = 0,
            filesUndelete = 0,
            filesVersioning = 0
        )

        val item2 = OCCapabilityEntity(
            "user@server",
            2,
            1,
            0,
            "1.0.0",
            "1.0.0",
            0,
            0,
            2,
            1,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        )

        // Autogenerate Id should differ but it is not generated at this moment
        // Tested on DAO
        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsNamedParamsNullValues() {
        val item1 = OCCapabilityEntity(
            accountName = null,
            versionMayor = 2,
            versionMinor = 1,
            versionMicro = 0,
            versionString = null,
            versionEdition = null,
            corePollInterval = 0,
            filesSharingApiEnabled = 0,
            filesSharingSearchMinLength = 3,
            filesSharingPublicEnabled = 1,
            filesSharingPublicPasswordEnforced = 0,
            filesSharingPublicPasswordEnforcedReadOnly = 0,
            filesSharingPublicPasswordEnforcedReadWrite = 0,
            filesSharingPublicPasswordEnforcedUploadOnly = 0,
            filesSharingPublicExpireDateEnabled = 0,
            filesSharingPublicExpireDateDays = 0,
            filesSharingPublicExpireDateEnforced = 0,
            filesSharingPublicSendMail = 0,
            filesSharingPublicUpload = 0,
            filesSharingPublicMultiple = 0,
            filesSharingPublicSupportsUploadOnly = 0,
            filesSharingUserSendMail = 0,
            filesSharingResharing = 0,
            filesSharingFederationOutgoing = 0,
            filesSharingFederationIncoming = 0,
            filesBigFileChunking = 0,
            filesUndelete = 0,
            filesVersioning = 0
        )

        val item2 = OCCapabilityEntity(
            null,
            2,
            1,
            0,
            null,
            null,
            0,
            0,
            3,
            1,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        )

        // Autogenerate Id should differ but it is not generated at this moment
        // Tested on DAO
        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testNotEqualsNamedParams() {
        val item1 = OCCapabilityEntity(
            accountName = "user@server",
            versionMayor = 2,
            versionMinor = 1,
            versionMicro = 0,
            versionString = "1.0.0",
            versionEdition = "1.0.0",
            corePollInterval = 0,
            filesSharingApiEnabled = 0,
            filesSharingSearchMinLength = 5,
            filesSharingPublicEnabled = 1,
            filesSharingPublicPasswordEnforced = 0,
            filesSharingPublicPasswordEnforcedReadOnly = 0,
            filesSharingPublicPasswordEnforcedReadWrite = 0,
            filesSharingPublicPasswordEnforcedUploadOnly = 0,
            filesSharingPublicExpireDateEnabled = 0,
            filesSharingPublicExpireDateDays = 0,
            filesSharingPublicExpireDateEnforced = 0,
            filesSharingPublicSendMail = 0,
            filesSharingPublicUpload = 0,
            filesSharingPublicMultiple = 0,
            filesSharingPublicSupportsUploadOnly = 0,
            filesSharingUserSendMail = 0,
            filesSharingResharing = 0,
            filesSharingFederationOutgoing = 0,
            filesSharingFederationIncoming = 0,
            filesBigFileChunking = 0,
            filesUndelete = 0,
            filesVersioning = 0
        )

        val item2 = OCCapabilityEntity(
            "AnyAccountName",
            2,
            1,
            0,
            "1.0.0",
            "1.0.0",
            0,
            0,
            5,
            1,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        )

        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }
}
