package com.owncloud.android.data.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.PowerManager
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkStateObserver(
    private val appContext: Context,
) {
    private val networkRequestFactory: NetworkRequestFactory = NetworkRequestFactory()
    private val intentFilter: IntentFilter = IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)

    private val powerManager: PowerManager by lazy { appContext.getSystemService(PowerManager::class.java) }
    private val connectivityManager: ConnectivityManager by lazy { appContext.getSystemService(ConnectivityManager::class.java) }

    private fun obtainNetworkCallback(sendChannel: SendChannel<Connectivity>): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                sendChannel.trySendBlocking(Connectivity.fromNetworkCapabilities(networkCapabilities))
            }

            override fun onLost(network: Network) {
                sendIfUnavailable(sendChannel)
            }
        }
    }

    private fun obtainVpnCallback(sendChannel: SendChannel<Connectivity>): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                sendChannel.trySendBlocking(Connectivity.fromNetworkCapabilities(networkCapabilities))
            }

            override fun onLost(network: Network) {
                val capabilities = connectivityManager.allNetworks.mapNotNull { connectivityManager.getNetworkCapabilities(it) }
                sendChannel.trySendBlocking(Connectivity.fromNetworkCapabilities(capabilities))
            }
        }
    }

    fun observeNetworkState(): Flow<Connectivity> = channelFlow {
        sendIfUnavailable(this@channelFlow)

        val connectivityCallback = obtainNetworkCallback(this@channelFlow)
        val vpnCallback = obtainVpnCallback(this@channelFlow)

        val idleReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (isIdleMode(context)) {
                    trySendBlocking(Connectivity.unavailable())
                }
            }
        }
        appContext.registerReceiver(idleReceiver, intentFilter)

        val request =
            networkRequestFactory.createNetworkRequest(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                    NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED,
            )
        val vpnRequest = networkRequestFactory.createVpnRequest()

        connectivityManager.registerNetworkCallback(request, connectivityCallback)
        connectivityManager.registerNetworkCallback(vpnRequest, vpnCallback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
            connectivityManager.unregisterNetworkCallback(vpnCallback)
            appContext.unregisterReceiver(idleReceiver)
        }
    }.distinctUntilChanged()

    private fun sendIfUnavailable(sendChannel: SendChannel<Connectivity>) {
        if (connectivityManager.activeNetwork == null) {
            sendChannel.trySendBlocking(Connectivity.unavailable())
        }
    }

    private fun isIdleMode(context: Context): Boolean {
        val packageName = context.packageName
        val isIgnoringOptimizations =
            powerManager.isIgnoringBatteryOptimizations(packageName)
        return powerManager.isDeviceIdleMode && !isIgnoringOptimizations
    }

}