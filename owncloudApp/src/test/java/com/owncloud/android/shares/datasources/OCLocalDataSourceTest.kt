package com.owncloud.android.shares.datasources

import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.db.OCShareDao
import com.owncloud.android.utils.ShareUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCLocalDataSourceTest {
    private lateinit var ocLocalSharesDataSource: OCLocalSharesDataSource
    private val ocSharesDao = mock(OCShareDao::class.java)

    @Before
    fun init() {
        val db = mock(OwncloudDatabase::class.java)
        `when`(db.shareDao()).thenReturn(ocSharesDao)
        `when`(
            ocSharesDao.getSharesForFile(
                "/Photos/image1.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        ).thenReturn(
            listOf(
                ShareUtils.shareWithNameAndLink(
                    "/Photos/image1.jpg", false, "Image 1 link", "http://server:port/s/1"
                )
            )
        )

        ocLocalSharesDataSource = OCLocalSharesDataSource(ocSharesDao)
    }

    @Test
    fun check_shares_correctly_retrieved() {
        val shares = ocLocalSharesDataSource.getSharesForFile(
            "/Photos/image1.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK)
        )

        assertEquals(shares.size, 1)
    }
}
