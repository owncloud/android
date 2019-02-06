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

package com.owncloud.android.shares.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertThat
import org.hamcrest.Matchers.notNullValue
import java.lang.IllegalArgumentException

@RunWith(AndroidJUnit4::class)
@SmallTest
class SharesContentProviderTest {

    private var mContentResolver: ContentResolver? = null

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getTargetContext()
        OwncloudDatabase.switchToInMemory(context)
        mContentResolver = context.contentResolver
    }

    @Test
    fun initiallyEmptySharesQuery() {
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_SHARE,
            null, null, null, null
        )
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(0))
        cursor.close()
    }

    @Test
    fun insertPublicShares() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_SHARE,
            arrayOf(
                shareWithNameAndLink("Picture link", "http://server:port/s/1"),
                shareWithNameAndLink("Picture link 2", "http://server:port/s/2"),
                shareWithNameAndLink("Picture link 3", "http://server:port/s/3")
            )
        )
        assertThat(itemUri, notNullValue())
    }

    @Test
    fun allSharesQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_SHARE,
            arrayOf(
                shareWithNameAndLink("IMG_1213 link", "http://server:port/s/10"),
                shareWithNameAndLink("IMG_1213 link 2", "http://server:port/s/20"),
                shareWithNameAndLink("IMG_1213 link 3", "http://server:port/s/30")
            )
        )
        assertThat(itemUri, notNullValue())

        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_SHARE,
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
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_NAME)
            ),
            Matchers.`is`("IMG_1213 link")
        )

        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_URL)
            ),
            Matchers.`is`("http://server:port/s/10")
        )

        // Last entry
        assertThat(cursor.moveToLast(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_NAME)
            ),
            Matchers.`is`("IMG_1213 link 3")
        )

        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_URL)
            ),
            Matchers.`is`("http://server:port/s/30")
        )

        cursor.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun sharesProjectionQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_SHARE,
            arrayOf(
                shareWithNameAndLink("Video link", "http://server:port/s/100"),
                shareWithNameAndLink("Video 2 link", "http://server:port/s/200"),
                shareWithNameAndLink("Video 3 link", "http://server:port/s/300")
            )
        )
        assertThat(itemUri, notNullValue())

        // Get name of all shares
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_SHARE,
            arrayOf(ProviderTableMeta.OCSHARES_NAME),
            null,
            null,
            null
        )

        // Check all items were properly inserted
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(3))

        // "Name" column requested within projection
        assertThat(cursor.moveToFirst(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_NAME)
            ),
            Matchers.`is`("Video link")
        )

        // "Share link" column not requested within projection
        cursor.getString(cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_URL))
    }

    @Test
    fun sharesSelectionQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_SHARE,
            arrayOf(
                shareWithNameAndLink("Document link", "http://server:port/s/1000"),
                shareWithNameAndLink("Document 2 link", "http://server:port/s/2000"),
                shareWithNameAndLink("Document 3 link", "http://server:port/s/3000"),
                shareWithNameAndLink("Document 4 link", "http://server:port/s/4000")
            )
        )
        assertThat(itemUri, notNullValue())

        // Get shares with name "Document 3 link"
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_SHARE,
            null,
            ProviderTableMeta.OCSHARES_NAME + " = ?",
            arrayOf("Document 3 link"),
            null
        )

        // Check all items were properly inserted
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(1))

        assertThat(cursor.moveToFirst(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_NAME)
            ),
            Matchers.`is`("Document 3 link")
        )

        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_URL)
            ),
            Matchers.`is`("http://server:port/s/3000")
        )
    }

    @Test
    fun sharesProjectionSelectionQuery() {
        val itemUri = mContentResolver!!.bulkInsert(
            ProviderTableMeta.CONTENT_URI_SHARE,
            arrayOf(
                shareWithNameAndLink("Pdf link", "http://server:port/s/1000"),
                shareWithNameAndLink("Pdf friends link", "http://server:port/s/2000"),
                shareWithNameAndLink("Pdf friends link 2", "http://server:port/s/3000"),
                shareWithNameAndLink("Pdf 4 link", "http://server:port/s/4000"),
                shareWithNameAndLink("Pdf 5 link", "http://server:port/s/5000")
            )
        )
        assertThat(itemUri, notNullValue())

        // Get links of shares with "friends" in name
        val cursor = mContentResolver!!.query(
            ProviderTableMeta.CONTENT_URI_SHARE,
            arrayOf(ProviderTableMeta.OCSHARES_URL),
            ProviderTableMeta.OCSHARES_NAME + " LIKE ?",
            arrayOf("%friends link%"),
            null
        )

        // Check all items were properly inserted
        assertThat(cursor, notNullValue())
        assertThat(cursor!!.count, Matchers.`is`(2))

        assertThat(cursor.moveToFirst(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_URL)
            ),
            Matchers.`is`("http://server:port/s/2000")
        )

        assertThat(cursor.moveToNext(), Matchers.`is`(true))
        assertThat(
            cursor.getString(
                cursor.getColumnIndexOrThrow(ProviderTableMeta.OCSHARES_URL)
            ),
            Matchers.`is`("http://server:port/s/3000")
        )
    }

    private fun shareWithNameAndLink(
        name: String,
        url: String
    ): ContentValues {
        val values = ContentValues()
        values.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, 7)
        values.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, 7)
        values.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, 3)
        values.put(ProviderTableMeta.OCSHARES_SHARE_WITH, "")
        values.put(ProviderTableMeta.OCSHARES_PATH, "/Photos/")
        values.put(ProviderTableMeta.OCSHARES_PERMISSIONS, 1)
        values.put(ProviderTableMeta.OCSHARES_SHARED_DATE, 1542628397)
        values.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, 0)
        values.put(ProviderTableMeta.OCSHARES_TOKEN, "pwdasd12dasdWZ")
        values.put(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME, "")
        values.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, 1)
        values.put(ProviderTableMeta.OCSHARES_USER_ID, -1)
        values.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, 1)
        values.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, "admin@server")
        values.put(ProviderTableMeta.OCSHARES_NAME, name)
        values.put(ProviderTableMeta.OCSHARES_URL, url)
        return values
    }
}
