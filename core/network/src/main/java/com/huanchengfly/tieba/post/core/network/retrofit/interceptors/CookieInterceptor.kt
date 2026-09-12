package com.huanchengfly.tieba.post.core.network.retrofit.interceptors

import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import okhttp3.Interceptor
import okhttp3.Response

internal class CookieInterceptor(private val clientConfigProvider: ClientConfigProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (clientConfigProvider.getBaiduId().isNullOrEmpty()) {
            clientConfigProvider.saveBaiduId(getBaiduID(response))
        }
        return response
    }

    private fun getBaiduID(response: Response): String? {
        val uidCookie = response.headers("Set-Cookie").find {
            it.substringBefore("=").equals("BAIDUID", ignoreCase = true)
        }

        return uidCookie?.run { substringAfter("=").substringBefore(";") }
    }
}