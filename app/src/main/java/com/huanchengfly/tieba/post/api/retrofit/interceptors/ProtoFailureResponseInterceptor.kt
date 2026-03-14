package com.huanchengfly.tieba.post.api.retrofit.interceptors

import com.huanchengfly.tieba.post.api.Error
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.ProtoCommonResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import okhttp3.Interceptor
import okhttp3.Response

object ProtoFailureResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body
        if (!response.isSuccessful || body == null || body.contentLength() == 0L) return response

        val inputStream = body.source().also {
            it.request(Long.MAX_VALUE)
        }.buffer.clone().inputStream()

        val protoCommonResponse = try {
            ProtoCommonResponse.ADAPTER.decode(inputStream)
        } catch (exception: Exception) {
            exception.printStackTrace()
            //如果返回内容解析失败, 说明它不是一个合法的 json
            //如果在拦截器抛出 MalformedJsonException 会导致 Retrofit 的异步请求一直卡着直到超时
            return response
        } finally {
            inputStream.close()
        }

        protoCommonResponse.error?.run {
            if (error_code != 0 && error_code != Error.ERROR_ACCOUNT_BLOCKED/* 账号封禁错误由DataSource 处理 */) {
                throw TiebaApiException(CommonResponse(error_code, error_msg))
            }
        }
        return response
    }
}