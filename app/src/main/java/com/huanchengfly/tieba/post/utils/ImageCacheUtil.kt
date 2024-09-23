package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache.Factory.DEFAULT_DISK_CACHE_DIR
import com.huanchengfly.tieba.post.utils.ImageUtil.FILE_PROVIDER_SHARE_DIR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 图片缓存工具类
 * Created by Trojx on 2016/10/10 0010.
 */
object ImageCacheUtil {
    /**
     * 清除图片所有缓存
     */
    suspend fun clearImageAllCache(context: Context) = withContext(Dispatchers.IO) {
        val glide: Glide = Glide.get(context)
        // 清除图片内存缓存, 只能在主线程执行
        withContext(Dispatchers.Main) { glide.clearMemory() }

        // 清除图片磁盘缓存
        glide.clearDiskCache()

        // 清除分享图片缓存
        try {
            File(context.cacheDir, FILE_PROVIDER_SHARE_DIR).deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取图片缓存大小, Android O 之后使用国际单位制
     *
     * @see Formatter.FLAG_IEC_UNITS
     * @see Formatter.FLAG_SI_UNITS
     *
     * @return Formatted cacheSize
     */
    suspend fun getCacheSize(context: Context): String = withContext(Dispatchers.IO) {
        val glideCacheSize = getFolderSize(File(context.cacheDir, DEFAULT_DISK_CACHE_DIR))
        val shareCacheSize = getFolderSize(File(context.cacheDir, FILE_PROVIDER_SHARE_DIR))
        Formatter.formatShortFileSize(context, glideCacheSize + shareCacheSize)
    }

    /**
     * 获取指定文件夹内所有文件大小的和
     *
     * @param file file
     * @return size
     */
    @WorkerThread
    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        try {
            val fileList = file.listFiles() ?: return 0
            for (aFileList in fileList) {
                size = if (aFileList.isDirectory) {
                    size + getFolderSize(aFileList)
                } else {
                    size + aFileList.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }
}