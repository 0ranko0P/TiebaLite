package com.huanchengfly.tieba.post.components

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
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
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object MediaCache {

    private const val LOCAL_CACHE_DIRECTORY = "media"

    private const val BD_VIDEO_HOST = "tb-video.bdstatic.com"

    private val Context.mediaCacheDir: File
        get() = if (!Environment.isExternalStorageRemovable()) {
            File(externalCacheDir, LOCAL_CACHE_DIRECTORY)
        } else {
            File(cacheDir, LOCAL_CACHE_DIRECTORY)
        }

    @Volatile
    private var mCache: Cache? = null

    private fun getCache(context: Context): Cache {
        return mCache ?: synchronized(this) {
            mCache ?: SimpleCache(
                context.mediaCacheDir,
                LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024), // 100 MiB
                StandaloneDatabaseProvider(context)
            ).also { mCache = it }
        }
    }

    fun Factory(context: Context): CacheDataSource.Factory {
        val downloadCache = getCache(context)
        val cacheSink = CacheDataSink.Factory()
            .setCache(downloadCache)

        val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory())
        val downStreamFactory = FileDataSource.Factory()

        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setCacheKeyFactory(BdVideoCacheKeyFactory)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setCacheReadDataSourceFactory(downStreamFactory)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    // Keep it sync with [VideoInfo.videoMD5]
    @VisibleForTesting()
    fun Uri.getBdVideoMD5(): String? {
        if (host != BD_VIDEO_HOST) return null

        try {
            val start = path!!.indexOf('_') + 1
            val end = path!!.indexOf('_', start)
            return path!!.substring(start, end)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private val BdVideoCacheKeyFactory = CacheKeyFactory { dataSpec: DataSpec ->
        dataSpec.uri.getBdVideoMD5() ?: CacheKeyFactory.DEFAULT.buildCacheKey(dataSpec)
    }

    @WorkerThread
    fun release() {
        mCache?.let {
            it.release()
            mCache = null
        }
    }
}