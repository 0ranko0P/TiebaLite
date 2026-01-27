package com.huanchengfly.tieba.post.components.viewer

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.iielse.imageviewer.core.ImageLoader
import com.github.iielse.imageviewer.core.Photo
import com.github.iielse.imageviewer.widgets.video.ExoVideoView2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.glide.ProgressListenerOnUI
import com.huanchengfly.tieba.post.components.glide.TbGlideUrl
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewItem
import com.huanchengfly.tieba.post.utils.GlideUtil.addProgressListener

class SimpleImageLoader(
    private val glide: RequestManager,
    private val onClick: View.OnClickListener,
    private val useTbGlideUrl: Boolean,
    private val onProgress: (data: PhotoViewItem) -> Unit
) : ImageLoader {

    private var initialAnimation = true

    private fun getGlideModel(url: String): Any = if (useTbGlideUrl) TbGlideUrl(url) else url

    override fun load(view: ImageView, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        val it = (data as? PhotoViewItem?)?.originUrl ?: return

        view.setOnClickListener(onClick)
        glide.load(getGlideModel(url = it))
            .placeholder(view.drawable)
            .error(R.drawable.ic_error)
            .addProgressListener(data.originUrl, ProgressListenerOnUI { progress ->
                data.progress = progress
                onProgress(data)
            })
            .let {
                // Set transition animation on first ImageView
                // the rest ImageViews loads in background without animation
                if (initialAnimation) {
                    initialAnimation = false
                    it.transition(DrawableTransitionOptions.withCrossFade())
                } else it
            }
            .into(view)
    }

    override fun load(exoVideoView: ExoVideoView2, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        throw RuntimeException("Stub!")
    }

    override fun load(subsamplingView: SubsamplingScaleImageView, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        if (data !is PhotoViewItem) throw RuntimeException("Not implemented: ${data::class.simpleName}")

        subsamplingView.setOnClickListener(onClick)
        glide.downloadOnly()
            .error(R.drawable.ic_error)
            .load(getGlideModel(url = data.originUrl))
            .addProgressListener(data.originUrl, ProgressListenerOnUI { progress ->
                data.progress = progress
                onProgress(data)
            })
            .into(SubsamplingScaleTarget(subsamplingView))
    }
}