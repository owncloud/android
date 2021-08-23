package com.owncloud.android.lib.common

import timber.log.Timber

class ConnectionValidator (
        private val ocClient: OwnCloudClient
    ) {

    fun dosomething() {
        Timber.d(ocClient.toString())
    }
}