package com.huanchengfly.tieba.post.core.network.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest.Builder
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.os.trace
import com.huanchengfly.tieba.post.core.common.Dispatcher
import com.huanchengfly.tieba.post.core.common.TbDispatchers.IO
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NetworkMonitor"

@Singleton
internal class ConnectivityManagerNetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : NetworkMonitor {

    private val connectivityManager: ConnectivityManager? = context.getSystemService()

    private val _networkState: Flow<Pair<Boolean, Boolean>> = callbackFlow {
        trace("NetworkMonitor.callbackFlow") {
            if (connectivityManager == null) {
                channel.trySend(false to false)
                channel.close()
                return@callbackFlow
            }

            val callback = object : ConnectivityManager.NetworkCallback() {
                private val networks = hashSetOf<Network>()
                private var unmetered = true

                override fun onLost(network: Network) {
                    Log.e(TAG, "onLost: Network $network")
                    networks -= network
                    channel.trySend(networks.isNotEmpty() to unmetered)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (isValidated) {
                        networks += network
                    }
                    val newState = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    Log.w(TAG, "onCapabilitiesChanged: Network $network, isValidated: $isValidated, isUnmetered: $unmetered to $newState")
                    if (isValidated && unmetered != newState) {
                        unmetered = newState
                        channel.trySend(networks.isNotEmpty() to newState)
                    }
                }
            }

            trace("NetworkMonitor.registerNetworkCallback") {
                val request = Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, callback)
            }

            /**
             * Sends the latest connectivity status to the underlying channel.
             */
            connectivityManager.run {
                channel.trySend(isCurrentlyValidated() to isCurrentlyNotMetered())
            }

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }
        .flowOn(ioDispatcher)
        .conflate()

    override val isValidated: StateFlow<Boolean> = _networkState
        .map { it.first }
        .stateIn(coroutineScope, started = SharingStarted.Eagerly, false)

    override val isUnmetered: StateFlow<Boolean> = _networkState
        .map { it.second }
        .stateIn(coroutineScope, started = SharingStarted.Eagerly, true)

    override fun isCurrentlyValidated(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun isCurrentlyNotMetered(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}