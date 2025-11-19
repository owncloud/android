package com.owncloud.android.data.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Observes the application lifecycle to track foreground/background state.
 * 
 * This class uses ProcessLifecycleOwner to detect when the entire application
 * goes to background or comes to foreground.
 * 
 * Usage:
 * ```
 * // Observe state changes
 * appLifecycleObserver.appState.collect { state ->
 *     when (state) {
 *         AppState.FOREGROUND -> // App is visible
 *         AppState.BACKGROUND -> // App is in background
 *     }
 * }
 * 
 * // Get current state
 * val isInForeground = appLifecycleObserver.isInForeground()
 * ```
 */
class AppLifecycleObserver(
    processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) : DefaultLifecycleObserver {

    private val _appState = MutableStateFlow(AppState.FOREGROUND)
    
    /**
     * StateFlow emitting current application state.
     * Initial state is FOREGROUND.
     */
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        // Register this observer with ProcessLifecycleOwner
        processLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("App moved to FOREGROUND")
        _appState.value = AppState.FOREGROUND
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.d("App moved to BACKGROUND")
        _appState.value = AppState.BACKGROUND
    }

    /**
     * Check if app is currently in foreground.
     * 
     * @return true if app is in foreground, false if in background
     */
    fun isInForeground(): Boolean {
        return _appState.value == AppState.FOREGROUND
    }

    /**
     * Check if app is currently in background.
     * 
     * @return true if app is in background, false if in foreground
     */
    fun isInBackground(): Boolean {
        return _appState.value == AppState.BACKGROUND
    }

    /**
     * Get current app state.
     * 
     * @return Current AppState
     */
    fun getCurrentState(): AppState {
        return _appState.value
    }

    /**
     * Cleanup and remove lifecycle observer.
     * Should be called when observer is no longer needed.
     */
    fun dispose() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        Timber.d("AppLifecycleObserver disposed")
    }
}

/**
 * Represents the state of the application.
 */
enum class AppState {
    /**
     * Application is visible to the user (one or more activities are visible).
     */
    FOREGROUND,
    
    /**
     * Application is not visible to the user (all activities are stopped).
     */
    BACKGROUND
}


