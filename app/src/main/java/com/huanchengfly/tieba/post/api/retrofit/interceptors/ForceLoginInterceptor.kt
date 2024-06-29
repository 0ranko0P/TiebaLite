package com.huanchengfly.tieba.post.api.retrofit.interceptors

import com.huanchengfly.tieba.post.api.Header
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.utils.AccountUtil
import okhttp3.Interceptor
import okhttp3.Response

object ForceLoginInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var headers = request.headers
        val httpUrl = request.url
        val body = request.body

        //是否强制登录
        var forceLogin = false
        val forceLoginHeader = headers[Header.FORCE_LOGIN]
        if (forceLoginHeader != null) {
            if (forceLoginHeader == Header.FORCE_LOGIN_TRUE) forceLogin = true
            headers = headers.newBuilder().removeAll(Header.FORCE_LOGIN).build()
        }

        if (forceLogin && !AccountUtil.isLoggedIn()) {
            throw TiebaNotLoggedInException()
        }

        return chain.proceed(
            request.newBuilder()
                .headers(headers)
                .url(httpUrl)
                .method(request.method, body)
                .build()
        )
    }

}