package com.huanchengfly.tieba.post.api.retrofit.exception

import java.io.IOException

open class TiebaException(message: String) : IOException(message) {
    open val code: Int = -1

    override fun toString(): String {
        return "TiebaException(code=$code, message=$message)"
    }
}

object TiebaUnknownException : TiebaException("未知错误") {
    override val code: Int
        get() = -1
}