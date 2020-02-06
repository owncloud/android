/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.shares.datasources

import com.owncloud.android.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.owncloud.android.data.sharing.sharees.network.OCShareeService
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

// Needs to be instrumented since JSONObject is tied to the Android platform
class OCRemoteShareesDataSourceTest {
    private lateinit var ocRemoteShareesDataSource: OCRemoteShareeDataSource
    private val ocShareeService: OCShareeService = mockk()

    @Before
    fun init() {
        ocRemoteShareesDataSource =
            OCRemoteShareeDataSource(ocShareeService)
    }

    @Ignore("We need first to address https://github.com/owncloud/android/issues/2704 to properly test this")
    @Test
    fun readRemoteSharees() {
        val remoteSharees: ArrayList<JSONObject> = arrayListOf(
            createSharee("User 1", "0", "user1", "user1@mail.com"),
            createSharee("User 2", "0", "user2", "user2@mail.com"),
            createSharee("User 3", "0", "user3", "user3@mail.com")
        )

        val getRemoteShareesOperationResult = createRemoteOperationResultMock(
            remoteSharees,
            true
        )

        every {
            ocShareeService.getSharees("user", 1, 30)
        } returns getRemoteShareesOperationResult

        // Get sharees from remote datasource
        val sharees = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30
        )

        assertNotNull(sharees)
        assertEquals(3, sharees.size)

        val sharee1 = sharees.first()
        assertEquals(sharee1.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "User 1")
        val value = sharee1.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), "0")
        assertEquals(value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user1")
        assertEquals(value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user1@mail.com")

        val sharee2 = sharees[1]
        assertEquals(sharee2.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "User 2")
        val value2 = sharee2.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value2.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), "0")
        assertEquals(value2.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user2")
        assertEquals(value2.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user2@mail.com")

        val sharee3 = sharees[2]
        assertEquals(sharee3.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "User 3")
        val value3 = sharee3.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value3.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), "0")
        assertEquals(value3.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user3")
        assertEquals(value3.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user3@mail.com")
    }

    private fun createSharee(
        label: String,
        shareType: String,
        shareWith: String,
        shareWithAdditionalInfo: String
    ): JSONObject {
        val jsonObject = JSONObject()

        jsonObject.put(GetRemoteShareesOperation.PROPERTY_LABEL, label)

        val value = JSONObject()
        value.put(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE, shareType)
        value.put(GetRemoteShareesOperation.PROPERTY_SHARE_WITH, shareWith)
        value.put(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO, shareWithAdditionalInfo)

        jsonObject.put(GetRemoteShareesOperation.NODE_VALUE, value)

        return jsonObject
    }
}
