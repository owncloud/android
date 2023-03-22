/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.data.spaces.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toEntity
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toModel
import com.owncloud.android.domain.spaces.model.OCSpace.Companion.DRIVE_TYPE_PROJECT
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITHOUT_IMAGE
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.testutil.OC_SPACE_SPECIAL_IMAGE
import com.owncloud.android.testutil.OC_SPACE_SPECIAL_README
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@MediumTest
class SpacesDaoTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var spacesDao: SpacesDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OwncloudDatabase.switchToInMemory(context)
        val db: OwncloudDatabase = OwncloudDatabase.getDatabase(context)
        spacesDao = db.spacesDao()
    }

    @Test
    fun insertOrDeleteSpacesWithEmptyDatabase() = runTest {
        val accountName = OC_SPACE_PROJECT_WITHOUT_IMAGE.accountName

        val specialsToInsert = listOf(
            OC_SPACE_SPECIAL_README.toEntity(accountName, OC_SPACE_PROJECT_WITHOUT_IMAGE.id)
        )

        val spacesToInsertModel = listOf(
            OC_SPACE_PROJECT_WITHOUT_IMAGE
        )

        val spacesToInsert = spacesToInsertModel.map { it.toEntity() }

        spacesDao.insertOrDeleteSpaces(spacesToInsert, specialsToInsert)

        val spacesInDatabase = spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            accountName = accountName,
            filterDriveTypes = setOf(DRIVE_TYPE_PROJECT),
        ).first().map { it.toModel() }

        assertNotNull(spacesInDatabase)
        assertEquals(1, spacesInDatabase.size)
        assertEquals(spacesToInsertModel, spacesInDatabase)
    }

    @Test
    fun insertOrDeleteSpacesWithSpacesAlreadyInDatabaseNotAttachedToAccountAnymore() = runTest {
        val accountName = OC_SPACE_PROJECT_WITHOUT_IMAGE.accountName

        val specialsAlreadyInDatabaseToInsert = listOf(
            OC_SPACE_SPECIAL_IMAGE.toEntity(accountName, OC_SPACE_PROJECT_WITH_IMAGE.id),
            OC_SPACE_SPECIAL_README.toEntity(accountName, OC_SPACE_PROJECT_WITH_IMAGE.id)
        )

        val spacesAlreadyInDatabaseToInsert = listOf(
            OC_SPACE_PROJECT_WITH_IMAGE.toEntity()
        )

        spacesDao.insertOrDeleteSpaces(spacesAlreadyInDatabaseToInsert, specialsAlreadyInDatabaseToInsert)

        val spacesAlreadyInDatabase = spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            accountName = accountName,
            filterDriveTypes = setOf(DRIVE_TYPE_PROJECT),
        ).first()

        assertEquals(1, spacesAlreadyInDatabase.size)
        assertEquals(2, spacesAlreadyInDatabase[0].specials.size)

        val newSpecialsToInsert = listOf(
            OC_SPACE_SPECIAL_README.toEntity(accountName, OC_SPACE_PROJECT_WITHOUT_IMAGE.id),
        )

        val newSpacesToInsertModel = listOf(
            OC_SPACE_PROJECT_WITHOUT_IMAGE
        )

        val newSpacesToInsert = newSpacesToInsertModel.map { it.toEntity() }

        spacesDao.insertOrDeleteSpaces(newSpacesToInsert, newSpecialsToInsert)

        val spacesInDatabaseEntity = spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            accountName = accountName,
            filterDriveTypes = setOf(DRIVE_TYPE_PROJECT),
        ).first()

        val spacesInDatabase = spacesInDatabaseEntity.map { it.toModel() }
        val specialsInDatabase = spacesInDatabaseEntity.flatMap { it.specials }

        assertNotNull(spacesInDatabase)
        assertEquals(1, spacesInDatabase.size)
        assertEquals(1, specialsInDatabase.size)
        assertEquals(newSpacesToInsertModel, spacesInDatabase)
    }

    @Test
    fun insertOrDeleteSpacesWithSpacesAlreadyInDatabaseStillAttachedToAccount() = runTest {
        val accountName = OC_SPACE_PROJECT_WITHOUT_IMAGE.accountName

        val specialsAlreadyInDatabaseToInsert = listOf(
            OC_SPACE_SPECIAL_IMAGE.toEntity(accountName, OC_SPACE_PROJECT_WITH_IMAGE.id),
            OC_SPACE_SPECIAL_README.toEntity(accountName, OC_SPACE_PROJECT_WITH_IMAGE.id)
        )

        val spacesAlreadyInDatabaseToInsert = listOf(
            OC_SPACE_PROJECT_WITH_IMAGE.toEntity()
        )

        spacesDao.insertOrDeleteSpaces(spacesAlreadyInDatabaseToInsert, specialsAlreadyInDatabaseToInsert)

        val spacesAlreadyInDatabase = spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            accountName = accountName,
            filterDriveTypes = setOf(DRIVE_TYPE_PROJECT),
        ).first()

        assertEquals(1, spacesAlreadyInDatabase.size)
        assertEquals(2, spacesAlreadyInDatabase[0].specials.size)

        val anotherSpaceId = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568190"

        val newSpecialsToInsert = listOf(
            OC_SPACE_SPECIAL_README.copy(
                id = "$anotherSpaceId!1c7bbc13-469f-482c-8f13-55ae1402b4c4"
            ).toEntity(accountName, OC_SPACE_PROJECT_WITHOUT_IMAGE.id),
        ) + specialsAlreadyInDatabaseToInsert

        val newSpacesToInsert = listOf(
            OC_SPACE_PROJECT_WITHOUT_IMAGE.toEntity()
        ) + spacesAlreadyInDatabaseToInsert

        spacesDao.insertOrDeleteSpaces(newSpacesToInsert, newSpecialsToInsert)

        val spacesInDatabase = spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            accountName = accountName,
            filterDriveTypes = setOf(DRIVE_TYPE_PROJECT),
        ).first()
        val specialsInDatabase = spacesInDatabase.flatMap { it.specials }

        assertNotNull(spacesInDatabase)
        assertEquals(2, spacesInDatabase.size)
        assertEquals(3, specialsInDatabase.size)
    }
}
