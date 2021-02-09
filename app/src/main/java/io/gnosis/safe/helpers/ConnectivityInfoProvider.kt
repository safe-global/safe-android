package io.gnosis.safe.helpers

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import java.io.IOException

class ConnectivityInfoProvider(private val connectivityManager: ConnectivityManager) {

    var offline: Boolean = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            super.onAvailable(network)
            offline = false
        }

        override fun onLost(network: Network?) {
            super.onLost(network)
            offline = isOffline()
        }
    }

    init {
        register()
    }

    private fun isOffline(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var online = false
            // Active network is null after returning from sleep. Default active network listener is not triggered
            // Thus we check connectivity of all available networks to see if device is offline
            connectivityManager.allNetworks.forEach loop@{ network ->
                online = connectivityManager.getNetworkCapabilities(network)?.let {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                } ?: false
                if (online) return@loop
            }
            !online
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo == null || !activeNetworkInfo.isConnected
        }
    
    private fun register() {
        val builder = NetworkRequest.Builder()
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        connectivityManager.addDefaultNetworkActiveListener { offline = isOffline() }
    }

    private fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

class Offline : IOException()
