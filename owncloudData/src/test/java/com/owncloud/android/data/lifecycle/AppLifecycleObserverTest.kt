package com.owncloud.android.data.lifecycle

import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AppLifecycleObserverTest {

    private val lifecycleOwner: LifecycleOwner = mockk(relaxed = true) {
        every { lifecycle } returns mockk(relaxed = true)
    }
    private lateinit var observer: AppLifecycleObserver

    @Before
    fun setup() {
        observer = AppLifecycleObserver(lifecycleOwner)
    }

    @Test
    fun `initial state should be FOREGROUND`() {
        assertEquals(AppState.FOREGROUND, observer.getCurrentState())
        assertTrue(observer.isInForeground())
        assertFalse(observer.isInBackground())
    }

    @Test
    fun `onStart should change state to FOREGROUND`() = runTest {
        observer.appState.test {
            // Initial state
            assertEquals(AppState.FOREGROUND, awaitItem())
            
            // Simulate app going to background first
            observer.onStop(lifecycleOwner)
            assertEquals(AppState.BACKGROUND, awaitItem())
            
            // Now simulate app coming to foreground
            observer.onStart(lifecycleOwner)
            assertEquals(AppState.FOREGROUND, awaitItem())
            
            assertTrue(observer.isInForeground())
            assertFalse(observer.isInBackground())
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onStop should change state to BACKGROUND`() = runTest {
        observer.appState.test {
            // Initial state
            assertEquals(AppState.FOREGROUND, awaitItem())
            
            // Simulate app going to background
            observer.onStop(lifecycleOwner)
            assertEquals(AppState.BACKGROUND, awaitItem())
            
            assertFalse(observer.isInForeground())
            assertTrue(observer.isInBackground())
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple lifecycle transitions should emit correct states`() = runTest {
        observer.appState.test {
            // Initial
            assertEquals(AppState.FOREGROUND, awaitItem())
            
            // Foreground -> Background
            observer.onStop(lifecycleOwner)
            assertEquals(AppState.BACKGROUND, awaitItem())
            
            // Background -> Foreground
            observer.onStart(lifecycleOwner)
            assertEquals(AppState.FOREGROUND, awaitItem())
            
            // Foreground -> Background again
            observer.onStop(lifecycleOwner)
            assertEquals(AppState.BACKGROUND, awaitItem())
            
            // Background -> Foreground again
            observer.onStart(lifecycleOwner)
            assertEquals(AppState.FOREGROUND, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCurrentState returns current app state`() {
        assertEquals(AppState.FOREGROUND, observer.getCurrentState())
        
        observer.onStop(lifecycleOwner)
        assertEquals(AppState.BACKGROUND, observer.getCurrentState())
        
        observer.onStart(lifecycleOwner)
        assertEquals(AppState.FOREGROUND, observer.getCurrentState())
    }

    @Test
    fun `isInForeground returns correct value`() {
        assertTrue(observer.isInForeground())
        
        observer.onStop(lifecycleOwner)
        assertFalse(observer.isInForeground())
        
        observer.onStart(lifecycleOwner)
        assertTrue(observer.isInForeground())
    }

    @Test
    fun `isInBackground returns correct value`() {
        assertFalse(observer.isInBackground())
        
        observer.onStop(lifecycleOwner)
        assertTrue(observer.isInBackground())
        
        observer.onStart(lifecycleOwner)
        assertFalse(observer.isInBackground())
    }

    @Test
    fun `appState flow emits distinct states`() = runTest {
        observer.appState.test {
            assertEquals(AppState.FOREGROUND, awaitItem())
            
            // Multiple onStart calls should not emit multiple times
            observer.onStart(lifecycleOwner)
            observer.onStart(lifecycleOwner)
            
            // Go to background
            observer.onStop(lifecycleOwner)
            assertEquals(AppState.BACKGROUND, awaitItem())
            
            // Multiple onStop calls should not emit multiple times
            observer.onStop(lifecycleOwner)
            observer.onStop(lifecycleOwner)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}

