package com.huanchengfly.tieba.post.core.network.retrofit.interceptors

import com.huanchengfly.tieba.post.core.network.Header
import com.huanchengfly.tieba.post.core.network.Method
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Response

internal object DropInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var headers = request.headers
        var httpUrl = request.url
        var body = request.body

        val dropHeadersHeader = headers[Header.DROP_HEADERS]
        if (dropHeadersHeader != null) {
            headers = headers.newBuilder()
                .apply {
                    removeAll(Header.DROP_HEADERS)
                    dropHeadersHeader.split(",").forEach {
                        removeAll(it)
                    }
                }
                .build()

        }

        val dropParamsHeader = headers[Header.DROP_PARAMS]
        if (dropParamsHeader != null) {
            headers = headers.newBuilder()
                .removeAll(Header.DROP_PARAMS)
                .build()
            val dropParams = dropParamsHeader.split(",")
            when {
                request.method == Method.GET -> {
                    httpUrl = request.url.newBuilder().apply {
                        dropParams.forEach {
                            removeAllQueryParameters(it)
                        }
                    }.build()
                }

                body is FormBody -> {
                    val newBody = FormBody.Builder().apply {
                        for (i in 0 until body.size) {
                            if (!dropParams.contains(body.name(i))) {
                                add(body.name(i), body.value(i))
                            }
                        }
                    }.build()
                    body = newBody
                }

                else -> {}
            }
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