package com.owncloud.android.domain.server.usecases

import app.cash.turbine.test
import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAvailableServersUseCase
import com.owncloud.android.domain.server.model.Server
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
class GetAvailableServersUseCaseTest {

    private val getRemoteAvailableServersUseCase: GetRemoteAvailableServersUseCase = mockk()
    private val discoverLocalNetworkDevicesUseCase: DiscoverLocalNetworkDevicesUseCase = mockk()

    private val useCase = GetAvailableServersUseCase(
        getRemoteAvailableServersUseCase,
        discoverLocalNetworkDevicesUseCase
    )

    @Test
    fun `getServersUpdates should return a merged list of remote and local servers`() = runTest {
        // --- Mocks Setup ---
        val servers = listOf(
            Server("Remote Server 1", "https://remote1.com"),
        )
        val servers2 = listOf(
            Server("Remote Server 1", "https://remote1.com"),
            Server("Remote Server 2", "https://remote2.com")
        )
        coEvery { getRemoteAvailableServersUseCase.execute() }.returnsMany(servers, servers2)

        val localDevicesFlow = flowOf("https://local.com")
        val params = DiscoverLocalNetworkDevicesUseCase.Params("serviceType", "serviceName", 10.seconds)
        coEvery { discoverLocalNetworkDevicesUseCase.execute(any()) } returns localDevicesFlow

        // --- Scope Setup: This is the crucial part to fix the deadlock ---
        // Create a job that we can control manually.
        val job = SupervisorJob()
        // Create a child scope that inherits the TestDispatcher but uses our new job.
        val scope = CoroutineScope(coroutineContext + job)

        // --- Test Execution ---
        useCase.getServersUpdates(scope, params).test {
            useCase.refreshRemoteAccessDevices()
            
            // Consume initial, remote, and combined emissions
            awaitItem()
            awaitItem()
            awaitItem()
            
            useCase.refreshRemoteAccessDevices()
            val finalEmission = awaitItem()

            val expectedServers = listOf(
                Server(hostName = "Remote Server 1", hostUrl = "https://remote1.com"),
                Server(hostName = "Remote Server 2", hostUrl = "https://remote2.com"),
                Server(hostName = "https://local.com", hostUrl = "https://local.com")
            )

            assertEquals(expectedServers, finalEmission)

            // Cleanly finish the Turbine test
            cancelAndConsumeRemainingEvents()
        }

        // --- Cleanup ---
        // By cancelling the job, we immediately terminate the coroutines launched by `stateIn`
        // in the use case, allowing `runTest` to complete without a timeout.
        job.cancel()
    }
}