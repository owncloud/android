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

package com.owncloud.android.capabilities.provider

import android.content.ContentResolver
import android.content.ContentValues
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertThat
import org.hamcrest.Matchers.notNullValue
import java.lang.IllegalArgumentException

@RunWith(AndroidJUnit4::class)
@SmallTest
class CapabilitiesContentProviderTest {
    private var mContentResolver: ContentResolver? = null

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OwncloudDatabase.switchToInMemory(context)
        mContentResolver = context.contentResolver
    }

    @Test
    fun initiallyEmptyCapabilitiesQuery() {
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            null, null, null, null
        )
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(0))
        cursor.close()
    }

    @Test
    fun insertCapabilities() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            arrayOf(
                capabilityWithAccountNameAndVersion("user1@server", 1),
                capabilityWithAccountNameAndVersion("user2@server", 2),
                capabilityWithAccountNameAndVersion("user3@server", 3)
            )
        )
        assertThat(itemUri, notNullValue())
    }

    @Test
    fun allCapabilitiesQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            arrayOf(
                capabilityWithAccountNameAndVersion("user10@server", 10),
                capabilityWithAccountNameAndVersion("user20@server", 20),
                capabilityWithAccountNameAndVersion("user30@server", 30)
            )
        )
        assertThat(itemUri, notNullValue())

        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            null,
            null,
            null,
            null
        )

        // Check all items were properly inserted
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(3))

        // First entry
        assertThat(cursor.moveToFirst(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)
            ),
            Matchers.`is`("user10@server")
        )

        assertThat(
            cursor.getInt(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)
            ),
            Matchers.`is`(10)
        )

        // Last entry
        assertThat(cursor.moveToLast(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)
            ),
            Matchers.`is`("user30@server")
        )

        assertThat(
            cursor.getInt(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED)
            ),
            Matchers.`is`(CapabilityBooleanType.TRUE.value)
        )

        cursor.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun capabilitiesProjectionQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            arrayOf(
                capabilityWithAccountNameAndVersion("user100@server", 4),
                capabilityWithAccountNameAndVersion("user200@server", 5),
                capabilityWithAccountNameAndVersion("user300@server", 6)
            )
        )
        assertThat(itemUri, notNullValue())

        // Get account name of all capabilities
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            arrayOf(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME),
            null,
            null,
            null
        )

        // Check all items were properly inserted
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(3))

        // "account" column requested within projection
        assertThat(cursor.moveToFirst(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)
            ),
            Matchers.`is`("user100@server")
        )

        // "sharing_public_enabled" column not requested within projection
        cursor.getString(cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED))
    }

    @Test
    fun capabilitiesSelectionQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            arrayOf(
                capabilityWithAccountNameAndVersion("user1@server", 7),
                capabilityWithAccountNameAndVersion("user2@server", 8),
                capabilityWithAccountNameAndVersion("user3@server", 9),
                capabilityWithAccountNameAndVersion("user4@server", 10)
            )
        )
        assertThat(itemUri, notNullValue())

        // Get capabilities with account name "user 3"
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            null,
            ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + " = ?",
            arrayOf("user3@server"),
            null
        )

        // Check all items were properly inserted
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(1))

        assertThat(cursor.moveToFirst(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)
            ),
            Matchers.`is`("user3@server")
        )

        assertThat(
            cursor.getInt(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)
            ),
            Matchers.`is`(9)
        )
    }

    @Test
    fun capabilitiesProjectionSelectionQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            arrayOf(
                capabilityWithAccountNameAndVersion("student1@server", 1),
                capabilityWithAccountNameAndVersion("student2@server", 2),
                capabilityWithAccountNameAndVersion("student21@server", 3),
                capabilityWithAccountNameAndVersion("student4@server", 4),
                capabilityWithAccountNameAndVersion("student5@server", 5)
            )
        )
        assertThat(itemUri, notNullValue())

        // Get version of capabilities with "student2" in account name
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_CAPABILITIES,
            arrayOf(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR),
            ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + " LIKE ?",
            arrayOf("%student2%"),
            null
        )

        // Check all items were properly inserted
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(2))

        assertThat(cursor.moveToFirst(), Matchers.`is`(true))
        assertThat(
            cursor.getInt(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)
            ),
            Matchers.`is`(2)
        )

        assertThat(cursor.moveToNext(), Matchers.`is`(true))
        assertThat(
            cursor.getInt(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)
            ),
            Matchers.`is`(3)
        )
    }

    private fun capabilityWithAccountNameAndVersion(accountName: String, versionMayor: Int): ContentValues {
        val values = ContentValues()
        values.put(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME, accountName)
        values.put(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR, versionMayor)
        values.put(ProviderTableMeta.CAPABILITIES_VERSION_MINOR, 0)
        values.put(ProviderTableMeta.CAPABILITIES_VERSION_MICRO, 0)
        values.put(ProviderTableMeta.CAPABILITIES_VERSION_STRING, "")
        values.put(ProviderTableMeta.CAPABILITIES_VERSION_EDITION, "")
        values.put(ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL, 0)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED, CapabilityBooleanType.FALSE.value)
        values.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY,
            CapabilityBooleanType.FALSE.value
        )
        values.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE,
            CapabilityBooleanType.FALSE.value
        )
        values.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY,
            CapabilityBooleanType.FALSE.value
        )
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED, CapabilityBooleanType.FALSE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS, CapabilityBooleanType.FALSE.value)
        values.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED,
            CapabilityBooleanType.FALSE.value
        )
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_RESHARING, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_FILES_UNDELETE, CapabilityBooleanType.TRUE.value)
        values.put(ProviderTableMeta.CAPABILITIES_FILES_VERSIONING, CapabilityBooleanType.TRUE.value)
        return values
    }
}
