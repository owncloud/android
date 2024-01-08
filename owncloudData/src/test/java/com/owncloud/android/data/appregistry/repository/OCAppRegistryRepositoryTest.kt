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

import com.owncloud.android.data.appregistry.OCAppRegistryRepository
import com.owncloud.android.data.appregistry.datasources.LocalAppRegistryDataSource
import com.owncloud.android.data.appregistry.datasources.RemoteAppRegistryDataSource
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_APP_REGISTRY
import com.owncloud.android.testutil.OC_APP_REGISTRY_MIMETYPE
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.OC_FILE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

import org.junit.Assert.*

class OCAppRegistryRepositoryTest {


    private val localAppRegistryDataSource = mockk<LocalAppRegistryDataSource>(relaxUnitFun = true)
    private val remoteAppRegistryDataSource = mockk<RemoteAppRegistryDataSource>(relaxUnitFun = true)
    private val localCapabilitiesDataSource = mockk<LocalCapabilitiesDataSource>(relaxUnitFun = true)
    private val appRegistryRepository = OCAppRegistryRepository(
        localAppRegistryDataSource,
        remoteAppRegistryDataSource,
        localCapabilitiesDataSource,
    )

    @Test
    fun `refreshAppRegistryForAccount get the AppRegistry object relate to an account and save it`() {

        every { localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME) } returns OC_CAPABILITY
        every {
            remoteAppRegistryDataSource.getAppRegistryForAccount(
                OC_ACCOUNT_NAME,
                OC_CAPABILITY.filesAppProviders?.appsUrl
            )
        } returns OC_APP_REGISTRY

        appRegistryRepository.refreshAppRegistryForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME, OC_CAPABILITY.filesAppProviders?.appsUrl)
            localAppRegistryDataSource.saveAppRegistryForAccount(OC_APP_REGISTRY)
        }
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns a Flow of AppRegistryMimeType`() = runTest {

        every {
            localAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(
                accountName = OC_ACCOUNT_NAME,
                mimeType = OC_FILE.mimeType
            )
        } returns flowOf(
            OC_APP_REGISTRY_MIMETYPE
        )
        val resultActual = appRegistryRepository.getAppRegistryForMimeTypeAsStream(accountName = OC_ACCOUNT_NAME, mimeType = OC_FILE.mimeType).first()

        assertEquals(OC_APP_REGISTRY_MIMETYPE, resultActual)

        verify(exactly = 1) {
            localAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(
                accountName = OC_ACCOUNT_NAME,
                mimeType = OC_FILE.mimeType
            )
        }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns a Flow of List of AppRegistryMimeType`() = runTest {

        every { localAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) } returns
                flowOf(listOf(OC_APP_REGISTRY_MIMETYPE))
        val resultActual = appRegistryRepository.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME).first()

        assertEquals(listOf(OC_APP_REGISTRY_MIMETYPE), resultActual)

        verify(exactly = 1) { localAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getUrlToOpenInWeb returns a String correctly`() {
        val expectedUrl = "https://example.com/file123"
        val openWebEndpoint = "app/open-with-web"
        val appName = "ownCloud"
        every {
            remoteAppRegistryDataSource.getUrlToOpenInWeb(
                accountName = OC_ACCOUNT_NAME,
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.id.toString(),
                appName = appName
            )
        } returns expectedUrl

        val resultActual = appRegistryRepository.getUrlToOpenInWeb(
            accountName = OC_ACCOUNT_NAME,
            openWebEndpoint = openWebEndpoint,
            fileId = OC_FILE.id.toString(),
            appName = appName
        )

        assertEquals(expectedUrl, resultActual)

        verify(exactly = 1) {
            remoteAppRegistryDataSource.getUrlToOpenInWeb(
                accountName = OC_ACCOUNT_NAME,
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.id.toString(),
                appName = appName
            )
        }
    }

    @Test
    fun `createFileWithAppProvider returns a String`() {
        val createFileWithAppProviderEndpoint = "app/file-with-app-provider-endpoint"
        val parentContainerId = OC_FILE.remoteId.toString()
        val fileWithAppProvider = "fileWithAppProvider"
        every {
            remoteAppRegistryDataSource.createFileWithAppProvider(
                accountName = OC_ACCOUNT_NAME,
                createFileWithAppProviderEndpoint = createFileWithAppProviderEndpoint,
                parentContainerId = parentContainerId,
                filename = OC_FILE.fileName,
            )
        } returns fileWithAppProvider

        val resultActual = remoteAppRegistryDataSource.createFileWithAppProvider(
            accountName = OC_ACCOUNT_NAME,
            createFileWithAppProviderEndpoint = createFileWithAppProviderEndpoint,
            parentContainerId = parentContainerId,
            filename = OC_FILE.fileName,
        )
        assertEquals(fileWithAppProvider, resultActual)

        verify(exactly = 1) {
            remoteAppRegistryDataSource.createFileWithAppProvider(
                accountName = OC_ACCOUNT_NAME,
                createFileWithAppProviderEndpoint = createFileWithAppProviderEndpoint,
                parentContainerId = parentContainerId,
                filename = OC_FILE.fileName,
            )
        }
    }
}