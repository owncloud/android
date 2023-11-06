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

package com.owncloud.android.data.appregistry.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.domain.appregistry.model.AppRegistry
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.appregistry.responses.AppRegistryResponse
import com.owncloud.android.lib.resources.appregistry.services.OCAppRegistryService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteAppRegistryDataSourceTest {

    private lateinit var ocRemoteAppRegistryDataSource: OCRemoteAppRegistryDataSource
    private val clientManager: ClientManager = mockk(relaxed = true)
    private val ocAppRegistryService: OCAppRegistryService = mockk()
    private val appResgitryResponse = AppRegistryResponse(value = mockk(relaxed = true))
    private val openWebEndpoint = "https://example.com"
    private val expectedFileUrl = "https://example.com/files/testFile.txt"
    private val expectedUrl = "https://example.com/file123/TestApp"
    private val appUrl = "storage/file123/TestApp"
    private val appName = "TestApp"

    @Before
    fun setUp() {
        every { clientManager.getAppRegistryService(any()) } returns ocAppRegistryService

        ocRemoteAppRegistryDataSource = OCRemoteAppRegistryDataSource(clientManager)
    }

    @Test
    fun `getAppRegistryForAccount returns the appRegistry object`() {
        val getAppRegistryResult: RemoteOperationResult<AppRegistryResponse> =
            createRemoteOperationResultMock(data = appResgitryResponse, isSuccess = true)

        val appResgitryMock = AppRegistry(accountName = OC_ACCOUNT_NAME, mimetypes = emptyList())

        every { ocAppRegistryService.getAppRegistry(any()) } returns getAppRegistryResult

        val appRegistry = ocRemoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME, appUrl)

        assertEquals(appResgitryMock, appRegistry)

        verify(exactly = 1) { ocAppRegistryService.getAppRegistry(appUrl) }
    }

    @Test(expected = Exception::class)
    fun `getAppRegistryForAccount returns an Exception when the operation is not successful`() {
        val getAppRegistryResult: RemoteOperationResult<AppRegistryResponse> =
            createRemoteOperationResultMock(data = appResgitryResponse, isSuccess = false)

        every { ocAppRegistryService.getAppRegistry(any()) } returns getAppRegistryResult

        ocRemoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME, appUrl)

    }

    @Test(expected = Exception::class)
    fun `getAppRegistryForAccount returns an Exception when getAppRegistry() has an error controlled by an Exception`() {
        every { ocAppRegistryService.getAppRegistry(any()) } throws Exception()

        ocRemoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME, appUrl)
    }

    @Test
    fun `getUrlToOpenInWeb returns an url to open in website`() {
        val getUrlToOpenInWebResult: RemoteOperationResult<String> = createRemoteOperationResultMock(data = expectedUrl, isSuccess = true)

        every {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName,
            )
        } returns getUrlToOpenInWebResult

        val result = ocAppRegistryService.getUrlToOpenInWeb(
            openWebEndpoint = openWebEndpoint,
            fileId = OC_FILE.remoteId.toString(),
            appName = appName,
        )

        assertEquals(expectedUrl, result.data)

        verify {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName,
            )
        }
    }

    @Test(expected = Exception::class)
    fun `getUrlToOpenInWeb returns an exception when something there is an error in the method`() {

        every {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName,
            )
        } throws Exception()

        ocAppRegistryService.getUrlToOpenInWeb(
            openWebEndpoint = openWebEndpoint,
            fileId = OC_FILE.remoteId.toString(),
            appName = appName,
        )
    }

    @Test
    fun `createFileWithAppProvider returns the url to open in web`() {

        val createFileWithAppProviderResult: RemoteOperationResult<String> = createRemoteOperationResultMock(data = expectedFileUrl, isSuccess = true)

        every {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = openWebEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName,
            )
        } returns createFileWithAppProviderResult

        val result = ocAppRegistryService.createFileWithAppProvider(
            createFileWithAppProviderEndpoint = openWebEndpoint,
            parentContainerId = OC_FILE.remoteId.toString(),
            filename = OC_FILE.fileName,
        )

        assertEquals(expectedFileUrl, result.data)

        verify(exactly = 1) {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = openWebEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName,
            )
        }
    }

    @Test(expected = Exception::class)
    fun `createFileWithAppProvider returns an Exception in method`() {

        every {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = openWebEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName,
            )
        } throws Exception()

        ocAppRegistryService.createFileWithAppProvider(
            createFileWithAppProviderEndpoint = openWebEndpoint,
            parentContainerId = OC_FILE.remoteId.toString(),
            filename = OC_FILE.fileName,
        )
    }
}
