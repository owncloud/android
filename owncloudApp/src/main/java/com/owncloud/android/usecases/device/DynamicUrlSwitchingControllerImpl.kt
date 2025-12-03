package com.owncloud.android.usecases.device

import android.content.Context
import com.owncloud.android.data.device.DynamicBaseUrlSwitcher
import com.owncloud.android.data.lifecycle.AppLifecycleObserver
import com.owncloud.android.data.lifecycle.AppState
import com.owncloud.android.domain.device.usecases.DynamicUrlSwitchingController
import com.owncloud.android.presentation.authentication.AccountUtils.getCurrentOwnCloudAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DynamicUrlSwitchingControllerImpl(
    private val appContext: Context,
    private val dynamicBaseUrlSwitcher: DynamicBaseUrlSwitcher,
    private val coroutineScope: CoroutineScope,
    private val appLifecycleObserver: AppLifecycleObserver,
) : DynamicUrlSwitchingController {

    override fun initDynamicUrlSwitching() {
        coroutineScope.launch {
            appLifecycleObserver.appState.collect {
                when (it) {
                    AppState.FOREGROUND -> startDynamicUrlSwitching()
                    AppState.BACKGROUND -> stopDynamicUrlSwitching()
                }
            }
        }
    }

    override fun startDynamicUrlSwitching() {
        val account = getCurrentOwnCloudAccount(appContext)
        account?.let {
            dynamicBaseUrlSwitcher.startDynamicUrlSwitching(it)
        }
    }

    override fun stopDynamicUrlSwitching() {
        dynamicBaseUrlSwitcher.stopDynamicUrlSwitching()
    }

    override suspend fun oneShotDynamicUrlSwitching() {
        val account = getCurrentOwnCloudAccount(appContext)
        account?.let {
            dynamicBaseUrlSwitcher.oneShotDynamicUrlSwitching(it)
        }
    }
}