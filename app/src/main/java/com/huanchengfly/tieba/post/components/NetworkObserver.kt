package com.huanchengfly.tieba.post.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.arch.unsafeLazy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

object NetworkObserver: ConnectivityManager.NetworkCallback(), DefaultLifecycleObserver {

    private const val TAG = "NetworkObserver"

    private val connectivityManager by unsafeLazy {
        App.INSTANCE.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val _isNetworkConnected = MutableStateFlow(false)
    val isNetworkConnected: StateFlow<Boolean> = _isNetworkConnected.asStateFlow()

    private val _isNetworkUnmetered = MutableStateFlow(true)
    val isNetworkUnmetered: StateFlow<Boolean> = _isNetworkUnmetered.asStateFlow()

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
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        val newState = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val oldState = _isNetworkUnmetered.getAndUpdate { newState }
        if (oldState != isNetworkUnmetered.value) {
            Log.w(TAG, "onCapabilitiesChanged: Network ID: $network, unmetered $oldState to $newState.")
        }
    }

    private fun onAvailabilitiesChanged(network: Network?, available: Boolean) {
        val oldState = _isNetworkConnected.getAndUpdate { available }
        if (oldState != available) {
            Log.e(TAG, "onAvailabilitiesChanged: Network ID: $network, from: $oldState to $available.")
        }
    }

    override fun onAvailable(network: Network) = onAvailabilitiesChanged(network, available = true)

    override fun onUnavailable() = onAvailabilitiesChanged(null, available = false)

    override fun onLost(network: Network) = onAvailabilitiesChanged(network, available = false)

    override fun onDestroy(owner: LifecycleOwner) {
        connectivityManager.unregisterNetworkCallback(this)
        isObserving = false
    }
}