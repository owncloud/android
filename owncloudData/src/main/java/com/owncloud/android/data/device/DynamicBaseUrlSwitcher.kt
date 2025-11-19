package com.owncloud.android.data.device

import android.accounts.Account
import android.accounts.AccountManager
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages dynamic base URL switching for an account based on network conditions.
 * 
 * This class:
 * - Observes available base URLs from BaseUrlChooser
 * - Automatically updates the account's KEY_OC_BASE_URL when a better URL becomes available
 * - Has its own coroutine scope for lifecycle management
 * 
 * Usage:
 * ```
 * // On login
 * dynamicBaseUrlSwitcher.startDynamicUrlSwitching(account)
 * 
 * // On logout
 * dynamicBaseUrlSwitcher.stopDynamicUrlSwitching()
 * ```
 */
class DynamicBaseUrlSwitcher(
    private val accountManager: AccountManager,
    private val baseUrlChooser: BaseUrlChooser,
    private val coroutineScope: CoroutineScope,
) {

    private var observationJob: Job? = null
    private var currentAccount: Account? = null

    /**
     * Start observing and dynamically switching base URLs for the given account.
     * 
     * This will:
     * 1. Cancel any previous observation
     * 2. Start observing base URL changes from BaseUrlChooser
     * 3. Update the account's KEY_OC_BASE_URL whenever a new URL becomes available
     * 
     * @param account The account to manage
     */
    fun startDynamicUrlSwitching(account: Account) {
        stopDynamicUrlSwitching()
        
        currentAccount = account
        
        Timber.d("Starting dynamic URL switching for account: ${account.name}")
        
        observationJob = coroutineScope.launch {
            baseUrlChooser.observeAvailableBaseUrl()
                .catch { error ->
                    Timber.e(error, "Error observing base URL changes")
                }
                .collect { newBaseUrl ->
                    handleBaseUrlChange(account, newBaseUrl)
                }
        }
    }

    /**
     * Stop observing and cancel dynamic URL switching.
     * 
     * This should be called when:
     * - User logs out
     * - Account is removed
     * - App is shutting down
     */
    fun stopDynamicUrlSwitching() {
        observationJob?.cancel()
        observationJob = null
        
        currentAccount?.let {
            Timber.d("Stopped dynamic URL switching for account: ${it.name}")
        }
        
        currentAccount = null
    }

    /**
     * Check if dynamic URL switching is currently active.
     * 
     * @return true if observing URL changes, false otherwise
     */
    fun isActive(): Boolean {
        return observationJob?.isActive == true
    }

    /**
     * Handle base URL changes by updating the account's KEY_OC_BASE_URL.
     * 
     * @param account The account to update
     * @param newBaseUrl The new base URL, or null if no URL is available
     */
    private fun handleBaseUrlChange(account: Account, newBaseUrl: String?) {
        val currentBaseUrl = accountManager.getUserData(
            account,
            AccountUtils.Constants.KEY_OC_BASE_URL
        )

        when (newBaseUrl) {
            null -> {
                Timber.w("No base URL available for account: ${account.name}")
                // Don't update - keep the last known URL
            }
            currentBaseUrl -> {
                Timber.d("Base URL unchanged: $currentBaseUrl")
                // No change needed
            }
            else -> {
                Timber.i("Updating base URL for ${account.name}: $currentBaseUrl -> $newBaseUrl")
                updateAccountBaseUrl(account, newBaseUrl)
            }
        }
    }

    /**
     * Update the account's base URL in AccountManager.
     * 
     * @param account The account to update
     * @param newBaseUrl The new base URL
     */
    private fun updateAccountBaseUrl(account: Account, newBaseUrl: String) {
        try {
            accountManager.setUserData(
                account,
                AccountUtils.Constants.KEY_OC_BASE_URL,
                newBaseUrl
            )
            SingleSessionManager.getDefaultSingleton().cancelAllRequests()
            Timber.d("Successfully updated base URL to: $newBaseUrl")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update base URL for account: ${account.name}")
        }
    }

    /**
     * Cancel all ongoing operations and clean up resources.
     * Should be called when the switcher is no longer needed.
     */
    fun dispose() {
        stopDynamicUrlSwitching()
        coroutineScope.cancel()
    }
}

