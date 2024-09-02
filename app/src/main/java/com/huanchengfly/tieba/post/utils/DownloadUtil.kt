package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.components.glide.ProgressInterceptor
import com.huanchengfly.tieba.post.components.glide.ProgressListener
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.FileUtil.ensureParents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DownloadUtil {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ProgressInterceptor())
            .build()
    }

    @Throws(IOException::class)
    suspend fun downloadCancelable(url: String, onProgress: ProgressListener?): ResponseBody {
        onProgress?.let {
            ProgressInterceptor.addListener(url, it)
        }
        val request = Request.Builder().url(url).build()
        var result: Response? = null
        try {
            result = client.newCall(request).await()
            if (!result.isSuccessful) {
                throw IOException("Error on HTTP ${result.code}")
            } else {
                return result.body!!
            }
        } catch (e: Exception) {
            result?.closeQuietly()
            throw e
        } finally {
            onProgress?.let {
                ProgressInterceptor.removeListener(url)
            }
        }
    }

    @Throws(IOException::class)
    suspend fun downloadCancelable(url: String, dest: File, onProgress: ProgressListener?) {
        if (dest.exists() && dest.length() > 0) { // Check downloaded
            onProgress?.let { withContext(Dispatchers.Main) { it.onProgress(100) } }
            return
        }
        withContext(Dispatchers.IO) {
            dest.ensureParents()
            val body: ResponseBody = downloadCancelable(url, onProgress)
            try {
                dest.sink().buffer().use { bufferedSink ->
                    bufferedSink.writeAll(body.source())
                }
            } catch (e: Exception) {
                dest.deleteQuietly() // Delete file if error occurred
                throw e
            } finally {
                body.closeQuietly()
            }
        }
    }

    private suspend inline fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            val callback = ContinuationCallback(this, continuation)
            enqueue(callback)
            continuation.invokeOnCancellation(callback)
        }
    }

    private class ContinuationCallback(
        private val call: Call,
        private val continuation: CancellableContinuation<Response>
    ) : Callback, CompletionHandler {

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!call.isCanceled()) {
                continuation.resumeWithException(e)
            }
        }

        override fun invoke(cause: Throwable?) {
            try {
                call.cancel()
            } catch (_: Throwable) {}
        }
    }
}