package com.huanchengfly.tieba.post.api.retrofit.interceptors

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.components.NetworkObserver.isNetworkConnected
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

object ConnectivityInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return runCatching { chain.proceed(chain.request()) }
            .onFailure {
                throw wrapException(it)
            }
            .getOrThrow()
    }

    fun wrapException(e: Throwable): Throwable {
        return when (e) {
            is SocketTimeoutException,
            is SocketException,
            is SSLHandshakeException -> if (isNetworkConnected) {
                NoConnectivityException(App.INSTANCE.getString(R.string.connectivity_timeout))
            } else {
                e
            }

            is IOException -> if (!isNetworkConnected) {
                NoConnectivityException(App.INSTANCE.getString(R.string.no_internet_connectivity))
            } else {
                e
            }

            else -> e
        }
    }
}