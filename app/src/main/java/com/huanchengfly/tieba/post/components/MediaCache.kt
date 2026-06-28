package com.huanchengfly.tieba.post.components

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.WorkerThread
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
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
import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.api.getUserAgent
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max

/** 百度视频 CDN 域名 */
const val BD_VIDEO_HOST = "tb-video.bdstatic.com"

/** 媒体缓存管理单例 */
@UnstableApi
object MediaCache {
    private const val LOCAL_CACHE_DIRECTORY = "media"

    private const val DEFAULT_CACHE_LIMIT_BYTES = 500L * 1024L * 1024L

    /** 数据读取来源 */
    enum class ReadOrigin {
        Upstream,
        Cache,
    }

    /** 数据读取事件：起始/结束字节位置、实际读取字节数、来源和上游返回的内容总长度 */
    data class ReadEvent(
        val source: ReadOrigin,
        val dataSpec: DataSpec,
        val chunkStart: Long,
        val chunkEnd: Long,
        val bytesRead: Int,
        val contentLength: Long = C.LENGTH_UNSET.toLong(),
    )

    /** 数据读取观察者回调接口 */
    fun interface ReadObserver {
        fun onRead(event: ReadEvent)
    }

    /** 获取缓存目录：优先使用外部存储，不可用时回退到内部存储 */
    private val Context.mediaCacheDir: File
        get() {
            val baseDir =
                if (!Environment.isExternalStorageRemovable()) {
                    externalCacheDir ?: cacheDir
                } else {
                    cacheDir
                }
            return File(baseDir, LOCAL_CACHE_DIRECTORY)
        }

    @Volatile
    private var mCache: Cache? = null
    private val cacheLock = Any()

    @Volatile
    private var cacheMaxBytes: Long = DEFAULT_CACHE_LIMIT_BYTES

    @Volatile
    private var appliedCacheMaxBytes: Long = DEFAULT_CACHE_LIMIT_BYTES

    private val activeCacheKeys = mutableSetOf<String>()

    /** 单线程复用，避免每次释放都创建 Thread */
    private val releaseExecutor =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "MediaCache-release").apply { isDaemon = true }
        }

    /** 创建 SimpleCache 实例 */
    private fun buildCache(context: Context): Cache =
        SimpleCache(
            context.mediaCacheDir,
            LeastRecentlyUsedCacheEvictor(cacheMaxBytes),
            StandaloneDatabaseProvider(context),
        ).also {
            appliedCacheMaxBytes = cacheMaxBytes
        }

    /** 获取缓存实例（双重检查锁） */
    fun cache(context: Context): Cache =
        mCache ?: synchronized(cacheLock) {
            mCache ?: buildCache(context.applicationContext).also { mCache = it }
        }

    /** 设置缓存大小上限（字节）；有活跃播放器时延迟到空闲后重建缓存实例 */
    fun setMaxBytes(maxBytes: Long) {
        val safeBytes = max(1L, maxBytes)
        synchronized(cacheLock) {
            if (cacheMaxBytes == safeBytes) return
            cacheMaxBytes = safeBytes
            if (activeCacheKeys.isEmpty() && cacheMaxBytes != appliedCacheMaxBytes) {
                scheduleReleaseLocked()
            }
        }
    }

    /** 没有活跃播放器时释放缓存实例，使下次创建时应用待生效的大小上限 */
    @WorkerThread
    fun releaseIfIdle() {
        synchronized(cacheLock) {
            if (activeCacheKeys.isEmpty() && cacheMaxBytes != appliedCacheMaxBytes) {
                releaseCacheLocked()
            }
        }
    }

    /** 释放当前缓存实例；调用方需持有 [cacheLock]，下次访问会按最新上限重建 */
    private fun releaseCacheLocked() {
        mCache?.let {
            it.release()
            mCache = null
        }
    }

    /** 在后台线程释放缓存实例，避免主线程阻塞 I/O；由 [setMaxBytes] 和 [unmarkActive] 在主线程触发时使用 */
    private fun scheduleReleaseLocked() {
        if (activeCacheKeys.isNotEmpty() || cacheMaxBytes == appliedCacheMaxBytes) return
        releaseExecutor.execute {
            synchronized(cacheLock) {
                releaseCacheLocked()
            }
        }
    }

    /** 获取当前缓存目录的实际大小（字节） */
    fun sizeBytes(context: Context): Long {
        val dir = context.applicationContext.mediaCacheDir
        return dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

    /** 清除视频缓存，保留当前正在使用的资源 */
    fun clear(
        context: Context,
        excludeCacheKeys: Set<String> = emptySet(),
    ) {
        val appContext = context.applicationContext
        val failures =
            synchronized(cacheLock) {
                val downloadCache = mCache ?: buildCache(appContext).also { mCache = it }
                val keysToRemove = downloadCache.keys.filterNot(excludeCacheKeys::contains)
                keysToRemove.mapNotNull { key ->
                    runCatching { downloadCache.removeResource(key) }.exceptionOrNull()?.let { key to it }
                }
            }
        if (failures.isNotEmpty()) {
            val failedKeys = failures.joinToString(limit = 3) { it.first }
            throw IllegalStateException("Failed to clear video cache entries: $failedKeys", failures.first().second)
        }
    }

    /** 注册当前活跃的视频缓存键，避免清理或重建时影响正在使用的资源 */
    fun markActive(cacheKey: String) {
        synchronized(cacheLock) {
            cacheKey.takeIf { it.isNotEmpty() }?.let(activeCacheKeys::add)
        }
    }

    /** 取消注册当前活跃的视频缓存键，并在需要时应用待生效的大小上限 */
    fun unmarkActive(cacheKey: String) {
        synchronized(cacheLock) {
            activeCacheKeys.remove(cacheKey)
            if (activeCacheKeys.isEmpty() && cacheMaxBytes != appliedCacheMaxBytes) {
                scheduleReleaseLocked()
            }
        }
    }

    /** 获取当前活跃的视频缓存键快照 */
    fun activeKeys(): Set<String> = synchronized(cacheLock) { activeCacheKeys.toSet() }

    /** 生成缓存键：百度视频使用路径中的 MD5，其他 URL 使用默认策略 */
    fun key(url: String): String {
        val uri = Uri.parse(url)
        return uri.getBdVideoMD5() ?: CacheKeyFactory.DEFAULT.buildCacheKey(DataSpec(uri))
    }

    /** 获取缓存元数据中的内容长度（字节），未知时返回 -1 */
    fun contentLength(
        context: Context,
        url: String,
    ): Long {
        val key = key(url)
        return ContentMetadata.getContentLength(cache(context).getContentMetadata(key))
    }

    /** 创建数据源工厂 */
    fun dataSourceFactory(
        context: Context,
        readObserver: ReadObserver? = null,
        disableDiskCache: Boolean = false,
    ): CacheDataSource.Factory {
        val appContext = context.applicationContext
        val downloadCache = cache(appContext)
        val cacheSink =
            if (disableDiskCache) {
                null
            } else {
                CacheDataSink.Factory().setCache(downloadCache)
            }

        // HTTP 数据源工厂，设置贴吧客户端 User-Agent
        val httpFactory =
            DefaultHttpDataSource.Factory().apply {
                setUserAgent(getUserAgent("tieba/${ClientVersion.TIEBA_V12.version}"))
            }

        val baseUpstreamFactory = DefaultDataSource.Factory(appContext, httpFactory)
        val baseDownstreamFactory = FileDataSource.Factory()

        fun DataSource.Factory.observing(source: ReadOrigin): DataSource.Factory =
            readObserver?.let { observer ->
                ObservingDataSourceFactory(
                    upstream = this,
                    source = source,
                    observer = observer,
                )
            } ?: this

        val upstreamFactory = baseUpstreamFactory.observing(ReadOrigin.Upstream)
        val downStreamFactory = baseDownstreamFactory.observing(ReadOrigin.Cache)

        return CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setCacheKeyFactory(BdMediaCacheKeyFactory)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setCacheReadDataSourceFactory(downStreamFactory)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** 创建 [ObservingDataSource] 实例的工厂 */
    private class ObservingDataSourceFactory(
        private val upstream: DataSource.Factory,
        private val source: ReadOrigin,
        private val observer: ReadObserver?,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = ObservingDataSource(upstream.createDataSource(), source, observer)
    }

    /** 委托模式包装 [DataSource]，在 [read] 时通过 [observer] 回调字节范围 */
    private class ObservingDataSource(
        private val upstream: DataSource,
        private val source: ReadOrigin,
        private val observer: ReadObserver?,
    ) : DataSource by upstream {
        private var openedDataSpec: DataSpec? = null

        private var readBytes: Long = 0L

        private var contentLength: Long = C.LENGTH_UNSET.toLong()

        override fun open(dataSpec: DataSpec): Long {
            openedDataSpec = dataSpec
            readBytes = 0L
            val resolvedLength = upstream.open(dataSpec)
            contentLength =
                if (resolvedLength != C.LENGTH_UNSET.toLong() &&
                    dataSpec.length == C.LENGTH_UNSET.toLong() &&
                    dataSpec.position != C.LENGTH_UNSET.toLong()
                ) {
                    dataSpec.position + resolvedLength
                } else {
                    C.LENGTH_UNSET.toLong()
                }
            return resolvedLength
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            val bytesReadNow = upstream.read(buffer, offset, length)
            if (bytesReadNow > 0) {
                val spec = openedDataSpec
                if (spec != null && observer != null && spec.position != C.LENGTH_UNSET.toLong()) {
                    // 计算本次读取在文件中的字节范围
                    val chunkStart = spec.position + readBytes
                    val chunkEnd = chunkStart + bytesReadNow
                    observer.onRead(
                        ReadEvent(
                            source = source,
                            dataSpec = spec,
                            chunkStart = chunkStart,
                            chunkEnd = chunkEnd,
                            bytesRead = bytesReadNow,
                            contentLength = contentLength,
                        ),
                    )
                }
                readBytes += bytesReadNow
            }
            return bytesReadNow
        }

        override fun close() {
            try {
                upstream.close()
            } finally {
                openedDataSpec = null
                readBytes = 0L
                contentLength = C.LENGTH_UNSET.toLong()
            }
        }
    }

    /** 从 URL 提取 MD5 作为缓存键 */
    fun Uri.getBdVideoMD5(): String? {
        if (host != BD_VIDEO_HOST) return null

        val parts = path?.split('_') ?: return null
        if (parts.size < 3) return null

        return parts[1]
    }

    /** 缓存键工厂 */
    private val BdMediaCacheKeyFactory =
        CacheKeyFactory { dataSpec: DataSpec ->
            dataSpec.uri.getBdVideoMD5() ?: CacheKeyFactory.DEFAULT.buildCacheKey(dataSpec)
        }

    /** 强制释放缓存实例 */
    @WorkerThread
    fun release() {
        synchronized(cacheLock) {
            releaseCacheLocked()
        }
    }
}
