package com.huanchengfly.tieba.post.components.viewer

import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.lifecycle
import coil3.size.SizeResolver
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.iielse.imageviewer.core.ImageLoader
import com.github.iielse.imageviewer.core.Photo
import com.github.iielse.imageviewer.widgets.video.ExoVideoView2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.coil.SubsamplingScaleTarget.Companion.SubsamplingImage
import com.huanchengfly.tieba.post.components.coil.SubsamplingScaleTarget.Companion.load
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewItem
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultErrorResource

class SimpleImageLoader(
    private val lifecycle: Lifecycle,
    private val loader: coil3.ImageLoader,
    private val onClick: View.OnClickListener,
) : ImageLoader {

    private var initialAnimation = true

    override fun load(view: ImageView, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        val it = (data as? PhotoViewItem?)?.originUrl ?: return

        view.setOnClickListener(onClick)
        view.load(data = it, imageLoader = loader) {
            error(DefaultErrorResource)
            size(SizeResolver.ORIGINAL)
            lifecycle(lifecycle)
            // Set animation on first ImageView
            // the rest ImageViews loads in background without animation
            crossfade(initialAnimation)
            if (initialAnimation) {
                initialAnimation = false
            }
        }
    }

    override fun load(exoVideoView: ExoVideoView2, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        throw RuntimeException("Stub!")
    }

    override fun load(subsamplingView: SubsamplingScaleImageView, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        if (data !is PhotoViewItem) throw RuntimeException("Not implemented: ${data::class.simpleName}")

        subsamplingView.setOnClickListener(onClick)
        subsamplingView.load(data = data.originUrl, imageLoader = loader) {
            error(DefaultErrorResource)
            lifecycle(lifecycle)
            listener(
                onStart = {
                    subsamplingView.setTag(R.id.image_load_tag, data.originUrl)
                },
                onError = { _, result ->
                    Log.w(SimpleImageLoader::class.simpleName, "onError: ${result.throwable.message}")
                },
                onSuccess = { request, _ ->
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                        subsamplingView.getTag(R.id.image_load_tag) == data.originUrl
                    ) {
                        val cache = loader.diskCache!!.openSnapshot(data.originUrl)!!.data
                        request.target!!.onSuccess(SubsamplingImage(fullImage = cache.toFile()))
                    }
                }
            )
        }
    }
}