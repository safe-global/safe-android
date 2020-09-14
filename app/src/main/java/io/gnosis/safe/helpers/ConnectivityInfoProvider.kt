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
            offline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities =  connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                capabilities?.let {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> false
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> false
                        else -> true
                    }
                } ?: true
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting
            }
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
