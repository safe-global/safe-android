package io.gnosis.data.repositories

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class ConnectivityRepository(private val connectivityManager: ConnectivityManager) {

    var offline: Boolean = false

    interface ConnectivityCallback {
        fun onConnectivityChange(offline: Boolean)
    }

    private val networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var connectivityCallback: ConnectivityCallback

    init {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network?) {
                super.onAvailable(network)
                connectivityCallback.onConnectivityChange(false)
            }

            override fun onLost(network: Network?) {
                super.onLost(network)
               connectivityCallback.onConnectivityChange(true)
            }
        }
    }

    fun register(callback: ConnectivityCallback) {
        connectivityCallback = callback
        val builder = NetworkRequest.Builder()
        builder
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
