package com.owncloud.android.data.appRegistry.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.appregistry.datasources.implementation.OCRemoteAppRegistryDataSource
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

    private val appName = "TestApp"

    @Before
    fun init() {
        every { clientManager.getAppRegistryService(any()) } returns ocAppRegistryService

        ocRemoteAppRegistryDataSource = OCRemoteAppRegistryDataSource(clientManager)
    }

    @Test
    fun `getAppRegistryForAccount returns the appRegistry object`() {
        val getAppRegistryResult: RemoteOperationResult<AppRegistryResponse> =
            createRemoteOperationResultMock(data = appResgitryResponse, isSuccess = true)

        val appResgitryMock = AppRegistry(accountName = OC_ACCOUNT_NAME, mimetypes = emptyList())

        every { ocAppRegistryService.getAppRegistry() } returns getAppRegistryResult

        val appRegistry = ocRemoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME)

        assertEquals(appResgitryMock, appRegistry)

        verify(exactly = 1) { ocAppRegistryService.getAppRegistry() }
    }

    @Test(expected = Exception::class)
    fun `getAppRegistryForAccount returns an Exception when the operation is not successful`() {
        val getAppRegistryResult: RemoteOperationResult<AppRegistryResponse> =
            createRemoteOperationResultMock(data = appResgitryResponse, isSuccess = false)

        every { ocAppRegistryService.getAppRegistry() } returns getAppRegistryResult

        ocRemoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME)

    }

    @Test(expected = Exception::class)
    fun `getAppRegistryForAccount returns an Exception when getAppRegistry() has an error controlled by an Exception`() {
        every { ocAppRegistryService.getAppRegistry() } throws Exception()

        ocRemoteAppRegistryDataSource.getAppRegistryForAccount(OC_ACCOUNT_NAME)
    }

    @Test
    fun `getUrlToOpenInWeb returns an url to open in website`() {
        val expectedUrl = "https://example.com/file123/TestApp"
        val getUrlToOpenInWebResult: RemoteOperationResult<String> = createRemoteOperationResultMock(data = expectedUrl, isSuccess = true)

        every {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName
            )
        } returns getUrlToOpenInWebResult

        val result = ocAppRegistryService.getUrlToOpenInWeb(
            openWebEndpoint = openWebEndpoint,
            fileId = OC_FILE.remoteId.toString(),
            appName = appName
        )

        assertEquals(expectedUrl, result.data)

        verify {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName
            )
        }
    }

    @Test(expected = Exception::class)
    fun `getUrlToOpenInWeb returns an exception when something there is an error in the method`() {

        every {
            ocAppRegistryService.getUrlToOpenInWeb(
                openWebEndpoint = openWebEndpoint,
                fileId = OC_FILE.remoteId.toString(),
                appName = appName
            )
        } throws Exception()

        ocAppRegistryService.getUrlToOpenInWeb(
            openWebEndpoint = openWebEndpoint,
            fileId = OC_FILE.remoteId.toString(),
            appName = appName
        )
    }

    @Test
    fun `createFileWithAppProvider returns the url to open in web`() {

        val expectedFileUrl = "https://example.com/files/testFile.txt"
        val createFileWithAppProviderResult: RemoteOperationResult<String> = createRemoteOperationResultMock(data = expectedFileUrl, isSuccess = true)

        every {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = openWebEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName
            )
        } returns createFileWithAppProviderResult

        val result = ocAppRegistryService.createFileWithAppProvider(
            createFileWithAppProviderEndpoint = openWebEndpoint,
            parentContainerId = OC_FILE.remoteId.toString(),
            filename = OC_FILE.fileName
        )

        assertEquals(expectedFileUrl, result.data)

        verify(exactly = 1) {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = openWebEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName
            )
        }
    }

    @Test(expected = Exception::class)
    fun `createFileWithAppProvider returns an Exception in method`() {

        every {
            ocAppRegistryService.createFileWithAppProvider(
                createFileWithAppProviderEndpoint = openWebEndpoint,
                parentContainerId = OC_FILE.remoteId.toString(),
                filename = OC_FILE.fileName
            )
        } throws Exception()

        ocAppRegistryService.createFileWithAppProvider(
            createFileWithAppProviderEndpoint = openWebEndpoint,
            parentContainerId = OC_FILE.remoteId.toString(),
            filename = OC_FILE.fileName
        )
    }
}