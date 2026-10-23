package com.huanchengfly.tieba.post.components.coil

import android.net.Uri
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.BlackholeDecoder
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.target.Target
import coil3.toBitmap
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.huanchengfly.tieba.post.core.ui.R
import com.huanchengfly.tieba.post.core.ui.util.CoilUtil
import com.huanchengfly.tieba.post.core.ui.util.CoilUtil.downloadOnly
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Coil [Target] for loading large image into [SubsamplingScaleImageView]
 * */
class SubsamplingScaleTarget(val view: SubsamplingScaleImageView): Target {

    init {
        view.maxScale = 10F
        view.isZoomEnabled = true
    }

    override fun onError(error: Image?) {
        view.recycle()

        if (error != null) {
            val errorImg = when (error) {
                is BitmapImage -> ImageSource.cachedBitmap(error.bitmap)

                is DrawableImage -> ImageSource.bitmap(error.drawable.toBitmap())

                else -> ImageSource.bitmap(error.toBitmap())
            }
            view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            view.setImage(errorImg)
        }
    }

    override fun onSuccess(result: Image) {
        if (result !is SubsamplingImage) return

        MainScope().launch {
            val uri = Uri.fromFile(result.fullImage)
            withContext(Dispatchers.IO) { CoilUtil.decodeRawDimensions(view.context, uri) }
                .onFailure { err ->
                    Log.e(TAG, "onFailure: unable to decode dimensions", err)
                    onError(AppCompatResources.getDrawable(view.context, R.drawable.ic_error)?.asImage())
                }
                .onSuccess {
                    val isLongPic = ImageUtil.isLongImg(it.width, it.height)
                    Log.i(TAG, "onSuccess: dimensions: $it, fSize: ${result.fullImage.length()/1024} KiB")

                    if (isLongPic) {
                        view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START)
                    } else {
                        view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    }
                    view.setImage(ImageSource.uri(uri))
                }
        }
    }

    companion object {

        private const val TAG = "SubsamplingScaleTarget"

        class SubsamplingImage(val fullImage: File): Image by BlackholeDecoder.Factory.EMPTY_IMAGE

        /**
         * Load the image referenced by [data] and set it on this [SubsamplingScaleImageView].
         *
         * Example:
         * ```
         * SubsamplingScaleImageView.load("https://example.com/image.jpg") {
         *     crossfade(true)
         *     transformations(CircleCropTransformation())
         * }
         * ```
         *
         * @param data The data to load.
         * @param imageLoader The [ImageLoader] that will be used to enqueue the [ImageRequest].
         *  By default, the singleton [ImageLoader] will be used.
         * @param builder An optional lambda to configure the [ImageRequest].
         */
        inline fun SubsamplingScaleImageView.load(
            data: Any?,
            imageLoader: ImageLoader = context.imageLoader,
            builder: ImageRequest.Builder.() -> Unit = {},
        ): Disposable {
            val request = ImageRequest.Builder(context)
                .data(data)
                .downloadOnly()
                .target(SubsamplingScaleTarget(this))
                .apply(builder)
                .build()
            return imageLoader.enqueue(request)
        }
    }
}
