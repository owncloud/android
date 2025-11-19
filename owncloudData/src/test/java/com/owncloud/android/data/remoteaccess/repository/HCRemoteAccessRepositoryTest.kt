package com.owncloud.android.data.remoteaccess.repository

import com.owncloud.android.data.device.CurrentDeviceStorage
import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.data.remoteaccess.RemoteAccessTokenStorage
import com.owncloud.android.data.remoteaccess.datasources.RemoteAccessService
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDevicePathsResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDeviceResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessPath
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessPathType
import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePathType
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
    private val currentDeviceStorage: CurrentDeviceStorage = mockk(relaxed = true)

    private val repository = HCRemoteAccessRepository(
        remoteAccessService,
        tokenStorage,
        deviceVerificationClient,
        currentDeviceStorage
    )

    @Test
    fun `getAvailableDevices returns empty list when getDevices returns empty list`() = runTest {
        coEvery { remoteAccessService.getDevices() } returns emptyList()

        val result = repository.getAvailableDevices()

        assertEquals(emptyList<Device>(), result)
    }

    @Test
    fun `getAvailableDevices returns empty list when getDeviceById returns empty list`() = runTest {
        val devices = listOf(RemoteAccessDeviceResponse(seagateDeviceId = "1", friendlyName = "Test Device", "local.com", ""))
        coEvery { remoteAccessService.getDevices() } returns devices
        coEvery { remoteAccessService.getDeviceById("1") } returns RemoteAccessDevicePathsResponse("1",  paths = emptyList())

        val result = repository.getAvailableDevices()

        assertEquals(emptyList<Device>(), result)
    }

    @Test
    fun `getAvailableDevices returns device with unverified server when verifyDevice fails`() = runTest {
        val devices = listOf(RemoteAccessDeviceResponse(seagateDeviceId = "1", friendlyName = "Test Device", "local.com", ""))
        val paths = listOf(RemoteAccessPath(type = RemoteAccessPathType.REMOTE, address = "test.com", port = 443))
        coEvery { remoteAccessService.getDevices() } returns devices
        coEvery { remoteAccessService.getDeviceById("1") } returns RemoteAccessDevicePathsResponse("1", paths = paths)
        coEvery { deviceVerificationClient.verifyDevice("https://test.com:443") } returns false
        coEvery { deviceVerificationClient.getCertificateCommonName("https://test.com:443") } returns ""

        val result = repository.getAvailableDevices()

        assertEquals(1, result.size)
        assertEquals("Test Device", result[0].name)
        assertEquals(1, result[0].availablePaths.size)
        assertEquals("", result[0].certificateCommonName)
    }

    @Test
    fun `getAvailableDevices returns device with verified server as preferred`() = runTest {
        val devices = listOf(RemoteAccessDeviceResponse(seagateDeviceId = "1", friendlyName = "Test Device", "local.com", ""))
        val paths = listOf(
            RemoteAccessPath(type = RemoteAccessPathType.PUBLIC, address = "public.com", port = 443),
            RemoteAccessPath(type = RemoteAccessPathType.REMOTE, address = "test.com", port = 443)
        )
        coEvery { remoteAccessService.getDevices() } returns devices
        coEvery { remoteAccessService.getDeviceById("1") } returns RemoteAccessDevicePathsResponse("1",  paths = paths)
        coEvery { deviceVerificationClient.verifyDevice("https://public.com:443") } returns false
        coEvery { deviceVerificationClient.getCertificateCommonName("https://public.com:443") } returns ""
        coEvery { deviceVerificationClient.verifyDevice("https://test.com:443") } returns true
        coEvery { deviceVerificationClient.getCertificateCommonName("https://test.com:443") } returns "test-cert-001"

        val result = repository.getAvailableDevices()

        assertEquals(1, result.size)
        val device = result[0]
        assertEquals("Test Device", device.name)
        assertEquals(2, device.availablePaths.size)
        assertEquals("test-cert-001", device.certificateCommonName)
    }
    
    @Test
    fun `getAvailableDevices returns device with all available paths`() = runTest {
        val devices = listOf(RemoteAccessDeviceResponse(seagateDeviceId = "1", friendlyName = "Test Device", "local.com", ""))
        val paths = listOf(
            RemoteAccessPath(type = RemoteAccessPathType.LOCAL, address = "192.168.1.100", port = null),
            RemoteAccessPath(type = RemoteAccessPathType.PUBLIC, address = "public.com", port = 443),
            RemoteAccessPath(type = RemoteAccessPathType.REMOTE, address = "remote.com", port = 8080)
        )
        coEvery { remoteAccessService.getDevices() } returns devices
        coEvery { remoteAccessService.getDeviceById("1") } returns RemoteAccessDevicePathsResponse("1",  paths = paths)
        coEvery { deviceVerificationClient.verifyDevice("https://192.168.1.100") } returns true
        coEvery { deviceVerificationClient.getCertificateCommonName("https://192.168.1.100") } returns "test-cert-001"
        coEvery { deviceVerificationClient.verifyDevice("https://public.com:443") } returns true
        coEvery { deviceVerificationClient.getCertificateCommonName("https://public.com:443") } returns "test-cert-001"
        coEvery { deviceVerificationClient.verifyDevice("https://remote.com:8080") } returns true
        coEvery { deviceVerificationClient.getCertificateCommonName("https://remote.com:8080") } returns "test-cert-001"

        val result = repository.getAvailableDevices()

        assertEquals(1, result.size)
        val device = result[0]
        assertEquals("Test Device", device.name)
        assertEquals(3, device.availablePaths.size)
        assertEquals(true, device.availablePaths.containsKey(DevicePathType.LOCAL))
        assertEquals(true, device.availablePaths.containsKey(DevicePathType.PUBLIC))
        assertEquals(true, device.availablePaths.containsKey(DevicePathType.REMOTE))
        // First verified should be preferred (LOCAL in this case)
    }
}