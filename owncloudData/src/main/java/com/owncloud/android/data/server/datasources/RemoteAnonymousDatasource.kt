package com.owncloud.android.data.server.datasources

import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.lib.resources.status.OwnCloudVersion

interface RemoteAnonymousDatasource {
    fun checkPathExistence(path: String, checkUserCredentials: Boolean): Boolean

    fun getAuthenticationMethods(path: String): List<AuthenticationMethod>

    /**
     * Returns a Pair<OwncloudVersion, isSSLConnection>
     */
    fun getRemoteStatus(path: String): Pair<OwnCloudVersion, Boolean>
}
