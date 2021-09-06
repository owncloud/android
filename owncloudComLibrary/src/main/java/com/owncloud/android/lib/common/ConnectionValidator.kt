package com.owncloud.android.lib.common

import com.owncloud.android.lib.common.http.methods.HttpBaseMethod
import com.owncloud.android.lib.common.http.methods.nonwebdav.HttpMethod
import timber.log.Timber

class ConnectionValidator {

    fun validate(method: HttpBaseMethod, client: OwnCloudClient) {
        Timber.d("hello world")
    }
}