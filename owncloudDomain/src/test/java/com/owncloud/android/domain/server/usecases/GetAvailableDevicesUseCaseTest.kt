package com.owncloud.android.domain.server.usecases

import app.cash.turbine.test
import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePath
import com.owncloud.android.domain.device.model.DevicePathType
import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAvailableDevicesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class GetAvailableDevicesUseCaseTest {

    private val mGetRemoteAvailableDevicesUseCase: GetRemoteAvailableDevicesUseCase = mockk()
    private val discoverLocalNetworkDevicesUseCase: DiscoverLocalNetworkDevicesUseCase = mockk()

    private val useCase = GetAvailableDevicesUseCase(
        mGetRemoteAvailableDevicesUseCase,
        discoverLocalNetworkDevicesUseCase
    )

    @Test
    fun `getServersUpdates should return a merged list of remote and local devices`() = runTest {
        val device1DevicePath = DevicePath("Device 1", "https://remote1.com", DevicePathType.REMOTE)
        val devices = listOf(
            Device(
                id = "id1",
                availablePaths = mapOf(DevicePathType.REMOTE to device1DevicePath),
                preferredPath = device1DevicePath
            )
        )
        
        val device2DevicePath = DevicePath("Device 2", "https://remote2.com", DevicePathType.PUBLIC)
        val devices2 = listOf(
            Device(
                id = "id1",
                availablePaths = mapOf(DevicePathType.REMOTE to device1DevicePath),
                preferredPath = device1DevicePath
            ),
            Device(
                id = "id2",
                availablePaths = mapOf(DevicePathType.PUBLIC to device2DevicePath),
                preferredPath = device2DevicePath
            )
        )
        coEvery { mGetRemoteAvailableDevicesUseCase.execute() }.returnsMany(devices, devices2)

        val localDevicePath = DevicePath(hostName = "https://local.com", hostUrl = "https://local.com", DevicePathType.LOCAL)
        val localDevice = Device(
            id = "local-id",
            availablePaths = mapOf(DevicePathType.LOCAL to localDevicePath),
            preferredPath = localDevicePath
        )
        val localDevicesFlow = flowOf(localDevice)
        val params = DiscoverLocalNetworkDevicesUseCase.Params("serviceType", "serviceName", 10.seconds)
        coEvery { discoverLocalNetworkDevicesUseCase.execute(any()) } returns localDevicesFlow

        val job = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + job)

        useCase.getServersUpdates(scope, params).test {
            useCase.refreshRemoteAccessDevices()
            
            awaitItem()
            awaitItem()
            awaitItem()
            
            useCase.refreshRemoteAccessDevices()
            val finalEmission = awaitItem()

            assertEquals(3, finalEmission.size)
            assertEquals("Device 1", finalEmission[0].name)
            assertEquals("Device 2", finalEmission[1].name)
            assertEquals("https://local.com", finalEmission[2].name)

            cancelAndConsumeRemainingEvents()
        }

        job.cancel()
    }

    @Test
    fun `getServersUpdates should merge local server into existing device by certificate`() = runTest {
        val remoteDevicePath1 = DevicePath("Device 1", "https://remote1.com", DevicePathType.REMOTE)
        val remoteDevicePath2 = DevicePath("Device 2", "https://remote2.com", DevicePathType.PUBLIC)
        val devices = listOf(
            Device(
                id = "id1",
                availablePaths = mapOf(DevicePathType.REMOTE to remoteDevicePath1),
                preferredPath = remoteDevicePath1,
                certificateCommonName = "cert-001"
            ),
            Device(
                id = "id2",
                availablePaths = mapOf(DevicePathType.PUBLIC to remoteDevicePath2),
                preferredPath = remoteDevicePath2,
                certificateCommonName = "cert-002"
            )
        )
        coEvery { mGetRemoteAvailableDevicesUseCase.execute() }.returns(devices)

        val localDevicePath = DevicePath(
            hostName = "https://local.com", 
            hostUrl = "https://local.com",
            devicePathType = DevicePathType.LOCAL
        )
        val localDevice = Device(
            id = "local-id",
            availablePaths = mapOf(DevicePathType.LOCAL to localDevicePath),
            preferredPath = localDevicePath,
            certificateCommonName = "cert-001"
        )
        val localDevicesFlow = flowOf(localDevice)
        val params = DiscoverLocalNetworkDevicesUseCase.Params("serviceType", "serviceName", 10.seconds)
        coEvery { discoverLocalNetworkDevicesUseCase.execute(any()) } returns localDevicesFlow

        val job = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + job)

        useCase.getServersUpdates(scope, params).test {
            useCase.refreshRemoteAccessDevices()
            
            awaitItem()
            awaitItem()
            val finalEmission = awaitItem()

            assertEquals(2, finalEmission.size)
            assertEquals("https://local.com", finalEmission[0].name)
            assertEquals(2, finalEmission[0].availablePaths.size) // Now has both REMOTE and LOCAL
            assertEquals(true, finalEmission[0].availablePaths.containsKey(DevicePathType.REMOTE))
            assertEquals(true, finalEmission[0].availablePaths.containsKey(DevicePathType.LOCAL))
            assertEquals(DevicePathType.LOCAL, finalEmission[0].preferredPath.devicePathType)
            
            assertEquals("Device 2", finalEmission[1].name)
            assertEquals(true, finalEmission[1].availablePaths.containsKey(DevicePathType.PUBLIC))
            assertEquals(1, finalEmission[1].availablePaths.size) // Only has PUBLIC

            cancelAndConsumeRemainingEvents()
        }

        job.cancel()
    }

    @Test
    fun `getServersUpdates should create separate device for local server with different certificate`() = runTest {
        val remoteDevicePath1 = DevicePath("Device 1", "https://remote1.com", DevicePathType.REMOTE)
        val remoteDevicePath2 = DevicePath("Device 2", "https://remote2.com", DevicePathType.PUBLIC)
        val devices = listOf(
            Device(
                id = "id1",
                availablePaths = mapOf(DevicePathType.REMOTE to remoteDevicePath1),
                preferredPath = remoteDevicePath1,
                certificateCommonName = "cert-001"
            ),
            Device(
                id = "id2",
                availablePaths = mapOf(DevicePathType.PUBLIC to remoteDevicePath2),
                preferredPath = remoteDevicePath2,
                certificateCommonName = "cert-002"
            )
        )
        coEvery { mGetRemoteAvailableDevicesUseCase.execute() }.returns(devices)

        val localDevicePath = DevicePath(
            hostName = "https://local.com", 
            hostUrl = "https://local.com",
            devicePathType = DevicePathType.LOCAL
        )
        val localDevice = Device(
            id = "local-id",
            availablePaths = mapOf(DevicePathType.LOCAL to localDevicePath),
            preferredPath = localDevicePath,
            certificateCommonName = "cert-003"
        )
        val localDevicesFlow = flowOf(localDevice)
        val params = DiscoverLocalNetworkDevicesUseCase.Params("serviceType", "serviceName", 10.seconds)
        coEvery { discoverLocalNetworkDevicesUseCase.execute(any()) } returns localDevicesFlow

        val job = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + job)

        useCase.getServersUpdates(scope, params).test {
            useCase.refreshRemoteAccessDevices()

            awaitItem()
            awaitItem()
            val finalEmission = awaitItem()

            assertEquals(3, finalEmission.size)
            assertEquals("Device 1", finalEmission[0].name)
            assertEquals("Device 2", finalEmission[1].name)
            assertEquals("https://local.com", finalEmission[2].name)
            assertEquals(DevicePathType.LOCAL, finalEmission[2].preferredPath.devicePathType)

            cancelAndConsumeRemainingEvents()
        }

        job.cancel()
    }

    @Test
    fun `getServersUpdates should create separate device for local server with empty certificate`() = runTest {
        val remoteDevicePath1 = DevicePath("Device 1", "https://remote1.com", DevicePathType.REMOTE)
        val remoteDevicePath2 = DevicePath("Device 2", "https://remote2.com", DevicePathType.PUBLIC)
        val devices = listOf(
            Device(
                id = "id1",
                availablePaths = mapOf(DevicePathType.REMOTE to remoteDevicePath1),
                preferredPath = remoteDevicePath1,
                certificateCommonName = "cert1"
            ),
            Device(
                id = "id2",
                availablePaths = mapOf(DevicePathType.PUBLIC to remoteDevicePath2),
                preferredPath = remoteDevicePath2,
                certificateCommonName = "cert2"
            )
        )
        coEvery { mGetRemoteAvailableDevicesUseCase.execute() }.returns(devices)

        val localDevicePath = DevicePath(
            hostName = "https://local.com", 
            hostUrl = "https://local.com",
            devicePathType = DevicePathType.LOCAL
        )
        val localDevice = Device(
            id = "local-id",
            availablePaths = mapOf(DevicePathType.LOCAL to localDevicePath),
            preferredPath = localDevicePath,
            certificateCommonName = "cert3"
        )
        val localDevicesFlow = flowOf(localDevice)
        val params = DiscoverLocalNetworkDevicesUseCase.Params("serviceType", "serviceName", 10.seconds)
        coEvery { discoverLocalNetworkDevicesUseCase.execute(any()) } returns localDevicesFlow

        val job = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + job)

        useCase.getServersUpdates(scope, params).test {
            useCase.refreshRemoteAccessDevices()
            
            awaitItem()
            awaitItem()
            val finalEmission = awaitItem()

            println(finalEmission)

            assertEquals(3, finalEmission.size)
            assertEquals("Device 1", finalEmission[0].name)
            assertEquals("Device 2", finalEmission[1].name)
            assertEquals("https://local.com", finalEmission[2].name)

            cancelAndConsumeRemainingEvents()
        }

        job.cancel()
    }
}