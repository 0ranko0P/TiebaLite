package com.huanchengfly.tieba.post.core.network.retrofit.interceptors

import com.huanchengfly.tieba.post.core.network.exception.NoConnectivityException
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class ConnectivityInterceptor(val networkMonitor: NetworkMonitor) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return runCatching { chain.proceed(chain.request()) }
            .onFailure {
                throw wrapException(networkMonitor, it)
            }
            .getOrThrow()
    }

    companion object {
        fun wrapException(networkMonitor: NetworkMonitor, e: Throwable): Throwable {
            return when (e) {
                is SocketTimeoutException,
                is SocketException,
                is SSLHandshakeException -> if (networkMonitor.isCurrentlyValidated()) {
                    NoConnectivityException("连接超时!")
                } else {
                    e
                }

                is IOException -> if (!networkMonitor.isCurrentlyValidated()) {
                    NoConnectivityException()
                } else {
                    e
                }

                else -> e
            }
        }
    }
}