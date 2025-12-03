package com.owncloud.android.domain.device.usecases

interface DynamicUrlSwitchingController {

    fun initDynamicUrlSwitching()

    fun startDynamicUrlSwitching()
    
    fun stopDynamicUrlSwitching()

    suspend fun oneShotDynamicUrlSwitching()
}

