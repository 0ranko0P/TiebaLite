package com.huanchengfly.tieba.post.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.arch.unsafeLazy

object NetworkObserver: ConnectivityManager.NetworkCallback(), DefaultLifecycleObserver {

    private val connectivityManager by unsafeLazy {
        App.INSTANCE.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    var isNetworkConnected = false
        private set

    var isNetworkUnmetered = true
        private set

    private var isObserving = false

    fun observeOnLifecycle(lifecycleOwner: LifecycleOwner) {
        if (!isObserving) {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (isObserving) return

        isObserving = true
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        isNetworkUnmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    override fun onAvailable(network: Network) {
        isNetworkConnected = true
    }

    override fun onUnavailable() {
        isNetworkConnected = false
    }

    override fun onLost(network: Network) = onUnavailable()

    override fun onDestroy(owner: LifecycleOwner) {
        connectivityManager.unregisterNetworkCallback(this)
        isObserving = false
    }
}