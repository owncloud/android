package com.owncloud.android.data.server.datasources

import com.owncloud.android.domain.server.model.ServerInfo

interface RemoteServerInfoDataSource {
    fun getServerInfo(path: String): ServerInfo
}
