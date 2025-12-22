package com.huanchengfly.tieba.post.api.retrofit.exception

import com.huanchengfly.tieba.post.api.Error
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

fun Throwable.getErrorCode(): Int = when (this) {
    is TiebaException -> code

    is SocketTimeoutException,
    is SocketException,
    is SSLHandshakeException -> Error.ERROR_NETWORK

    else -> Error.ERROR_UNKNOWN
}

fun Throwable.getErrorMessage(): String {
    return if (message.isNullOrEmpty()) {
        this::class.java.simpleName
    } else {
        message!!
    }
}