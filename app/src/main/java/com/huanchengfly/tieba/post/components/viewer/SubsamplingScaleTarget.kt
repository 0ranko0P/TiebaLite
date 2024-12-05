package com.huanchengfly.tieba.post.components.viewer

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.huanchengfly.tieba.post.utils.GlideUtil
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "SubsamplingScaleTarget"

/**
 * Glide [Target] for loading large image into [SubsamplingScaleImageView]
 * */
class SubsamplingScaleTarget(view: SubsamplingScaleImageView): CustomViewTarget<SubsamplingScaleImageView, File>(view) {

    override fun onLoadFailed(errorDrawable: Drawable?) {
        view.recycle()

        if (errorDrawable != null) {
            val errorImg = if (errorDrawable is BitmapDrawable) {
                ImageSource.cachedBitmap(errorDrawable.bitmap)
            } else {
                ImageSource.bitmap(errorDrawable.toBitmap())
            }
            view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            view.setImage(errorImg)
        }
    }

    override fun onResourceCleared(placeholder: Drawable?) = view.recycle()

    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
        MainScope().launch {
            val uri = Uri.fromFile(resource)
            withContext(Dispatchers.IO) { GlideUtil.decodeRawDimensions(view.context, uri) }
                .onFailure { err ->
                    Log.e(TAG, "onResourceReady: error while decode dimensions", err)
                    onLoadFailed(null)
                }
                .onSuccess {
                    val isLongPic = ImageUtil.isLongImg(it.width, it.height)
                    Log.i(TAG, "onResourceReady: dimensions: $it, fSize: ${resource.length()/1024} KiB")

                    if (isLongPic) {
                        view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START)
                    } else {
                        view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    }
                    view.setImage(ImageSource.uri(uri))
                }
        }
    }
}
