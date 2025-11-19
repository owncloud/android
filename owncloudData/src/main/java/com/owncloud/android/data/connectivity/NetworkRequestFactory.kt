package com.owncloud.android.data.connectivity

import android.net.NetworkCapabilities
import android.net.NetworkRequest

internal class NetworkRequestFactory {

    fun createNetworkRequest(vararg capability: Int): NetworkRequest =
        NetworkRequest.Builder().apply {
            capability.forEach {
                addCapability(it)
            }
        }.build()

    fun createVpnRequest(): NetworkRequest =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
}