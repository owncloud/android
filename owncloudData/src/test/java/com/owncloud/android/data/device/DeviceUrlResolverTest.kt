package com.owncloud.android.data.device

import com.owncloud.android.data.mdnsdiscovery.HCDeviceVerificationClient
import com.owncloud.android.domain.device.model.DevicePathType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@ExperimentalCoroutinesApi
class DeviceUrlResolverTest {

    private val deviceVerificationClient: HCDeviceVerificationClient = mockk()

    private val resolver = HCDeviceUrlResolver(
        deviceVerificationClient
    )

    @Test
    fun `resolveAvailableBaseUrl returns LOCAL url when available and verified`() = runTest {
        val devicePaths = mapOf(
            DevicePathType.LOCAL to "https://192.168.1.100/files"
        )
        coEvery { deviceVerificationClient.verifyDevice("https://192.168.1.100") } returns true

        val result = resolver.resolveAvailableBaseUrl(devicePaths)

        assertEquals("https://192.168.1.100/files", result)
        coVerify { deviceVerificationClient.verifyDevice("https://192.168.1.100") }
    }

    @Test
    fun `resolveAvailableBaseUrl returns PUBLIC url when LOCAL is not verified`() = runTest {
        val devicePaths = mapOf(
            DevicePathType.LOCAL to "https://192.168.1.100/files",
            DevicePathType.PUBLIC to "https://public.example.com/files"
        )
        coEvery { deviceVerificationClient.verifyDevice("https://192.168.1.100") } returns false
        coEvery { deviceVerificationClient.verifyDevice("https://public.example.com") } returns true

        val result = resolver.resolveAvailableBaseUrl(devicePaths)

        assertEquals("https://public.example.com/files", result)
        coVerify(exactly = 1) { deviceVerificationClient.verifyDevice("https://192.168.1.100") }
        coVerify(exactly = 1) { deviceVerificationClient.verifyDevice("https://public.example.com") }
    }

    @Test
    fun `resolveAvailableBaseUrl returns REMOTE url when LOCAL and PUBLIC are not verified`() = runTest {
        val devicePaths = mapOf(
            DevicePathType.LOCAL to "https://192.168.1.100/files",
            DevicePathType.PUBLIC to "https://public.example.com/files",
            DevicePathType.REMOTE to "https://remote.example.com/files"
        )
        coEvery { deviceVerificationClient.verifyDevice("https://192.168.1.100") } returns false
        coEvery { deviceVerificationClient.verifyDevice("https://public.example.com") } returns false
        coEvery { deviceVerificationClient.verifyDevice("https://remote.example.com") } returns true

        val result = resolver.resolveAvailableBaseUrl(devicePaths)

        assertEquals("https://remote.example.com/files", result)
        coVerify(exactly = 1) { deviceVerificationClient.verifyDevice("https://192.168.1.100") }
        coVerify(exactly = 1) { deviceVerificationClient.verifyDevice("https://public.example.com") }
        coVerify(exactly = 1) { deviceVerificationClient.verifyDevice("https://remote.example.com") }
    }

    @Test
    fun `resolveAvailableBaseUrl returns null when all URLs are not verified`() = runTest {
        val devicePaths = mapOf(
            DevicePathType.LOCAL to "https://192.168.1.100/files",
            DevicePathType.PUBLIC to "https://public.example.com/files",
            DevicePathType.REMOTE to "https://remote.example.com/files"
        )
        coEvery { deviceVerificationClient.verifyDevice(any()) } returns false

        val result = resolver.resolveAvailableBaseUrl(devicePaths)

        assertNull(result)
    }

    @Test
    fun `resolveAvailableBaseUrl returns null when empty list provided`() = runTest {
        val devicePaths = mapOf<DevicePathType, String>()

        val result = resolver.resolveAvailableBaseUrl(devicePaths)

        assertNull(result)
        coVerify(exactly = 0) { deviceVerificationClient.verifyDevice(any()) }
    }

    @Test
    fun `resolveAvailableBaseUrl removes files suffix for verification`() = runTest {
        val devicePaths = mapOf(
            DevicePathType.LOCAL to "https://192.168.1.100/files"
        )
        coEvery { deviceVerificationClient.verifyDevice("https://192.168.1.100") } returns true

        val result = resolver.resolveAvailableBaseUrl(devicePaths)

        assertEquals("https://192.168.1.100/files", result)
        // Should verify without /files suffix
        coVerify { deviceVerificationClient.verifyDevice("https://192.168.1.100") }
        coVerify(exactly = 0) { deviceVerificationClient.verifyDevice("https://192.168.1.100/files") }
    }
}

