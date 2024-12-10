package com.huanchengfly.tieba.post.components.viewer

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.iielse.imageviewer.core.ImageLoader
import com.github.iielse.imageviewer.core.Photo
import com.github.iielse.imageviewer.widgets.video.ExoVideoView2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.glide.ProgressListenerOnUI
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewItem
import com.huanchengfly.tieba.post.utils.GlideUtil.addProgressListener

class SimpleImageLoader(
    private val onClick: View.OnClickListener,
    private val onProgress: (position: Int, data: PhotoViewItem) -> Unit
) : ImageLoader {

    private var initialAnimation = true

    override fun load(view: ImageView, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        val it = (data as? PhotoViewItem?)?.originUrl ?: return

        view.setOnClickListener(onClick)
        val position = viewHolder.bindingAdapterPosition
        Glide.with(view)
            .load(it)
            .placeholder(view.drawable)
            .error(R.drawable.ic_error)
            .addProgressListener(data.originUrl, ProgressListenerOnUI { progress ->
                data.progress = progress
                onProgress(position, data)
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

        val position = viewHolder.bindingAdapterPosition
        subsamplingView.setOnClickListener(onClick)
        Glide.with(subsamplingView)
            .downloadOnly()
            .error(R.drawable.ic_error)
            .load(data.originUrl)
            .addProgressListener(data.originUrl, ProgressListenerOnUI { progress ->
                data.progress = progress
                onProgress(position, data)
            })
            .into(SubsamplingScaleTarget(subsamplingView))
    }
}