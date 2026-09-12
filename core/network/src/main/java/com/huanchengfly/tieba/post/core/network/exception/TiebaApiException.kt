package com.huanchengfly.tieba.post.core.network.exception

import com.huanchengfly.tieba.post.core.network.model.CommonResponse

class TiebaApiException(
    private val commonResponse: CommonResponse
) : TiebaException(commonResponse.errorMsg.takeIf { it.isNotEmpty() } ?: "未知错误") {
    override val code: Int
        get() = commonResponse.errorCode

    override fun toString(): String {
        return "TiebaApiException(code=$code, message=$message, commonResponse=$commonResponse)"
    }
}