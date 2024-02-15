/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.data.appregistry.repository

import com.owncloud.android.data.appregistry.datasources.LocalAppRegistryDataSource
import com.owncloud.android.data.appregistry.datasources.RemoteAppRegistryDataSource
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_APP_REGISTRY
import com.owncloud.android.testutil.OC_APP_REGISTRY_MIMETYPE
import com.owncloud.android.testutil.OC_CAPABILITY_WITH_FILES_APP_PROVIDERS
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class OCAppRegistryRepositoryTest {

    private val localAppRegistryDataSource = mockk<LocalAppRegistryDataSource>(relaxUnitFun = true)
    private val remoteAppRegistryDataSource = mockk<RemoteAppRegistryDataSource>()
    private val localCapabilitiesDataSource = mockk<LocalCapabilitiesDataSource>(relaxUnitFun = true)
    private val ocAppRegistryRepository = OCAppRegistryRepository(
        localAppRegistryDataSource,
        remoteAppRegistryDataSource,
        localCapabilitiesDataSource,
    )

    @Test
    fun `refreshAppRegistryForAccount fetches the AppRegistry of an account and saves it`() {

        every { localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME) } returns OC_CAPABILITY_WITH_FILES_APP_PROVIDERS
        every {
            remoteAppRegistryDataSource.getAppRegistryForAccount(
                OC_ACCOUNT_NAME,
                OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.appsUrl?.substring(1)
            )
        } returns OC_APP_REGISTRY

        ocAppRegistryRepository.refreshAppRegistryForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteAppRegistryDataSource.getAppRegistryForAccount(
                OC_ACCOUNT_NAME,
                OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.appsUrl?.substring(1)
            )
            localAppRegistryDataSource.saveAppRegistryForAccount(OC_APP_REGISTRY)
        }
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns a Flow of AppRegistryMimeType`() = runTest {
        val mimeType = "DIR"
        every {
            localAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(
                accountName = OC_ACCOUNT_NAME,
                mimeType = mimeType
            )
        } returns flowOf(
            OC_APP_REGISTRY_MIMETYPE
        )
        val resultActual =
            ocAppRegistryRepository.getAppRegistryForMimeTypeAsStream(accountName = OC_ACCOUNT_NAME, mimeType = mimeType).first()

        assertEquals(OC_APP_REGISTRY_MIMETYPE, resultActual)

        verify(exactly = 1) {
            localAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(
                accountName = OC_ACCOUNT_NAME,
                mimeType = mimeType
            )
        }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns a Flow of List of AppRegistryMimeType`() = runTest {

        every { localAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) } returns
                flowOf(listOf(OC_APP_REGISTRY_MIMETYPE))
        val resultActual = ocAppRegistryRepository.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME).first()

        assertEquals(listOf(OC_APP_REGISTRY_MIMETYPE), resultActual)

        verify(exactly = 1) { localAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getUrlToOpenInWeb returns a URL String`() {
        val expectedUrl = "https://example.com/file123"
        val appName = "ownCloud"
        every {
            remoteAppRegistryDataSource.getUrlToOpenInWeb(
                accountName = OC_ACCOUNT_NAME,
                openWebEndpoint = OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.openWebUrl!!,
                fileId = OC_FILE.remoteId!!,
                appName = appName
            )
        } returns expectedUrl

        val resultActual = ocAppRegistryRepository.getUrlToOpenInWeb(
            accountName = OC_ACCOUNT_NAME,
            openWebEndpoint = OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.openWebUrl!!,
            fileId = OC_FILE.remoteId!!,
            appName = appName
        )

        assertEquals(expectedUrl, resultActual)

        verify(exactly = 1) {
            remoteAppRegistryDataSource.getUrlToOpenInWeb(
                accountName = OC_ACCOUNT_NAME,
                openWebEndpoint = OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.openWebUrl!!,
                fileId = OC_FILE.remoteId!!,
                appName = appName
            )
        }
    }

    @Test
    fun `createFileWithAppProvider returns a String with the new file ID`() {

        every {
            remoteAppRegistryDataSource.createFileWithAppProvider(
                accountName = OC_ACCOUNT_NAME,
                createFileWithAppProviderEndpoint = OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.newUrl!!,
                parentContainerId = OC_FOLDER.remoteId!!,
                filename = OC_FILE.fileName,
            )
        } returns OC_FILE.remoteId!!

        val resultActual = ocAppRegistryRepository.createFileWithAppProvider(
            accountName = OC_ACCOUNT_NAME,
            createFileWithAppProviderEndpoint = OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.newUrl!!,
            parentContainerId = OC_FOLDER.remoteId!!,
            filename = OC_FILE.fileName,
        )
        assertEquals(OC_FILE.remoteId!!, resultActual)

        verify(exactly = 1) {
            remoteAppRegistryDataSource.createFileWithAppProvider(
                accountName = OC_ACCOUNT_NAME,
                createFileWithAppProviderEndpoint = OC_CAPABILITY_WITH_FILES_APP_PROVIDERS.filesAppProviders?.newUrl!!,
                parentContainerId = OC_FOLDER.remoteId!!,
                filename = OC_FILE.fileName,
            )
        }
    }
}
