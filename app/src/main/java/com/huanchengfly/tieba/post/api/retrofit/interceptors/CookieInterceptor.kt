package com.huanchengfly.tieba.post.api.retrofit.interceptors

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.utils.ClientUtils
import okhttp3.Interceptor
import okhttp3.Response

object CookieInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (ClientUtils.baiduId.isNullOrEmpty()) {
            ClientUtils.saveBaiduId(App.INSTANCE, getBaiduID(response))
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