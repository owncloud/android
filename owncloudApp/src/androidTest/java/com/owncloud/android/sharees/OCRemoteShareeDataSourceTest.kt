/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.sharees

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import com.owncloud.android.sharees.data.datasources.OCRemoteShareesDataSource
import com.owncloud.android.utils.TestUtil
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCRemoteShareeDataSourceTest {
    private lateinit var ocRemoteShareesDataSource: OCRemoteShareesDataSource
    private val ownCloudClient = mock(OwnCloudClient::class.java)

    @Before
    fun init() {
        ocRemoteShareesDataSource = OCRemoteShareesDataSource(ownCloudClient)
    }

    @Test
    fun readRemoteSharees() {
        val getRemoteShareesForFileOperation = mock(GetRemoteShareesOperation::class.java)

        val remoteSharees: ArrayList<JSONObject> = arrayListOf(
            createRemoteSharee("User 1", "0", "user1", "user1@mail.com"),
            createRemoteSharee("User 2", "0", "user2", "user2@mail.com"),
            createRemoteSharee("User 3", "0", "user3", "user3@mail.com")
        )

        val getRemoteShareesOperationResult = TestUtil.createRemoteOperationResultMock(
            remoteSharees,
            true
        )

        `when`(getRemoteShareesForFileOperation.execute(ownCloudClient)).thenReturn(
            getRemoteShareesOperationResult
        )

        // Get sharees from remote datasource
        val remoteOperationResult = ocRemoteShareesDataSource.getSharees(
            "user",
            1,
            30,
            getRemoteShareesForFileOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertEquals(3, remoteOperationResult.data.size)

        val sharee1 = remoteOperationResult.data[0]
        assertEquals(sharee1.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "User 1")
        val value = sharee1.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), "0")
        assertEquals(value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user1")
        assertEquals(value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user1@mail.com")

        val sharee2 = remoteOperationResult.data[1]
        assertEquals(sharee2.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "User 2")
        val value2 = sharee2.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value2.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), "0")
        assertEquals(value2.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user2")
        assertEquals(value2.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user2@mail.com")

        val sharee3 = remoteOperationResult.data[2]
        assertEquals(sharee3.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "User 3")
        val value3 = sharee3.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value3.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), "0")
        assertEquals(value3.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "user3")
        assertEquals(value3.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO), "user3@mail.com")
    }

    private fun createRemoteSharee(
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
