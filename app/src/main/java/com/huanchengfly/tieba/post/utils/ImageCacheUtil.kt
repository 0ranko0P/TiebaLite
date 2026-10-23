package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.annotation.WorkerThread
import coil3.imageLoader
import com.huanchengfly.tieba.post.core.media.util.ImageDownloadUtil.FILE_PROVIDER_SHARE_DIR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 图片缓存工具类
 * Created by Trojx on 2016/10/10 0010.
 */
object ImageCacheUtil {

    private const val GLIDE_DISK_CACHE_DIR = "image_manager_disk_cache"

    /**
     * 清除图片所有缓存
     */
    suspend fun clearImageAllCache(context: Context) = withContext(Dispatchers.IO) {
        val coil = context.imageLoader
        coil.diskCache?.clear()
        withContext(Dispatchers.Main) { coil.memoryCache?.clear() }

        // 清除分享图片缓存
        try {
            File(context.cacheDir, FILE_PROVIDER_SHARE_DIR).deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取图片缓存大小
     */
    suspend fun getCacheSize(context: Context): Long = withContext(Dispatchers.IO) {
        val coilCacheSize = context.imageLoader.diskCache?.size ?: 0
        val shareCacheSize = getFolderSize(File(context.cacheDir, FILE_PROVIDER_SHARE_DIR))
        coilCacheSize + shareCacheSize
    }

    // TODO: Remove
    @WorkerThread
    fun clearGlideDiskCache(context: Context) {
        val cacheDir = File(context.cacheDir, GLIDE_DISK_CACHE_DIR)
        if (cacheDir.exists()) {
            try {
                cacheDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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