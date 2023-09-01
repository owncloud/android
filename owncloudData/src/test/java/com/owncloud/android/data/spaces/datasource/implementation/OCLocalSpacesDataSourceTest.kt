package com.owncloud.android.data.spaces.datasource.implementation

import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toEntity
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toModel
import com.owncloud.android.data.spaces.db.SpaceRootEntity
import com.owncloud.android.data.spaces.db.SpaceSpecialEntity
import com.owncloud.android.data.spaces.db.SpacesDao
import com.owncloud.android.data.spaces.db.SpacesEntity
import com.owncloud.android.data.spaces.db.SpacesWithSpecials
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.OCSpace.Companion.SPACE_ID_SHARES
import com.owncloud.android.domain.spaces.model.SpaceDeleted
import com.owncloud.android.domain.spaces.model.SpaceOwner
import com.owncloud.android.domain.spaces.model.SpaceQuota
import com.owncloud.android.domain.spaces.model.SpaceRoot
import com.owncloud.android.domain.spaces.model.SpaceUser
import com.owncloud.android.testutil.OC_ACCOUNT_ID
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CLIENT_ID
import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import com.owncloud.android.testutil.OC_SPACE_SPECIAL_IMAGE
import com.owncloud.android.testutil.OC_SPACE_SPECIAL_README
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCLocalSpacesDataSourceTest {

    private lateinit var ocLocalSpacesDataSource: OCLocalSpacesDataSource
    private val spacesDao = mockkClass(SpacesDao::class)

    private val WEB_DAV_URL = "https://server.url/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f3805bca744-d89f-4e9c-a990-25a0d7f03fe9"

    private val spaceWithSpecials = SpacesWithSpecials(
        SpacesEntity(
            accountName = OC_ACCOUNT_NAME,
            driveAlias = "driveAlias",
            driveType = "driveType",
            id = OC_ACCOUNT_ID,
            ownerId = OC_CLIENT_ID,
            lastModifiedDateTime = "lastModifiedDateTime",
            name = "name",
            quota = null,
            root = SpaceRootEntity(
                eTag = "eTag",
                id = "id",
                webDavUrl = WEB_DAV_URL,
                deleteState = "state"
            ),
            webUrl = "webUrl",
            description = "description"
        ),
        listOf(
            SpaceSpecialEntity(
                accountName = OC_ACCOUNT_NAME,
                eTag = "eTag",
                fileMimeType = "fileMimeType",
                id = OC_ACCOUNT_ID,
                spaceId = OC_SPACE_PERSONAL.id,
                lastModifiedDateTime = "lastModifiedDateTime",
                name = "name",
                webDavUrl = WEB_DAV_URL,
                size = 100,
                specialFolderName = OC_SPACE_SPECIAL_IMAGE.name
            )
        )
    )

    private val listSpaceSpecial = listOf(
        OCSpace(
            accountName = OC_ACCOUNT_NAME,
            driveAlias = "driveAlias",
            driveType = "driveType",
            id = OC_ACCOUNT_ID,
            lastModifiedDateTime = "lastModifiedDateTime",
            name = "name",
            owner = SpaceOwner(
                user = SpaceUser(
                    id = "id"
                )
            ),
            quota = SpaceQuota(
                remaining = 100,
                state = "quotaResponse.state",
                total = 100,
                used = 100,
            ),
            root = SpaceRoot(
                eTag = "eTag",
                id = "id",
                webDavUrl = WEB_DAV_URL,
                deleted = SpaceDeleted(state = "state")
            ),
            webUrl = "webUrl",
            description = "description",
            special = listOf(
                OC_SPACE_SPECIAL_IMAGE,
                OC_SPACE_SPECIAL_README
            )
        )
    )

    @Before
    fun init() {
        val db = mockkClass(OwncloudDatabase::class)

        every { db.spacesDao() } returns spacesDao

        ocLocalSpacesDataSource = OCLocalSpacesDataSource(spacesDao)
    }

    @Test
    fun `saveSpacesForAccount inserts spaces and special spaces returns unit`() {
        val spaceEntities = mutableListOf<SpacesEntity>()
        val spaceSpecialEntities = mutableListOf<SpaceSpecialEntity>()

        listSpaceSpecial.forEach { spaceModel ->
            spaceEntities.add(spaceModel.toEntity())
            spaceModel.special?.let { listOfSpacesSpecials ->
                spaceSpecialEntities.addAll(listOfSpacesSpecials.map { it.toEntity(spaceModel.accountName, spaceModel.id) })
            }
        }

        every {
            spacesDao.insertOrDeleteSpaces(any(), any())
        } just Runs

        ocLocalSpacesDataSource.saveSpacesForAccount(listSpaceSpecial)

        verify(exactly = 1) {
            spacesDao.insertOrDeleteSpaces(spaceEntities, spaceSpecialEntities)
        }
    }

    @Test
    fun `getPersonalSpaceForAccount by drive type returns a OCSpace`() {
        every {
            spacesDao.getSpacesByDriveTypeForAccount(any(), any())
        } returns listOf(OC_SPACE_PERSONAL.toEntity())

        val resultActual = ocLocalSpacesDataSource.getPersonalSpaceForAccount(OC_ACCOUNT_NAME)

        assertEquals(OC_SPACE_PERSONAL, resultActual)

        verify(exactly = 1) {
            spacesDao.getSpacesByDriveTypeForAccount(OC_ACCOUNT_NAME, setOf(OCSpace.DRIVE_TYPE_PERSONAL))
        }
    }

    @Test
    fun `getSharesSpaceForAccount returns a OCSpace`() {
        every {
            spacesDao.getSpaceByIdForAccount(SPACE_ID_SHARES, OC_ACCOUNT_NAME)
        } returns OC_SPACE_PERSONAL.toEntity()

        val resultActual = ocLocalSpacesDataSource.getSharesSpaceForAccount(OC_ACCOUNT_NAME)

        assertEquals(OC_SPACE_PERSONAL, resultActual)

        verify(exactly = 1) {
            spacesDao.getSpaceByIdForAccount(SPACE_ID_SHARES, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getSpacesFromEveryAccountAsStream returns a flow of OCSpace`() = runBlocking {

        every {
            spacesDao.getSpacesByDriveTypeFromEveryAccountAsStream(
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        } returns flowOf(listOf(OC_SPACE_PERSONAL.toEntity()))

        val resultActual = ocLocalSpacesDataSource.getSpacesFromEveryAccountAsStream()

        resultActual.collect { result ->
            assertEquals(listOf(OC_SPACE_PERSONAL), result)
        }

        verify(exactly = 1) {
            spacesDao.getSpacesByDriveTypeFromEveryAccountAsStream(
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        }
    }

    @Test
    fun `getSpacesByDriveTypeWithSpecialsForAccountAsFlow returns a flow of OCSpace list`() = runBlocking {

        every {
            spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
                OC_ACCOUNT_NAME,
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        } returns flowOf(listOf(spaceWithSpecials))

        val resultActual = ocLocalSpacesDataSource.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            OC_ACCOUNT_NAME, setOf(
                OCSpace.DRIVE_TYPE_PERSONAL,
                OCSpace.DRIVE_TYPE_PROJECT
            )
        )

        resultActual.collect { result ->
            assertEquals(listOf(spaceWithSpecials.toModel()), result)
        }

        verify(exactly = 1) {
            spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
                OC_ACCOUNT_NAME,
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        }
    }

    @Test
    fun `getPersonalAndProjectSpacesForAccount returns a list of OCSpace`() {

        every {
            spacesDao.getSpacesByDriveTypeForAccount(
                OC_ACCOUNT_NAME,
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        } returns listOf(OC_SPACE_PERSONAL.toEntity())

        val resultActual = ocLocalSpacesDataSource.getPersonalAndProjectSpacesForAccount(OC_ACCOUNT_NAME)

        assertEquals(listOf(OC_SPACE_PERSONAL), resultActual)


        verify(exactly = 1) {
            spacesDao.getSpacesByDriveTypeForAccount(
                OC_ACCOUNT_NAME,
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        }
    }

    @Test
    fun `getSpaceWithSpecialsByIdForAccount returns a OCSpace`() {

        every {
            spacesDao.getSpaceWithSpecialsByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)
        } returns spaceWithSpecials

        val resultActual = ocLocalSpacesDataSource.getSpaceWithSpecialsByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)

        assertEquals(spaceWithSpecials.toModel(), resultActual)

        verify(exactly = 1) {
            spacesDao.getSpaceWithSpecialsByIdForAccount(
                OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME
            )
        }
    }

    @Test
    fun `getWebDavUrlForSpace returns a string of webDavUrl`() {

        every {
            spacesDao.getWebDavUrlForSpace(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)
        } returns WEB_DAV_URL

        val resultActual = ocLocalSpacesDataSource.getWebDavUrlForSpace(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)

        assertEquals(WEB_DAV_URL, resultActual)

        verify(exactly = 1) {
            spacesDao.getWebDavUrlForSpace(
                OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME
            )
        }
    }

    @Test
    fun `deleteSpacesForAccount delete the space by account returns Unit`() {

        every {
            spacesDao.deleteSpacesForAccount(OC_ACCOUNT_NAME)
        } returns Unit

        ocLocalSpacesDataSource.deleteSpacesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            spacesDao.deleteSpacesForAccount(OC_ACCOUNT_NAME)
        }
    }
}