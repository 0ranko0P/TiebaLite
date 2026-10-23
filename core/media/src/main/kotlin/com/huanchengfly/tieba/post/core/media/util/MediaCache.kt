package com.huanchengfly.tieba.post.core.media.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
object MediaCache {

    const val BD_VIDEO_HOST = "tb-video.bdstatic.com"

    private const val LOCAL_CACHE_DIRECTORY = "media"

    private val Context.mediaCacheDir: File
        get() = if (!Environment.isExternalStorageRemovable()) {
            File(externalCacheDir, LOCAL_CACHE_DIRECTORY)
        } else {
            File(cacheDir, LOCAL_CACHE_DIRECTORY)
        }

    @Volatile
    private var mCache: Cache? = null

    fun getCache(context: Context): Cache {
        return mCache ?: synchronized(this) {
            mCache ?: SimpleCache(
                context.mediaCacheDir,
                LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024),
                StandaloneDatabaseProvider(context)
            ).also { mCache = it }
        }
    }

    fun Factory(context: Context): CacheDataSource.Factory {
        val downloadCache = getCache(context)
        val cacheSink = CacheDataSink.Factory()
            .setCache(downloadCache)

        val httpFactory = DefaultHttpDataSource.Factory()
        val upstreamFactory = DefaultDataSource.Factory(context, httpFactory)
        val downStreamFactory = FileDataSource.Factory()

        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setCacheKeyFactory(BdVideoCacheKeyFactory)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setCacheReadDataSourceFactory(downStreamFactory)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun getContentLength(key: String?, url: String): Long {
        val cache = mCache ?: throw NullPointerException("Cache not initialized!")
        if (!cache.keys.contains(key ?: url)) return C.LENGTH_UNSET.toLong()

        val metadata = cache.getContentMetadata(key ?: url)
        return ContentMetadata.getContentLength(metadata)
    }

    fun isCached(url: String): Boolean {
        val key = url.toUri().getBdVideoMD5()
        val contentLength = getContentLength(key, url)
        return contentLength > C.LENGTH_UNSET && mCache!!.isCached(key ?: url, 0, contentLength)
    }

    // Keep it sync with [VideoInfo.videoMD5]
    fun Uri.getBdVideoMD5(): String? {
        if (host != BD_VIDEO_HOST) return null

        try {
            val start = path!!.indexOf('_') + 1
            return path!!.substring(start, start + 32)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun Uri.getBdMediaId(): String = getBdVideoMD5() ?: hashCode().toString()

    private val BdVideoCacheKeyFactory = CacheKeyFactory { dataSpec: DataSpec ->
        dataSpec.uri.getBdVideoMD5() ?: CacheKeyFactory.DEFAULT.buildCacheKey(dataSpec)
    }

    @WorkerThread
    fun release() = synchronized(this) {
        mCache?.release()
        mCache = null
    }
}