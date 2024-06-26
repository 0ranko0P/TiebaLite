package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.huanchengfly.tieba.post.App

object NetworkUtil {
    fun isNetworkConnected(): Boolean {
        val connectivityManager =
            App.INSTANCE.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
            return activeNetwork.isConnected
        } else {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    fun isWifiConnected(): Boolean {
        val mConnectivityManager = App.INSTANCE
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWiFiNetworkInfo = mConnectivityManager
            .getNetworkInfo(ConnectivityManager.TYPE_WIFI) ?: return false
        return mWiFiNetworkInfo.isConnected
    }
}