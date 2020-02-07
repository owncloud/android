package com.owncloud.android.testutil

import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.domain.server.model.ServerInfo

val OC_ServerInfo = ServerInfo(
    authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH,
    baseUrl = "https://demo.owncloud.com",
    ownCloudVersion = "10.3.2.1",
    isSecureConnection = false
)
