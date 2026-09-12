package com.huanchengfly.tieba.post.arch

import android.util.Log
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.core.network.Error
import com.huanchengfly.tieba.post.core.network.di.NetUtilsEntryPoint
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.core.network.exception.getErrorCode
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.retrofit.interceptors.ConnectivityInterceptor
import dagger.hilt.android.EntryPointAccessors
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
            val networkMonitor = EntryPointAccessors.fromApplication<NetUtilsEntryPoint>(App.INSTANCE).networkMonitor()
            val e = ConnectivityInterceptor.wrapException(networkMonitor, exception)
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