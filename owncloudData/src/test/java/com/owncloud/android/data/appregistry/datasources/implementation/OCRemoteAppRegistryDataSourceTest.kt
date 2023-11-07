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

import com.owncloud.android.data.ClientManager
import com.owncloud.android.lib.resources.appregistry.services.OCAppRegistryService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_APP_REGISTRY
import com.owncloud.android.testutil.OC_APP_REGISTRY_RESPONSE
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

    private val appUrl = "app/list"
    private val testEndpoint = "app/open-with-web"

    @Before
    fun setUp() {
        every { clientManager.getAppRegistryService(OC_ACCOUNT_NAME) } returns ocAppRegistryService

        ocRemoteAppRegistryDataSource = OCRemoteAppRegistryDataSource(clientManager)
    }

    @Test
    fun `getAppRegistryForAccount returns an AppRegistry`() {
        val getAppRegistryForAccountResult = createRemoteOperationResultMock(
            data = OC_APP_REGISTRY_RESPONSE, isSuccess = true
        )

        every { ocAppRegistryService.getAppRegistry(appUrl) } returns getAppRegistryForAccountResult

        val result = ocRemoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME, appUrl)

        assertEquals(OC_APP_REGISTRY, result)

        verify(exactly = 1) { ocAppRegistryService.getAppRegistry(appUrl) }
    }

    @Test
    fun `getUrlToOpenInWeb returns a URL String`() {
        val expectedUrl = "https://example.com/file123"
        val appName = "TestApp"

        val getUrlToOpenInWebResult = createRemoteOperationResultMock(
            data = expectedUrl, isSuccess = true
        )

        every {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = testEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName,
            )
        } returns getUrlToOpenInWebResult

        val result = ocRemoteAppRegistryDataSource.getUrlToOpenInWeb(
            accountName = OC_ACCOUNT_NAME,
            openWebEndpoint = testEndpoint,
            fileId = OC_FILE.remoteId.toString(),
            appName = appName,
        )

        assertEquals(expectedUrl, result)

        verify {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = testEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName,
            )
        }
    }

    @Test
    fun `createFileWithAppProvider returns a URL String to open the file in web`() {
        val expectedFileUrl = "https://example.com/files/testFile.txt"

        val createFileWithAppProviderResult = createRemoteOperationResultMock(
            data = expectedFileUrl, isSuccess = true)

        every {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = testEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName,
            )
        } returns createFileWithAppProviderResult

        val result = ocRemoteAppRegistryDataSource.createFileWithAppProvider(
            accountName = OC_ACCOUNT_NAME,
            createFileWithAppProviderEndpoint = testEndpoint,
            parentContainerId = OC_FILE.remoteId.toString(),
            filename = OC_FILE.fileName,
        )

        assertEquals(expectedFileUrl, result)

        verify(exactly = 1) {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = testEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName,
            )
        }
    }
}
