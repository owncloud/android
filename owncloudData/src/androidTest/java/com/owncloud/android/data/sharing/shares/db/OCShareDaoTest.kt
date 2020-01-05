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

package com.owncloud.android.data.sharing.shares.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.sharing.shares.datasources.mapper.OCShareMapper
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.testutil.OC_PRIVATE_SHARE
import com.owncloud.android.testutil.OC_PUBLIC_SHARE
import com.owncloud.android.testutil.livedata.getLastEmittedValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SmallTest
class OCShareDaoTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var ocShareDao: OCShareDao
    private val ocShareMapper = OCShareMapper()

    private val privateShareTypeValues = listOf(
        ShareType.USER.value, ShareType.GROUP.value, ShareType.FEDERATED.value
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OwncloudDatabase.switchToInMemory(context)
        val db: OwncloudDatabase = OwncloudDatabase.getDatabase(context)
        ocShareDao = db.shareDao()
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun insertEmptySharesList() {
        ocShareDao.insert(listOf())

        val shares = ocShareDao.getSharesAsLiveData(
            "/Test/",
            "admin@server",
            mutableListOf(ShareType.PUBLIC_LINK.value).plus(privateShareTypeValues)
        ).getLastEmittedValue()!!

        assertNotNull(shares)
        assertEquals(0, shares.size)
    }

    @Test
    fun insertSharesFromDifferentFilesAndRead() {
        ocShareDao.insert(
            listOf(
                ocShareMapper.toEntity(
                    OC_PUBLIC_SHARE.copy(
                        path = "/Photos/",
                        isFolder = true,
                        name = "Photos folder link",
                        shareLink = "http://server:port/s/1",
                        accountOwner = "admin@server"
                    )
                )!!,
                ocShareMapper.toEntity(
                    OC_PUBLIC_SHARE.copy(
                        path = "/Photos/image1.jpg",
                        isFolder = false,
                        name = "Image 1 link",
                        shareLink = "http://server:port/s/2",
                        accountOwner = "admin@server"
                    )
                )!!,
                ocShareMapper.toEntity(
                    OC_PRIVATE_SHARE.copy(
                        path = "/Photos/image2.jpg",
                        isFolder = false,
                        shareWith = "username",
                        sharedWithDisplayName = "John",
                        accountOwner = "admin@server"
                    )
                )!!
            )
        )

        val photosFolderPublicShares = ocShareDao.getSharesAsLiveData(
            "/Photos/",
            "admin@server",
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(photosFolderPublicShares)
        assertEquals(1, photosFolderPublicShares.size)
        assertEquals("/Photos/", photosFolderPublicShares[0].path)
        assertEquals(true, photosFolderPublicShares[0].isFolder)
        assertEquals("admin@server", photosFolderPublicShares[0].accountOwner)
        assertEquals("Photos folder link", photosFolderPublicShares[0].name)
        assertEquals("http://server:port/s/1", photosFolderPublicShares[0].shareLink)

        val image1PublicShares = ocShareDao.getSharesAsLiveData(
            "/Photos/image1.jpg",
            "admin@server",
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(image1PublicShares)
        assertEquals(1, image1PublicShares.size)
        assertEquals("/Photos/image1.jpg", image1PublicShares[0].path)
        assertEquals(false, image1PublicShares[0].isFolder)
        assertEquals("admin@server", image1PublicShares[0].accountOwner)
        assertEquals("Image 1 link", image1PublicShares[0].name)
        assertEquals("http://server:port/s/2", image1PublicShares[0].shareLink)

        val image2PrivateShares =
            ocShareDao.getSharesAsLiveData(
                "/Photos/image2.jpg", "admin@server", privateShareTypeValues
            ).getLastEmittedValue()!!
        assertNotNull(image2PrivateShares)
        assertEquals(1, image2PrivateShares.size)
        assertEquals("/Photos/image2.jpg", image2PrivateShares[0].path)
        assertEquals(false, image2PrivateShares[0].isFolder)
        assertEquals("admin@server", image1PublicShares[0].accountOwner)
        assertEquals("username", image2PrivateShares[0].shareWith)
        assertEquals("John", image2PrivateShares[0].sharedWithDisplayName)
    }

    @Test
    fun insertSharesFromDifferentAccountsAndRead() {
        ocShareDao.insert(
            listOf(
                ocShareMapper.toEntity(
                    OC_PUBLIC_SHARE.copy(
                        path = "/Documents/document1.docx",
                        isFolder = false,
                        accountOwner = "user1@server",
                        name = "Document 1 link",
                        shareLink = "http://server:port/s/1"
                    )
                )!!,
                ocShareMapper.toEntity(
                    OC_PUBLIC_SHARE.copy(
                        path = "/Documents/document1.docx",
                        isFolder = false,
                        accountOwner = "user2@server",
                        name = "Document 1 link",
                        shareLink = "http://server:port/s/2"
                    )
                )!!,
                ocShareMapper.toEntity(
                    OC_PRIVATE_SHARE.copy(
                        path = "/Documents/document1.docx",
                        isFolder = false,
                        accountOwner = "user3@server",
                        shareWith = "user_name",
                        sharedWithDisplayName = "Patrick"
                    )
                )!!
            )
        )

        val document1PublicSharesForUser1 = ocShareDao.getSharesAsLiveData(
            "/Documents/document1.docx",
            "user1@server",
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(document1PublicSharesForUser1)
        assertEquals(1, document1PublicSharesForUser1.size)
        assertEquals("/Documents/document1.docx", document1PublicSharesForUser1[0].path)
        assertEquals(false, document1PublicSharesForUser1[0].isFolder)
        assertEquals("user1@server", document1PublicSharesForUser1[0].accountOwner)
        assertEquals("Document 1 link", document1PublicSharesForUser1[0].name)
        assertEquals("http://server:port/s/1", document1PublicSharesForUser1[0].shareLink)

        val document1PublicSharesForUser2 = ocShareDao.getSharesAsLiveData(
            "/Documents/document1.docx",
            "user2@server",
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(document1PublicSharesForUser2)
        assertEquals(1, document1PublicSharesForUser2.size)
        assertEquals("/Documents/document1.docx", document1PublicSharesForUser2[0].path)
        assertEquals(false, document1PublicSharesForUser2[0].isFolder)
        assertEquals("user2@server", document1PublicSharesForUser2[0].accountOwner)
        assertEquals("Document 1 link", document1PublicSharesForUser2[0].name)
        assertEquals("http://server:port/s/2", document1PublicSharesForUser2[0].shareLink)

        val document1PrivateSharesForUser3 = ocShareDao.getSharesAsLiveData(
            "/Documents/document1.docx",
            "user3@server",
            privateShareTypeValues
        ).getLastEmittedValue()!!

        assertNotNull(document1PrivateSharesForUser3)
        assertEquals(1, document1PrivateSharesForUser3.size)
        assertEquals("/Documents/document1.docx", document1PrivateSharesForUser3[0].path)
        assertEquals(false, document1PrivateSharesForUser3[0].isFolder)
        assertEquals("user3@server", document1PrivateSharesForUser3[0].accountOwner)
        assertEquals("user_name", document1PrivateSharesForUser3[0].shareWith)
        assertEquals("Patrick", document1PrivateSharesForUser3[0].sharedWithDisplayName)
    }

    @Test
    fun testAutogenerateId() {
        ocShareDao.insert(
            listOf(
                ocShareMapper.toEntity(
                    OC_PUBLIC_SHARE.copy(
                        path = "/Documents/document1.docx",
                        accountOwner = "user1@server",
                        name = "Document 1 link",
                        shareLink = "http://server:port/s/1"
                    )
                )!!,
                ocShareMapper.toEntity(
                    OC_PUBLIC_SHARE.copy(
                        path = "/Documents/document1.docx",
                        accountOwner = "user1@server",
                        name = "Document 1 link",
                        shareLink = "http://server:port/s/1"
                    )
                )!!
            )
        )

        val sharesWithSameValues = ocShareDao.getSharesAsLiveData(
            "/Documents/document1.docx",
            "user1@server",
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(sharesWithSameValues)
        assertEquals(2, sharesWithSameValues.size)
        assertEquals("/Documents/document1.docx", sharesWithSameValues[0].path)
        assertEquals("/Documents/document1.docx", sharesWithSameValues[1].path)
        assertEquals(false, sharesWithSameValues[0].isFolder)
        assertEquals(false, sharesWithSameValues[1].isFolder)
        assertEquals("user1@server", sharesWithSameValues[0].accountOwner)
        assertEquals("user1@server", sharesWithSameValues[1].accountOwner)
        assertEquals("Document 1 link", sharesWithSameValues[0].name)
        assertEquals("Document 1 link", sharesWithSameValues[1].name)
        assertEquals("http://server:port/s/1", sharesWithSameValues[0].shareLink)
        assertEquals("http://server:port/s/1", sharesWithSameValues[1].shareLink)
        assertNotNull(sharesWithSameValues[0].id)
        assertNotNull(sharesWithSameValues[1].id)
        assert(sharesWithSameValues[0].id != sharesWithSameValues[1].id)
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun getNonExistingPrivateShare() {
        val privateShare = createDefaultPrivateShareEntity()

        ocShareDao.insert(privateShare)

        val nonExistingPrivateShare = ocShareDao.getSharesAsLiveData(
            privateShare.path,
            "user@server",
            privateShareTypeValues
        ).getLastEmittedValue()!!

        assertNotNull(nonExistingPrivateShare)
        assertEquals(0, nonExistingPrivateShare.size)
    }

    @Test
    fun replacePrivateShareIfAlreadyExists_exists() {
        val privateShare = createDefaultPrivateShareEntity()

        ocShareDao.insert(createDefaultPrivateShareEntity())

        val privateShareToReplace = createDefaultPrivateShareEntity(shareWith = "userName")

        ocShareDao.replaceShares(
            listOf(privateShareToReplace)
        )

        val textShares = ocShareDao.getSharesAsLiveData(
            privateShare.path,
            privateShare.accountOwner,
            listOf(ShareType.USER.value)
        ).getLastEmittedValue()!!

        assertNotNull(textShares)
        assertEquals(1, textShares.size)
        assertEquals(privateShareToReplace.shareWith, textShares[0].shareWith)
    }

    @Test
    fun replacePrivateShareIfAlreadyExists_doesNotExist() {
        val privateShare = createDefaultPrivateShareEntity(
            shareType = ShareType.GROUP
        )

        ocShareDao.insert(privateShare)

        val privateShareToReplace = createDefaultPrivateShareEntity(
            shareType = ShareType.GROUP,
            shareWith = "userName",
            path = "/Texts/text2.txt"
        )

        ocShareDao.replaceShares(
            listOf(privateShareToReplace)
        )

        val text1Shares = ocShareDao.getSharesAsLiveData(
            privateShare.path,
            privateShare.accountOwner,
            listOf(ShareType.GROUP.value)
        ).getLastEmittedValue()!!

        assertNotNull(text1Shares)
        assertEquals(1, text1Shares.size)
        assertEquals(privateShare.shareWith, text1Shares[0].shareWith)

        // text2 link didn't exist before, it should not replace the old one but be created
        val text2Shares = ocShareDao.getSharesAsLiveData(
            privateShareToReplace.path,
            privateShareToReplace.accountOwner,
            listOf(ShareType.GROUP.value)
        ).getLastEmittedValue()!!

        assertNotNull(text2Shares)
        assertEquals(1, text2Shares.size)
        assertEquals(privateShareToReplace.shareWith, text2Shares[0].shareWith)
    }

    @Test
    fun updatePrivateShare() {
        val privateShare = createDefaultPrivateShareEntity()

        ocShareDao.insert(privateShare)

        ocShareDao.update(
            createDefaultPrivateShareEntity(permissions = 17)
        )

        val textShares = ocShareDao.getSharesAsLiveData(
            privateShare.path,
            privateShare.accountOwner,
            listOf(ShareType.USER.value)
        ).getLastEmittedValue()!!

        assertNotNull(textShares)
        assertEquals(1, textShares.size)
        assertEquals(17, textShares[0].permissions)
    }

    @Test
    fun deletePrivateShare() {
        val privateShare = createDefaultPrivateShareEntity()

        ocShareDao.insert(createDefaultPrivateShareEntity())

        ocShareDao.deleteShare(privateShare.remoteId)

        val textShares = ocShareDao.getSharesAsLiveData(
            privateShare.path,
            privateShare.accountOwner,
            listOf(ShareType.USER.value)
        ).getLastEmittedValue()!!

        assertNotNull(textShares)
        assertEquals(0, textShares.size) // List of textShares empty after deleting the existing share
    }

    private fun createDefaultPrivateShareEntity(
        shareType: ShareType = ShareType.USER,
        shareWith: String = "username",
        path: String = "/Texts/text1.txt",
        permissions: Int = -1,
        shareWithDisplayName: String = "Steve"
    ) = ocShareMapper.toEntity(
        OC_PRIVATE_SHARE.copy(
            shareType = shareType,
            shareWith = shareWith,
            path = path,
            permissions = permissions,
            isFolder = false,
            sharedWithDisplayName = shareWithDisplayName
        )
    )!!

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun getNonExistingPublicShare() {
        val publicShare = createDefaultPublicShareEntity()

        ocShareDao.insert(publicShare)

        val nonExistingPublicShare = ocShareDao.getSharesAsLiveData(
            publicShare.path,
            "user@server",
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(nonExistingPublicShare)
        assertEquals(0, nonExistingPublicShare.size)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_exists() {
        val publicShare = createDefaultPublicShareEntity()

        ocShareDao.insert(publicShare)

        val publicShareToReplace = createDefaultPublicShareEntity(name = "Text 2 link")

        ocShareDao.replaceShares(
            listOf(publicShareToReplace)
        )

        val textShares = ocShareDao.getSharesAsLiveData(
            publicShare.path,
            publicShare.accountOwner,
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!
        assertNotNull(textShares)
        assertEquals(1, textShares.size)
        assertEquals(publicShareToReplace.name, textShares[0].name)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_doesNotExist() {
        val publicShare = createDefaultPublicShareEntity()

        ocShareDao.insert(publicShare)

        val publicShareToReplace = createDefaultPublicShareEntity(path = "/Texts/text2.txt", name = "Text 2 link")

        ocShareDao.replaceShares(
            listOf(publicShareToReplace)
        )

        val text1Shares = ocShareDao.getSharesAsLiveData(
            publicShare.path,
            publicShare.accountOwner,
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(text1Shares)
        assertEquals(1, text1Shares.size)
        assertEquals(publicShare.name, text1Shares[0].name)

        // text2 link didn't exist before, it should not replace the old one but be created
        val text2Shares = ocShareDao.getSharesAsLiveData(
            publicShareToReplace.path,
            publicShareToReplace.accountOwner,
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(text2Shares)
        assertEquals(1, text2Shares.size)
        assertEquals(publicShareToReplace.name, text2Shares[0].name)
    }

    @Test
    fun updatePublicShare() {
        val publicShare = createDefaultPublicShareEntity()

        ocShareDao.insert(publicShare)

        val publicShareToUpdate = createDefaultPublicShareEntity(name = "Text 1 link updated", expirationDate = 2000)

        ocShareDao.update(publicShareToUpdate)

        val textShares = ocShareDao.getSharesAsLiveData(
            publicShareToUpdate.path,
            publicShareToUpdate.accountOwner,
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(textShares)
        assertEquals(1, textShares.size)
        assertEquals(publicShareToUpdate.name, textShares[0].name)
        assertEquals(publicShareToUpdate.expirationDate, textShares[0].expirationDate)
    }

    @Test
    fun deletePublicShare() {
        val publicShare = createDefaultPublicShareEntity()

        ocShareDao.insert(publicShare)

        ocShareDao.deleteShare(publicShare.remoteId)

        val textShares = ocShareDao.getSharesAsLiveData(
            publicShare.path,
            publicShare.accountOwner,
            listOf(ShareType.PUBLIC_LINK.value)
        ).getLastEmittedValue()!!

        assertNotNull(textShares)
        assertEquals(0, textShares.size) // List of textShares empty after deleting the existing share
    }

    private fun createDefaultPublicShareEntity(
        path: String = "/Texts/text1.txt",
        expirationDate: Long = 1000,
        name: String = "Text 1 link"
    ) = ocShareMapper.toEntity(
        OC_PUBLIC_SHARE.copy(
            path = path,
            expirationDate = expirationDate,
            isFolder = false,
            name = name,
            shareLink = "http://server:port/s/1"
        )
    )!!
}
