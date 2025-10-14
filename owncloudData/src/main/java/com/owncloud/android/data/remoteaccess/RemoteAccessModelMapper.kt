package com.owncloud.android.data.remoteaccess

import com.owncloud.android.data.remoteaccess.remote.RemoteAccessDeviceResponse
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessPath
import com.owncloud.android.data.remoteaccess.remote.RemoteAccessPathType
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessDevice
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPath as DomainRemoteAccessPath
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPathType as DomainRemoteAccessPathType

object RemoteAccessModelMapper {

    fun toModel(remote: RemoteAccessDeviceResponse): RemoteAccessDevice {
        return RemoteAccessDevice(
            seagateDeviceId = remote.seagateDeviceId,
            friendlyName = remote.friendlyName,
            hostname = remote.hostname,
            certificateCommonName = remote.certificateCommonName,
        )
    }

    fun toModel(remote: RemoteAccessPath): DomainRemoteAccessPath {
        return DomainRemoteAccessPath(
            type = toModel(remote.type),
            address = remote.address,
            port = remote.port
        )
    }

    private fun toModel(remote: RemoteAccessPathType): DomainRemoteAccessPathType {
        return when (remote) {
            RemoteAccessPathType.LOCAL -> DomainRemoteAccessPathType.LOCAL
            RemoteAccessPathType.PUBLIC -> DomainRemoteAccessPathType.PUBLIC
            RemoteAccessPathType.REMOTE -> DomainRemoteAccessPathType.REMOTE
        }
    }
}

