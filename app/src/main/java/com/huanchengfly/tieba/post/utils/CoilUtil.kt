package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.IntSize
import coil3.decode.BlackholeDecoder
import coil3.decode.Decoder
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.huanchengfly.tieba.post.core.common.ktx.unsafeLazy
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.IOException
import java.io.InputStream

object CoilUtil {

    val DarkFilter: ColorFilter by unsafeLazy {
        ColorFilter.colorMatrix(ColorMatrix().apply {
            setToScale(0.7f, 0.7f, 0.7f, 1.0f)
        })
    }

    val BlackholeDecoderFactory: Decoder.Factory by unsafeLazy {
        BlackholeDecoder.Factory()
    }

    fun ImageRequest.Builder.downloadOnly() = apply {
        decoderFactory(BlackholeDecoderFactory).memoryCachePolicy(CachePolicy.DISABLED)
    }

    /**
     * Download image or return cached file directly from Coil disk cache.
     * */
    suspend fun downloadCancelable(context: Context, url: String): File {
        val imageLoader = context.applicationContext.imageLoader
        val diskCache = imageLoader.diskCache ?: throw IllegalStateException("Disk cache disabled")
        var snapshot = diskCache.openSnapshot(url)
        // Cache miss, download from network
        if (snapshot == null) {
            val request = ImageRequest.Builder(context.applicationContext)
                .data(url)
                .downloadOnly()
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                snapshot = diskCache.openSnapshot(url)!!
            } else {
                throw (result as ErrorResult).throwable
            }
        }

        return snapshot.data.toFile()
    }

    /**
     * Decodes the raw dimensions without allocating memory for the entire image
     * */
    @WorkerThread
    fun decodeRawDimensions(context: Context, resource: Uri): Result<IntSize> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        var ins: InputStream? = null
        try {
            ins = context.contentResolver.openInputStream(resource) ?: throw IOException("Unable to open $resource")
            BitmapFactory.decodeStream(ins, null, options)

            if (options.outWidth == -1 || options.outHeight == -1) {
                throw IOException("Failed to decode dimensions of $resource")
            }
            return Result.success(
                IntSize(width = options.outWidth, height = options.outHeight)
            )
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            ins?.closeQuietly()
        }
    }
}