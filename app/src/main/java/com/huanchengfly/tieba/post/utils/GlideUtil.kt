package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.IntSize
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.huanchengfly.tieba.post.arch.unsafeLazy
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.io.InputStream

object GlideUtil {
    val CrossFadeTransition: DrawableTransitionOptions by unsafeLazy {
        DrawableTransitionOptions.withCrossFade()
    }

    private val ErrorListener by lazy {
        object : RequestListener<Any> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Any>, isFirstResource: Boolean): Boolean {
                Log.d("onLoadFailed", "Unable to load image for $model", e)
                return true
            }

            /*** NO-OP ***/
            override fun onResourceReady(resource: Any, model: Any, target: Target<Any>?, dataSource: DataSource, isFirstResource: Boolean): Boolean = false
        }
    }

    val DarkFilter: ColorFilter by unsafeLazy {
        ColorFilter.colorMatrix(ColorMatrix().apply {
            setToScale(0.7f, 0.7f, 0.7f, 1.0f)
        })
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getDefaultErrorListener(): RequestListener<T> {
        return ErrorListener as RequestListener<T>
    }

    inline fun <T> RequestBuilder<T>.addListener(
        crossinline onLoadFailed: (
            e: GlideException?,
            model: Any?,
            target: Target<T>,
            isFirstResource: Boolean
        ) -> Boolean = { _, _, _, _ -> false },
        crossinline onResourceReady: (
            resource: T,
            model: Any,
            target: Target<T>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ) -> Boolean = { _, _, _, _, _ -> false }
    ): RequestBuilder<T> {
        val listener = object : RequestListener<T> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<T>, isFirstResource: Boolean): Boolean =
                onLoadFailed.invoke(e, model, target, isFirstResource)

            override fun onResourceReady(resource: T & Any, model: Any, target: Target<T>?, dataSource: DataSource, isFirstResource: Boolean): Boolean =
                onResourceReady.invoke(resource, model, target, dataSource, isFirstResource)
        }

        return addListener(listener)
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