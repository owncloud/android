/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.data.spaces.datasources.implementation

import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toEntity
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toModel
import com.owncloud.android.data.spaces.db.SpaceSpecialEntity
import com.owncloud.android.data.spaces.db.SpacesDao
import com.owncloud.android.data.spaces.db.SpacesEntity
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.OCSpace.Companion.SPACE_ID_SHARES
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.testutil.SPACE_ENTITY_SHARE
import com.owncloud.android.testutil.SPACE_ENTITY_WITH_SPECIALS
import com.owncloud.android.testutil.WEB_DAV_URL
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCLocalSpacesDataSourceTest {

    private lateinit var ocLocalSpacesDataSource: OCLocalSpacesDataSource
    private val spacesDao = mockk<SpacesDao>()

    @Before
    fun setUp() {
        ocLocalSpacesDataSource = OCLocalSpacesDataSource(spacesDao)
    }

    @Test
    fun `saveSpacesForAccount inserts spaces and special spaces correctly`() {
        val spaceEntities = mutableListOf<SpacesEntity>()
        val spaceSpecialEntities = mutableListOf<SpaceSpecialEntity>()

        listOf(OC_SPACE_PROJECT_WITH_IMAGE).forEach { spaceModel ->
            spaceEntities.add(spaceModel.toEntity())
            spaceModel.special?.let { listOfSpacesSpecials ->
                spaceSpecialEntities.addAll(listOfSpacesSpecials.map { it.toEntity(spaceModel.accountName, spaceModel.id) })
            }
        }

        every {
            spacesDao.insertOrDeleteSpaces(any(), any())
        } just Runs

        ocLocalSpacesDataSource.saveSpacesForAccount(listOf(OC_SPACE_PROJECT_WITH_IMAGE))

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
        } returns SPACE_ENTITY_SHARE.space

        val resultActual = ocLocalSpacesDataSource.getSharesSpaceForAccount(OC_ACCOUNT_NAME)

        assertEquals(SPACE_ENTITY_SHARE.space.toModel(), resultActual)

        verify(exactly = 1) {
            spacesDao.getSpaceByIdForAccount(SPACE_ID_SHARES, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getSpacesFromEveryAccountAsStream returns a Flow with a list of OCSpace`() = runBlocking {

        every {
            spacesDao.getSpacesByDriveTypeFromEveryAccountAsStream(
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        } returns flowOf(listOf(OC_SPACE_PERSONAL.toEntity()))

        val resultActual = ocLocalSpacesDataSource.getSpacesFromEveryAccountAsStream().first()

        assertEquals(listOf(OC_SPACE_PERSONAL), resultActual)

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
    fun `getSpacesByDriveTypeWithSpecialsForAccountAsFlow returns a Flow with a list of OCSpace`() = runBlocking {

        every {
            spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
                OC_ACCOUNT_NAME,
                setOf(
                    OCSpace.DRIVE_TYPE_PERSONAL,
                    OCSpace.DRIVE_TYPE_PROJECT
                )
            )
        } returns flowOf(listOf(SPACE_ENTITY_WITH_SPECIALS))

        val resultActual = ocLocalSpacesDataSource.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            OC_ACCOUNT_NAME, setOf(
                OCSpace.DRIVE_TYPE_PERSONAL,
                OCSpace.DRIVE_TYPE_PROJECT
            )
        )

        val result = resultActual.first()
        assertEquals(listOf(SPACE_ENTITY_WITH_SPECIALS.toModel()), result)


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
        } returns SPACE_ENTITY_WITH_SPECIALS

        val resultActual = ocLocalSpacesDataSource.getSpaceWithSpecialsByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)

        assertEquals(SPACE_ENTITY_WITH_SPECIALS.toModel(), resultActual)

        verify(exactly = 1) {
            spacesDao.getSpaceWithSpecialsByIdForAccount(
                OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME
            )
        }
    }

    @Test
    fun `getSpaceByIdForAccount returns a OCSpace`() {

        every {
            spacesDao.getSpaceByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)
        } returns OC_SPACE_PERSONAL.toEntity()

        val result = ocLocalSpacesDataSource.getSpaceByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)

        assertEquals(OC_SPACE_PERSONAL, result)

        verify(exactly = 1) {
            spacesDao.getSpaceByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getWebDavUrlForSpace returns a String of webDavUrl`() {

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
    fun `deleteSpacesForAccount removes the spaces for an account correctly`() {

        every {
            spacesDao.deleteSpacesForAccount(OC_ACCOUNT_NAME)
        } returns Unit

        ocLocalSpacesDataSource.deleteSpacesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            spacesDao.deleteSpacesForAccount(OC_ACCOUNT_NAME)
        }
    }
}
