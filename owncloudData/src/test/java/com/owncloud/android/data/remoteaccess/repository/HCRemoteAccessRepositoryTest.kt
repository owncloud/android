package com.owncloud.android.data.remoteaccess.repository

import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDevicePathsResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDeviceResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessPath
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessPathType
import com.owncloud.android.domain.server.model.Server
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class HCRemoteAccessRepositoryTest {

    private val remoteAccessService: RemoteAccessService = mockk()
    private val tokenStorage: RemoteAccessTokenStorage = mockk()

    private val deviceVerificationClient: HCDeviceVerificationClient = mockk()

    private val repository = HCRemoteAccessRepository(
        remoteAccessService,
        tokenStorage,
        deviceVerificationClient
    )

    @Test
    fun `getAvailableServers returns empty list when getDevices returns empty list`() = runTest {
        coEvery { remoteAccessService.getDevices() } returns emptyList()

        val result = repository.getAvailableServers()

        assertEquals(emptyList<Server>(), result)
    }

    @Test
    fun `getAvailableServers returns empty list when getDeviceById returns empty list`() = runTest {
        val devices = listOf(RemoteAccessDeviceResponse(seagateDeviceId = "1", friendlyName = "Test Device", "local.com", ""))
        coEvery { remoteAccessService.getDevices() } returns devices
        coEvery { remoteAccessService.getDeviceById("1") } returns RemoteAccessDevicePathsResponse("1",  paths = emptyList())

        val result = repository.getAvailableServers()

        assertEquals(emptyList<Server>(), result)
    }

    @Test
    fun `getAvailableServers returns empty list when verifyDevice fails for all baseUrls`() = runTest {
        val devices = listOf(RemoteAccessDeviceResponse(seagateDeviceId = "1", friendlyName = "Test Device", "local.com", ""))
        val paths = listOf(RemoteAccessPath(type = RemoteAccessPathType.REMOTE, address = "test.com", port = 443))
        coEvery { remoteAccessService.getDevices() } returns devices
        coEvery { remoteAccessService.getDeviceById("1") } returns RemoteAccessDevicePathsResponse("1", paths = paths)
        coEvery { deviceVerificationClient.verifyDevice("https://test.com:443") } returns false

        val result = repository.getAvailableServers()

        assertEquals(emptyList<Server>(), result)
    }

    @Test
    fun `getAvailableServers returns a list of servers on positive scenario`() = runTest {
        val devices = listOf(RemoteAccessDeviceResponse(seagateDeviceId = "1", friendlyName = "Test Device", "local.com", ""))
        val paths = listOf(RemoteAccessPath(type = RemoteAccessPathType.REMOTE, address = "test.com", port = 443))
        coEvery { remoteAccessService.getDevices() } returns devices
        coEvery { remoteAccessService.getDeviceById("1") } returns RemoteAccessDevicePathsResponse("1",  paths = paths)
        coEvery { deviceVerificationClient.verifyDevice("https://test.com:443") } returns true
        coEvery { deviceVerificationClient.getCertificateCommonName("https://test.com:443") } returns "test-cert-001"

        val result = repository.getAvailableServers()

        val expectedServer = Server(hostName = "Test Device", hostUrl = "https://test.com:443/files", certificateCommonName = "test-cert-001")
        assertEquals(listOf(expectedServer), result)
    }
}