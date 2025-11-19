package com.owncloud.android.data.device

import android.accounts.Account
import android.accounts.AccountManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalCoroutinesApi
class DynamicBaseUrlSwitcherTest {

    private val accountManager: AccountManager = mockk(relaxed = true)
    private val baseUrlChooser: BaseUrlChooser = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val switcher = DynamicBaseUrlSwitcher(
        accountManager = accountManager,
        baseUrlChooser = baseUrlChooser,
        coroutineScope = testScope
    )

    private val testAccount: Account = mockk(relaxed = true)// Account("test@example.com", "owncloud")

    @Test
    fun `startDynamicUrlSwitching starts observing base URL changes`() = runTest(testDispatcher) {
        val baseUrlFlow = MutableStateFlow("https://192.168.1.100/files")
        every { baseUrlChooser.observeAvailableBaseUrl() } returns baseUrlFlow
        every { accountManager.getUserData(testAccount, AccountUtils.Constants.KEY_OC_BASE_URL) } returns null

        switcher.startDynamicUrlSwitching(testAccount)
        advanceUntilIdle()

        assertTrue(switcher.isActive())
        verify { accountManager.setUserData(testAccount, AccountUtils.Constants.KEY_OC_BASE_URL, "https://192.168.1.100/files") }
    }

    @Test
    fun `startDynamicUrlSwitching does not update when URL is unchanged`() = runTest(testDispatcher) {
        val baseUrl = "https://192.168.1.100/files"
        val baseUrlFlow = flowOf(baseUrl, baseUrl, baseUrl)
        every { baseUrlChooser.observeAvailableBaseUrl() } returns baseUrlFlow
        every { accountManager.getUserData(testAccount, AccountUtils.Constants.KEY_OC_BASE_URL) } returns baseUrl

        switcher.startDynamicUrlSwitching(testAccount)
        advanceUntilIdle()

        // Should not update since URL hasn't changed
        verify(exactly = 0) { 
            accountManager.setUserData(any(), any(), any()) 
        }
    }

    @Test
    fun `stopDynamicUrlSwitching stops observing and clears state`() = runTest(testDispatcher) {
        val baseUrlFlow = MutableStateFlow("https://192.168.1.100/files")
        every { baseUrlChooser.observeAvailableBaseUrl() } returns baseUrlFlow
        every { accountManager.getUserData(testAccount, AccountUtils.Constants.KEY_OC_BASE_URL) } returns null

        switcher.startDynamicUrlSwitching(testAccount)
        advanceUntilIdle()
        
        assertTrue(switcher.isActive())
        
        switcher.stopDynamicUrlSwitching()
        
        assertFalse(switcher.isActive())
    }

    @Test
    fun `startDynamicUrlSwitching cancels previous observation`() = runTest(testDispatcher) {
        val baseUrlFlow1 = flowOf("https://192.168.1.100/files")
        val baseUrlFlow2 = flowOf("https://public.example.com/files")
        
        every { baseUrlChooser.observeAvailableBaseUrl() } returns baseUrlFlow1 andThen baseUrlFlow2
        every { accountManager.getUserData(any(), any()) } returns null

        val account1 = Account("user1@example.com", "owncloud")
        val account2 = Account("user2@example.com", "owncloud")

        switcher.startDynamicUrlSwitching(account1)
        advanceUntilIdle()
        

        // Start with a different account - should cancel previous
        switcher.startDynamicUrlSwitching(account2)
        advanceUntilIdle()
    }

    @Test
    fun `isActive returns false when not started`() {
        assertFalse(switcher.isActive())
    }

    @Test
    fun `dispose cleans up resources`() = runTest(testDispatcher) {
        val baseUrlFlow = MutableStateFlow("https://192.168.1.100/files")
        every { baseUrlChooser.observeAvailableBaseUrl() } returns baseUrlFlow
        every { accountManager.getUserData(testAccount, AccountUtils.Constants.KEY_OC_BASE_URL) } returns null

        switcher.startDynamicUrlSwitching(testAccount)
        advanceUntilIdle()
        
        assertTrue(switcher.isActive())
        
        switcher.dispose()
        
        assertFalse(switcher.isActive())
    }

    @Test
    fun `handleBaseUrlChange handles AccountManager exceptions gracefully`() = runTest(testDispatcher) {
        val baseUrlFlow = MutableStateFlow("https://192.168.1.100/files")
        every { baseUrlChooser.observeAvailableBaseUrl() } returns baseUrlFlow
        every { accountManager.getUserData(testAccount, AccountUtils.Constants.KEY_OC_BASE_URL) } returns null
        every { 
            accountManager.setUserData(testAccount, AccountUtils.Constants.KEY_OC_BASE_URL, any()) 
        } throws SecurityException("Permission denied")

        switcher.startDynamicUrlSwitching(testAccount)
        advanceUntilIdle()

        // Should not crash, just log the error
        assertTrue(switcher.isActive())
    }
}

