package com.huanchengfly.tieba.post.arch

import android.util.Log
import com.huanchengfly.tieba.post.api.Error
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * @return a [CoroutineExceptionHandler] that treat network and account exception as warning.
 */
@Suppress("FunctionName")
inline fun TbLiteExceptionHandler(
    tag: String,
    crossinline handler: (CoroutineContext, Throwable, suppressed: Boolean) -> Unit
): CoroutineExceptionHandler {
    return object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            val e = ConnectivityInterceptor.wrapException(exception)
            val suppressed = e.getErrorCode() == Error.ERROR_NETWORK || exception is TiebaNotLoggedInException
            if (suppressed) {
                Log.w(tag, "onHandleException: ${e.getErrorMessage()}")
            } else {
                Log.e(tag, "onHandleException:", exception)
            }
            handler(context, exception, suppressed)
        }
    }
}