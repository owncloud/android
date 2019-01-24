package com.owncloud.android.shares.db

import android.support.test.InstrumentationRegistry
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import junit.framework.Assert.assertEquals
import org.junit.Test
import junit.framework.Assert.assertTrue

class OCShareDaoTest {
    private var ocShareDao: OCShareDao

    init {
        val context = InstrumentationRegistry.getTargetContext()
        OwncloudDatabase.switchToInMemory(context)
        val db: OwncloudDatabase = OwncloudDatabase.getDatabase(context)
        ocShareDao = db.shareDao()
    }

    @Test
    fun check_get_shares_for_file() {
        ocShareDao.insert(
            listOf(
                shareWithNameAndLink(
                    "/Photos/image1.jpg", false, "Image 1 link", "http://server:port/s/1"
                ),
                shareWithNameAndLink(
                    "/Photos/image2.jpg", false, "Image 2 link", "http://server:port/s/2"
                ),
                shareWithNameAndLink(
                    "/Photos/image3.jpg", false, "Image 3 link", "http://server:port/s/3"
                )
            )
        )

        assertEquals(
            ocShareDao.getSharesForFile(
                "/Photos/image3.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            ).size,
            1
        )

        assertEquals(
            ocShareDao.getSharesForFile(
                "/Photos/image3.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            ).get(0).shareLink,
            "http://server:port/s/3"
        )
    }

    @Test
    fun check_insert_shares() {
        ocShareDao.insert(
            listOf(
                shareWithNameAndLink(
                    "/Docs/doc1.docx", false, "Doc link", "http://server:port/s/1"
                ),
                shareWithNameAndLink(
                    "/Images/image1.jpg", false, "Image link", "http://server:port/s/2"
                ),
                shareWithNameAndLink(
                    "/Videos/video.mov", false, "Video link", "http://server:port/s/3"
                ),
                shareWithNameAndLink(
                    "/Projects/December2018", true, "December projects link", "http://server:port/s/4"
                )
            )
        )

        assertEquals(
            ocShareDao.getAllShares().size,
            4
        )

        assertEquals(
            ocShareDao.getAllShares().get(0).path,
            "/Docs/doc1.docx"
        )

        assertEquals(
            ocShareDao.getAllShares().get(1).name,
            "Image link"
        )

        assertEquals(
            ocShareDao.getAllShares().get(2).shareLink,
            "http://server:port/s/3"
        )

        assertTrue(ocShareDao.getAllShares().get(3).isFolder)
    }

    @Test
    fun check_cleared_shares_with_path() {
        ocShareDao.insert(
            listOf(
                shareWithNameAndLink(
                    "/Docs/doc1.jpg", false, "Image 1 link", "http://server:port/s/1"
                ),
                shareWithNameAndLink(
                    "/Photos/image2.jpg", false, "Image 2 link", "http://server:port/s/2"
                ),
                shareWithNameAndLink(
                    "/Photos/image3.jpg", false, "Image 3 link", "http://server:port/s/3"
                )
            )
        )

        // TODO
    }

    // Different share types


    // Different accounts

    fun shareWithNameAndLink(
        path: String,
        isFolder: Boolean,
        name: String,
        shareLink: String
    ): OCShare {
        return OCShare(
            7,
            7,
            3,
            "",
            path,
            1,
            1542628397,
            0,
            "pwdasd12dasdWZ",
            "",
            isFolder,
            -1,
            1,
            "admin@server",
            name,
            shareLink
        )
    }
}
