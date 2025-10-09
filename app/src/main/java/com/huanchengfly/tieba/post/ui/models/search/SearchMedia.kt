package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.ui.unit.IntSize
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.MediaInfo

sealed class SearchMedia(val url: String, val dimensions: IntSize?) {
    class Picture(media: MediaInfo): SearchMedia(
        url = with(media) { bigPic ?: smallPic ?: waterPic ?: "" },
        dimensions = media.dimensions
    )

    class Video(media: MediaInfo) : SearchMedia(media.vhsrc ?: "", media.dimensions) {
        val thumbnail: String = media.vpic ?: ""
    }

    fun aspectRatio(): Float = dimensions?.let { it.width.toFloat() / it.height } ?: 2.0f
}

private val MediaInfo.dimensions: IntSize?
    get() {
        // Note: 一些古老视频的尺寸为 null, 古老图片的尺寸为 0
        return if (width != null && height != null && width != 0 && height != 0) {
            IntSize(width = width, height = height)
        } else {
            null
        }
    }
