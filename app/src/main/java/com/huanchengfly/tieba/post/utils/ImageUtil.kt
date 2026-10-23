package com.huanchengfly.tieba.post.utils

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.webkit.URLUtil
import androidx.media3.common.MimeTypes
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.core.common.ktx.ensureParents
import com.huanchengfly.tieba.post.core.data.model.settings.HabitSettings
import com.huanchengfly.tieba.post.core.data.model.settings.ImageLoadType
import com.huanchengfly.tieba.post.core.network.di.NetUtilsEntryPoint
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object ImageUtil {

    // TODO: Refactor
    private val networkMonitor: NetworkMonitor by lazy {
        EntryPointAccessors.fromApplication<NetUtilsEntryPoint>(App.INSTANCE).networkMonitor()
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun Bitmap.toFile(output: File, quality: Int = 100, format: CompressFormat = CompressFormat.JPEG) {
        output.ensureParents()
        FileOutputStream(output).use { out ->
            if (!this.compress(format, quality, out)) {
                throw IOException("Unable to compress $output to $format.")
            }
        }
    }

    fun getPicId(picUrl: String?): String {
        val fileName = URLUtil.guessFileName(picUrl, null, MimeTypes.IMAGE_JPEG)
        return fileName.replace(".jpg", "")
    }

    /**
     * 根据流量设置返回要加载的缩略图 Url
     *
     * @param loadType 图片加载设置
     * @param originUrl   原图 Url
     * @param smallPicUrl 最差图片
     *
     * @see HabitSettings.imageLoadType
     */
    fun getThumbnail(@ImageLoadType loadType: Int, originUrl: String, smallPicUrl: String): String {
        // Workaround for empty srcPic, originPic in OriginThreadInfo (v12.52.1.0)
        val emptyOrigin = originUrl.isEmpty()
        return if (emptyOrigin || loadWorst(loadType)) smallPicUrl else originUrl
    }

    private fun loadWorst(@ImageLoadType loadType: Int): Boolean {
        return if (loadType == ImageLoadType.SMART_ORIGIN) {
            !networkMonitor.isCurrentlyNotMetered()
        } else {
            loadType != ImageLoadType.ALL_ORIGIN
        }
    }

    // Check is long image with given width x height size
    fun isLongImg(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        return height.toFloat() / width > 4f
    }
}
