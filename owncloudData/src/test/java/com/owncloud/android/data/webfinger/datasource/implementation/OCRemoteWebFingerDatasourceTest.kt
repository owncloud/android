package com.owncloud.android.data.webfinger.datasource.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.webfinger.datasources.implementation.OCRemoteWebFingerDatasource
import com.owncloud.android.domain.webfinger.model.WebFingerRel
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.webfinger.services.implementation.OCWebFingerService
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT_ID
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteWebFingerDatasourceTest {

    private lateinit var ocRemoteWebFingerDatasource: OCRemoteWebFingerDatasource

    private val clientManager: ClientManager = mockk(relaxed = true)
    private val ownCloudClient: OwnCloudClient = mockk(relaxed = true)
    private val ocWebFingerService: OCWebFingerService = mockk()
    private val listString: List<String> = mockk(relaxed = true)

    @Before
    fun init() {
        ocRemoteWebFingerDatasource = OCRemoteWebFingerDatasource(
            ocWebFingerService,
            clientManager,
        )

        every { clientManager.getClientForAnonymousCredentials(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false) } returns ownCloudClient

    }

    @Test
    fun `getInstancesFromWebFinger returns a list of web finger`() {

        val getInstancesFromWebFingerResult: RemoteOperationResult<List<String>> =
            createRemoteOperationResultMock(data = listString, isSuccess = true)

        every {
            ocWebFingerService.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY.uri,
                ownCloudClient,
            )
        } returns getInstancesFromWebFingerResult

        val actualResult = ocRemoteWebFingerDatasource.getInstancesFromWebFinger(
            lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
            resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
        )

        assertEquals(getInstancesFromWebFingerResult.data, actualResult)

        verify(exactly = 1) {
            clientManager.getClientForAnonymousCredentials(any(), false)
            ocWebFingerService.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY.uri,
                ownCloudClient,
            )
        }
    }

    @Test(expected = Exception::class)
    fun `getInstancesFromWebFinger returns an exception when service receive an exception`() {

        every {
            ocWebFingerService.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY.uri,
                any(),
            )
        } throws Exception()

        ocRemoteWebFingerDatasource.getInstancesFromWebFinger(
            lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
            resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
        )
    }

    @Test
    fun `getInstancesFromAuthenticatedWebFinger returns a list of web finger`() {

        val getInstancesFromAuthenticatedWebFingerResult: RemoteOperationResult<List<String>> =
            createRemoteOperationResultMock(data = listString, isSuccess = true)

        every {
            ocWebFingerService.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY.uri,
                ownCloudClient,
            )
        } returns getInstancesFromAuthenticatedWebFingerResult

        val actualResult = ocRemoteWebFingerDatasource.getInstancesFromAuthenticatedWebFinger(
            lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
            resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            username = OC_ACCOUNT_ID,
            accessToken = OC_ACCESS_TOKEN
        )

        assertEquals(getInstancesFromAuthenticatedWebFingerResult.data, actualResult)

        verify(exactly = 1) {
            clientManager.getClientForAnonymousCredentials(any(), false)
            ocWebFingerService.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY.uri,
                any(),
            )
        }
    }

    @Test(expected = Exception::class)
    fun `getInstancesFromAuthenticatedWebFinger returns an exception when service receive an exception`() {

        every {
            ocWebFingerService.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY.uri,
                ownCloudClient,
            )
        } throws Exception()

        ocRemoteWebFingerDatasource.getInstancesFromAuthenticatedWebFinger(
            lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
            resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            username = OC_ACCOUNT_ID,
            accessToken = OC_ACCESS_TOKEN
        )

    }
}