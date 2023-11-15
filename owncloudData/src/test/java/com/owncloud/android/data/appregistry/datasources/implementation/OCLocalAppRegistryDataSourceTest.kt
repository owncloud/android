/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
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

package com.owncloud.android.data.appregistry.datasources.implementation

import com.owncloud.android.data.appregistry.db.AppRegistryDao
import com.owncloud.android.domain.appregistry.model.AppRegistry
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_APP_REGISTRY_ENTITY
import com.owncloud.android.testutil.OC_APP_REGISTRY_MIMETYPE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class OCLocalAppRegistryDataSourceTest {
    private lateinit var ocLocalAppRegistryDataSource: OCLocalAppRegistryDataSource
    private val appRegistryDao = mockk<AppRegistryDao>(relaxUnitFun = true)

    private val mimeTypeDir = "DIR"

    @Before
    fun setUp() {

        ocLocalAppRegistryDataSource = OCLocalAppRegistryDataSource(appRegistryDao)
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns a Flow with an AppRegistryMimeType`() = runTest {

        every { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimeTypeDir) } returns flowOf(OC_APP_REGISTRY_ENTITY)

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimeTypeDir)

        val result = appRegistry.first()
        assertEquals(OC_APP_REGISTRY_MIMETYPE, result)

        verify(exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimeTypeDir) }
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns a Flow with null when there are no app registries for that mime type`() = runTest {

        every { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimeTypeDir) } returns flowOf(null)

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimeTypeDir)

        val result = appRegistry.first()
        assertNull(result)

        verify(exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimeTypeDir) }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns a Flow with a list of AppRegistryMimeType`() = runTest {

        every { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) } returns flowOf(listOf(OC_APP_REGISTRY_ENTITY))

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME)

        val result = appRegistry.first()
        assertEquals(listOf(OC_APP_REGISTRY_MIMETYPE), result)

        verify(exactly = 1) { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns an empty list when there are no app registries that allow creation`() = runTest {

        every { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) } returns flowOf(emptyList())

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME)

        val result = appRegistry.first()
        assertEquals(emptyList<AppRegistryMimeType>(), result)

        verify(exactly = 1) { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `saveAppRegistryForAccount saves an AppRegistry correctly`() {
        val appRegistryOtherName = "appRegistryMimeTypes.name2"
        val appRegistry = AppRegistry(
            OC_ACCOUNT_NAME, mutableListOf(
                OC_APP_REGISTRY_MIMETYPE,
                OC_APP_REGISTRY_MIMETYPE.copy(name = appRegistryOtherName)
            )
        )

        ocLocalAppRegistryDataSource.saveAppRegistryForAccount(appRegistry)

        verify(exactly = 1) {
            appRegistryDao.deleteAppRegistryForAccount(appRegistry.accountName)
            appRegistryDao.upsertAppRegistries(listOf(OC_APP_REGISTRY_ENTITY, OC_APP_REGISTRY_ENTITY.copy(name = appRegistryOtherName)))
        }
    }

    @Test
    fun `deleteAppRegistryForAccount removes the app registries for an account correctly`() {

        ocLocalAppRegistryDataSource.deleteAppRegistryForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) { appRegistryDao.deleteAppRegistryForAccount(OC_ACCOUNT_NAME) }
    }
}
