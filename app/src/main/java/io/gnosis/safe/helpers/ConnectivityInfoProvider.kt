package io.gnosis.safe.helpers

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
            offline = connectivityManager.activeNetworkInfo == null
        }
    }

    init {
        register()
    }

    private fun register() {
        val builder = NetworkRequest.Builder()
        builder
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
    }

    private fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

class Offline : IOException()
