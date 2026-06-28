package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.Context
import android.os.SystemClock
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.huanchengfly.tieba.post.components.MediaCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class CachedTimeRange(
    val startMs: Long,
    val endMs: Long,
)

/** 字节范围到播放时间范围的映射；精确来源优先于估算来源 */
private data class ByteTimeRange(
    val cacheKey: String,
    val byteStart: Long,
    val byteEnd: Long,
    val timeStartUs: Long,
    val timeEndUs: Long,
    val eventTimeMs: Long,
    val isExact: Boolean = false,
)

/** 读取时间游标，用于追踪连续读取事件的进度 */
private data class ReadCursor(
    val lastByteEnd: Long,
    val lastTimeUs: Long,
    val lastEventTimeMs: Long,
)

/** 待处理的数据读取事件，包含字节范围和上下文信息 */
private data class ReadChunk(
    val cacheKey: String,
    val chunkStart: Long,
    val chunkEnd: Long,
    val eventTimeMs: Long,
    val durationMs: Long,
    val contentLength: Long,
)

/** 待处理读取事件的上下文，用于分组和去重 */
private data class ReadChunkGroup(
    val cacheKey: String,
    val durationMs: Long,
    val contentLength: Long,
)

/** 字节范围，用于检查精确映射的覆盖情况 */
private data class ByteSpan(
    val start: Long,
    val end: Long,
)

/** 定期轮询 Media3 Cache span，并通过字节-时间映射表转换为时间段 */
@OptIn(UnstableApi::class)
internal class PlaybackCacheTracker(
    private val context: Context,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val persistScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())

    private val rangesByKey = mutableMapOf<String, MutableList<ByteTimeRange>>()
    private val loadedKeys = ConcurrentHashMap.newKeySet<String>()
    private val readCursors = mutableMapOf<String, ReadCursor>()
    private val readQueueLock = Any()
    private val readQueue = mutableListOf<ReadChunk>()
    private var readFlushScheduled = false

    @Volatile
    private var recordingEnabled: Boolean = false

    @Volatile
    private var durationHintMs: Long = 0L

    @Volatile
    private var contentLengthHint: Long = 0L

    @Volatile
    private var recordingKey: String = ""

    private val _cachedRanges = MutableStateFlow<List<CachedTimeRange>>(emptyList())
    private val _contentLengthBytes = MutableStateFlow(-1L)

    val cachedRanges: StateFlow<List<CachedTimeRange>> = _cachedRanges.asStateFlow()
    val contentLengthBytes: StateFlow<Long> = _contentLengthBytes.asStateFlow()

    @Volatile
    private var activeKey: String = ""
    private var lastDurationMs: Long = 0L
    private var pollingJob: Job? = null
    private var cacheEnabled: Boolean = false
    private var pollingEnabled: Boolean = false
    private var latestRanges: List<CachedTimeRange> = emptyList()

    /** DataSource 回调可能不在主线程；这里只采集不可变快照并入队，后续回主线程生成估算映射 */
    val readObserver =
        object : MediaCache.ReadObserver {
            override fun onRead(event: MediaCache.ReadEvent) {
                // 缓存命中不提供可靠的时间推进信息，只记录上游读取
                if (event.source != MediaCache.ReadOrigin.Upstream) return
                val resolvedKey =
                    event.dataSpec.key?.takeIf { it.isNotEmpty() } ?: event.dataSpec.uri
                        .toString()
                        .let { MediaCache.key(it) }
                if (resolvedKey.isEmpty() || resolvedKey != activeKey) return
                if (event.contentLength > 0) {
                    contentLengthHint = event.contentLength
                    _contentLengthBytes.value = event.contentLength
                }
                if (!recordingEnabled) return
                if (durationHintMs <= 0 || contentLengthHint <= 0) return
                val chunkStart = event.chunkStart
                val chunkEnd = event.chunkEnd
                if (chunkEnd <= chunkStart) return
                val eventTimeMs = SystemClock.elapsedRealtime()
                val durationMs = durationHintMs
                val contentLength = contentLengthHint
                if (durationMs <= 0 || contentLength <= 0) return
                enqueueRead(
                    ReadChunk(
                        cacheKey = resolvedKey,
                        chunkStart = chunkStart,
                        chunkEnd = chunkEnd,
                        eventTimeMs = eventTimeMs,
                        durationMs = durationMs,
                        contentLength = contentLength,
                    ),
                )
            }
        }

    /** 从 ExoPlayer 加载事件获取精确字节-时间映射 */
    val loadListener =
        object : AnalyticsListener {
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
            ) {
                if (!recordingEnabled) return
                if ((mediaLoadData.trackFormat?.sampleMimeType ?: mediaLoadData.trackFormat?.containerMimeType) != "video/mp4") return
                val resolvedKey = cacheKeyFrom(loadEventInfo) ?: recordingKey.takeIf { it.isNotEmpty() } ?: return
                val byteStart = loadEventInfo.dataSpec.position
                if (byteStart < 0) return
                val byteLen =
                    when {
                        loadEventInfo.dataSpec.length > 0 -> loadEventInfo.dataSpec.length
                        loadEventInfo.bytesLoaded > 0 -> loadEventInfo.bytesLoaded
                        else -> 0L
                    }
                if (byteLen <= 0) return
                val mediaStartMs = mediaLoadData.mediaStartTimeMs
                val mediaEndMs = mediaLoadData.mediaEndTimeMs
                if (mediaStartMs < 0 || mediaEndMs <= mediaStartMs) return
                addRange(
                    ByteTimeRange(
                        cacheKey = resolvedKey,
                        byteStart = byteStart,
                        byteEnd = byteStart + byteLen,
                        timeStartUs = mediaStartMs * 1000L,
                        timeEndUs = mediaEndMs * 1000L,
                        eventTimeMs = eventTime.realtimeMs,
                        isExact = true,
                    ),
                )
            }
        }

    /** 切源会重置跟踪状态但不自动轮询；调用方根据 UI 可见性决定是否启动轮询 */
    @MainThread
    fun setSource(newCacheKey: String) {
        flushReads()
        val oldKey = activeKey
        activeKey = newCacheKey
        recordingKey = activeKey
        setCachedRanges(emptyList())
        lastDurationMs = 0L
        durationHintMs = 0L
        contentLengthHint = 0L
        _contentLengthBytes.value = -1L
        if (oldKey.isNotEmpty() && oldKey != activeKey) persistRanges(oldKey)
    }

    @MainThread
    fun attachToPlayer(player: ExoPlayer) {
        player.removeAnalyticsListener(loadListener)
        player.addAnalyticsListener(loadListener)
    }

    /** 预览播放器生命周期独立，只在预览播放器释放时解绑监听器 */
    @MainThread
    fun attachToPreviewPlayer(player: ExoPlayer) {
        player.addAnalyticsListener(loadListener)
    }

    @MainThread
    fun detachFromPlayer(player: ExoPlayer) {
        player.removeAnalyticsListener(loadListener)
    }

    /** 控制是否启用缓存追踪 */
    @MainThread
    fun setRangeCacheEnabled(enabled: Boolean) {
        if (cacheEnabled == enabled) return
        cacheEnabled = enabled
        if (!enabled) {
            stopPolling()
            pollingEnabled = false
        }
        recordingEnabled = cacheEnabled && pollingEnabled
        publishRanges()
    }

    /** 控制轮询 */
    @MainThread
    fun setRangePollingEnabled(enabled: Boolean) {
        val active = enabled && cacheEnabled
        if (pollingEnabled == active) return
        pollingEnabled = active
        recordingEnabled = cacheEnabled && pollingEnabled
        if (!active) {
            stopPolling()
        }
        publishRanges()
    }

    /** 仅在播放/加载且 UI 需要展示分段时轮询；首次拿到有效时长后才能建立时间映射 */
    @MainThread
    fun updateRangePolling(player: Player?) {
        if (!pollingEnabled) return
        val p = player
        if (p == null) {
            stopPolling()
            return
        }
        val shouldPoll = p.isPlaying || p.isLoading
        val durationJustReady = p.duration > 0 && lastDurationMs <= 0

        if (durationJustReady) lastDurationMs = p.duration

        if (shouldPoll) {
            if (pollingJob?.isActive != true) startPolling(p)
        } else {
            stopPolling()
        }
    }

    /** 单次轮询缓存分段 */
    @MainThread
    internal suspend fun pollSpans(
        player: Player,
        force: Boolean = false,
    ): Long {
        if (!cacheEnabled) return 5_000L
        if (!pollingEnabled && !force) return 5_000L
        val key = activeKey.ifEmpty { return 5_000L }
        val (spanPairs, metadataLength) =
            withContext(Dispatchers.IO) {
                val cache = MediaCache.cache(context)
                val metadataLength =
                    ContentMetadata
                        .getContentLength(cache.getContentMetadata(key))
                        .takeIf { it > 0 }
                        ?: -1L
                val spans = cache.getCachedSpans(key)
                spans.map { it.position to it.length } to metadataLength
            }

        if (key != activeKey) return 5_000L
        if (metadataLength > 0 && contentLengthHint <= 0) {
            contentLengthHint = metadataLength
            _contentLengthBytes.value = metadataLength
        }
        val durMs = player.duration.takeIf { it > 0 } ?: return 5_000L
        lastDurationMs = durMs
        durationHintMs = durMs

        if (spanPairs.isEmpty() || contentLengthHint <= 0) {
            setCachedRanges(emptyList())
            return 5_000L
        }
        loadRangesIfNeeded(key)

        val ranges = mapSpansToRanges(key, spanPairs, durMs)
        val merged = mergeNearbyRanges(ranges)
        setCachedRanges(merged)
        val cachedMs = merged.sumOf { it.endMs - it.startMs }
        return if (cachedMs >= durMs - 1000) {
            60_000L
        } else {
            5_000L
        }
    }

    /** 释放前处理最后一批读取事件；缓存追踪启用时会持久化当前映射表 */
    @MainThread
    fun release() {
        stopPolling()
        recordingEnabled = false
        flushReads(processWhenDisabled = true)
        val persistJob = persistAllRanges()
        persistJob?.invokeOnCompletion { persistScope.cancel() } ?: persistScope.cancel()
        clearRanges()
        latestRanges = emptyList()
        setCachedRanges(emptyList())
        mainScope.cancel()
    }

    /** 映射按字节偏移排序并合并；超过上限时丢弃最旧记录以控制元数据体积 */
    private fun addRange(mapping: ByteTimeRange) {
        val ranges = rangesByKey.getOrPut(mapping.cacheKey) { mutableListOf() }
        val idx = ranges.binarySearch { it.byteStart.compareTo(mapping.byteStart) }
        val insertionIndex =
            if (idx < 0) {
                -idx - 1
            } else {
                idx
            }
        ranges.add(insertionIndex, mapping)
        mergeAround(ranges, insertionIndex)
        if (ranges.size > MAX_RANGES_PER_KEY) {
            val oldest = ranges.minByOrNull { it.eventTimeMs } ?: return
            ranges.remove(oldest)
        }
        if (mapping.cacheKey != recordingKey) persistRanges(mapping.cacheKey)
    }

    /** 每个缓存 span 与映射表求交集后线性插值为播放时间；忽略未命中的字节范围 */
    private fun mapSpansToRanges(
        key: String,
        spans: List<Pair<Long, Long>>,
        durationMs: Long,
    ): List<CachedTimeRange> {
        if (durationMs <= 0 || spans.isEmpty()) return emptyList()
        val ranges = rangesByKey[key]?.toList().orEmpty()
        if (ranges.isEmpty()) return emptyList()
        val mapped = mutableListOf<CachedTimeRange>()
        for ((position, length) in spans) {
            if (length <= 0) continue
            val spanStart = position
            val spanEnd = position + length
            for (range in ranges) {
                val overlapStart = maxOf(spanStart, range.byteStart)
                val overlapEnd = minOf(spanEnd, range.byteEnd)
                if (overlapEnd <= overlapStart) continue
                val mapByteLen = (range.byteEnd - range.byteStart).toDouble()
                if (mapByteLen <= 0.0) continue
                val ratioStart = (overlapStart - range.byteStart).toDouble() / mapByteLen
                val ratioEnd = (overlapEnd - range.byteStart).toDouble() / mapByteLen
                val mappedStartUs = range.timeStartUs.toDouble() + ratioStart * (range.timeEndUs - range.timeStartUs).toDouble()
                val mappedEndUs = range.timeStartUs.toDouble() + ratioEnd * (range.timeEndUs - range.timeStartUs).toDouble()
                val startMs = (mappedStartUs / 1000.0).toLong().coerceIn(0L, durationMs)
                val endMs = (mappedEndUs / 1000.0).toLong().coerceIn(0L, durationMs)
                if (endMs > startMs) mapped.add(CachedTimeRange(startMs, endMs))
            }
        }
        return mapped
    }

    /** 将指定缓存键的映射表序列化后写入缓存元数据 */
    private fun persistRanges(key: String): Job? {
        if (!cacheEnabled) return null
        if (key.isEmpty()) return null
        val snapshot = rangesByKey[key]?.toList().orEmpty()
        return persistScope.launch {
            withContext(NonCancellable) {
                writeRangeSnapshot(key, snapshot)
            }
        }
    }

    /** 持久化内存中的所有映射表，用于释放时保存最后一批读取事件 */
    private fun persistAllRanges(): Job? {
        if (!cacheEnabled) return null
        val snapshots =
            rangesByKey
                .mapValues { (_, mappings) -> mappings.toList() }
                .filter { (key, mappings) -> key.isNotEmpty() && mappings.isNotEmpty() }
        if (snapshots.isEmpty()) return null
        return persistScope.launch {
            withContext(NonCancellable) {
                snapshots.forEach { (key, snapshot) ->
                    writeRangeSnapshot(key, snapshot)
                }
            }
        }
    }

    /** 从缓存元数据懒加载映射表；IO 读取后回主线程写入内存表 */
    private suspend fun loadRangesIfNeeded(key: String) {
        if (!cacheEnabled) return
        if (key.isEmpty()) return
        if (!loadedKeys.add(key)) return
        withContext(Dispatchers.Main.immediate) {
            if (!rangesByKey[key].isNullOrEmpty()) return@withContext
            val restored: List<ByteTimeRange>? =
                withContext(Dispatchers.IO) {
                    try {
                        val data =
                            MediaCache
                                .cache(context)
                                .getContentMetadata(key)
                                .get(RANGES_METADATA_KEY, ByteArray(0))
                                ?: return@withContext emptyList<ByteTimeRange>()
                        if (data.isEmpty()) return@withContext emptyList()
                        deserializeRanges(key, data)
                    } catch (_: Exception) {
                        null
                    }
                }
            if (restored == null) {
                loadedKeys.remove(key)
                return@withContext
            }
            if (restored.isNotEmpty()) rangesByKey[key] = restored.toMutableList()
        }
    }

    /** 清空映射表、游标和待处理读取事件 */
    private fun clearRanges() {
        synchronized(readQueueLock) {
            readQueue.clear()
            readFlushScheduled = false
        }
        rangesByKey.clear()
        loadedKeys.clear()
        readCursors.clear()
    }

    /** 高频 DataSource 回调先入队，再短延迟批处理以降低主线程压力 */
    @AnyThread
    private fun enqueueRead(read: ReadChunk) {
        val shouldScheduleFlush =
            synchronized(readQueueLock) {
                readQueue.add(read)
                if (readFlushScheduled) {
                    false
                } else {
                    readFlushScheduled = true
                    true
                }
            }
        if (!shouldScheduleFlush) return
        mainScope.launch {
            delay(READ_BATCH_MS)
            flushReads()
        }
    }

    /** 记录关闭时默认丢弃旧队列；释放路径可强制处理最后一批事件 */
    private fun flushReads(processWhenDisabled: Boolean = false) {
        val reads =
            synchronized(readQueueLock) {
                readFlushScheduled = false
                val snapshot = readQueue.toList()
                readQueue.clear()
                if (!recordingEnabled && !processWhenDisabled) return
                snapshot
            }
        mergeReads(reads).forEach(::addRead)
    }

    private fun writeRangeSnapshot(
        key: String,
        snapshot: List<ByteTimeRange>,
    ) {
        try {
            val data = serializeRanges(snapshot) ?: return
            val mutations = ContentMetadataMutations()
            mutations.set(RANGES_METADATA_KEY, data)
            MediaCache.cache(context).applyContentMetadataMutations(key, mutations)
        } catch (_: Exception) {
        }
    }

    /** 连续读取可合并，但不能跨过精确映射范围，以免覆盖更可靠的数据 */
    private fun mergeReads(reads: List<ReadChunk>): List<ReadChunk> {
        if (reads.size <= 1) return reads
        val accurateRangesByKey =
            rangesByKey.mapValues { (_, mappings) ->
                mappings
                    .asSequence()
                    .filter { it.isExact }
                    .map { ByteSpan(it.byteStart, it.byteEnd) }
                    .toList()
            }
        val sorted =
            reads
                .groupBy { ReadChunkGroup(it.cacheKey, it.durationMs, it.contentLength) }
                .values
                .flatMap { group ->
                    group.sortedWith(
                        compareBy<ReadChunk>(
                            { it.chunkStart },
                            { it.chunkEnd },
                        ),
                    )
                }
        val merged = mutableListOf<ReadChunk>()
        sorted.forEach { read ->
            val previous = merged.lastOrNull()
            if (
                previous != null &&
                previous.cacheKey == read.cacheKey &&
                previous.chunkEnd == read.chunkStart &&
                previous.durationMs == read.durationMs &&
                previous.contentLength == read.contentLength &&
                !overlapsAccurateRange(accurateRangesByKey[read.cacheKey], previous.chunkStart, read.chunkEnd)
            ) {
                merged[merged.lastIndex] =
                    previous.copy(
                        chunkEnd = read.chunkEnd,
                        eventTimeMs = maxOf(previous.eventTimeMs, read.eventTimeMs),
                    )
            } else {
                merged.add(read)
            }
        }
        return merged
    }

    /** 根据字节比例估算时间范围；连续读取沿用上个游标，减少估算跳变 */
    private fun addRead(read: ReadChunk) {
        val durationUs = read.durationMs * 1000L
        val chunkBytes = max(0L, read.chunkEnd - read.chunkStart)
        val deltaUs = ((durationUs.toDouble() * chunkBytes.toDouble()) / read.contentLength.toDouble()).toLong().coerceAtLeast(1L)
        val absoluteStartUs =
            ((read.chunkStart.toDouble() / read.contentLength.toDouble()) * durationUs.toDouble())
                .toLong()
                .coerceIn(0L, durationUs.coerceAtLeast(1L) - 1L)
        val cursor = readCursors[read.cacheKey]
        val isContinuous =
            cursor != null &&
                read.chunkStart >= cursor.lastByteEnd &&
                read.chunkStart - cursor.lastByteEnd <= READ_CONTINUITY_GAP &&
                read.eventTimeMs - cursor.lastEventTimeMs <= READ_CONTINUITY_TIMEOUT_MS
        val startUs =
            if (isContinuous) {
                cursor.lastTimeUs.coerceIn(0L, durationUs.coerceAtLeast(1L) - 1L)
            } else {
                absoluteStartUs
            }
        val endUs = (startUs + deltaUs).coerceAtMost(durationUs)
        val fullyCovered = hasAccurateCover(read.cacheKey, read.chunkStart, read.chunkEnd)
        if (fullyCovered) return
        addRange(
            ByteTimeRange(
                cacheKey = read.cacheKey,
                byteStart = read.chunkStart,
                byteEnd = read.chunkEnd,
                timeStartUs = startUs,
                timeEndUs = max(startUs + 1L, endUs),
                eventTimeMs = read.eventTimeMs,
            ),
        )
        readCursors[read.cacheKey] =
            ReadCursor(
                lastByteEnd = read.chunkEnd,
                lastTimeUs = max(startUs + 1L, endUs),
                lastEventTimeMs = read.eventTimeMs,
            )
    }

    /** 检查指定字节范围是否已被精确映射完全覆盖 */
    private fun hasAccurateCover(
        key: String,
        byteStart: Long,
        byteEnd: Long,
    ): Boolean =
        rangesByKey[key]?.any {
            it.isExact && it.byteStart <= byteStart && it.byteEnd >= byteEnd
        } == true

    /** 检查指定字节范围是否与任一精确映射范围重叠 */
    private fun overlapsAccurateRange(
        ranges: List<ByteSpan>?,
        byteStart: Long,
        byteEnd: Long,
    ): Boolean =
        ranges?.any {
            it.start < byteEnd && it.end > byteStart
        } == true

    /** 在插入位置附近查找并合并可合并的映射条目 */
    private fun mergeAround(
        ranges: MutableList<ByteTimeRange>,
        insertedIndex: Int,
    ) {
        var index = insertedIndex
        while (index > 0 && canMerge(ranges[index - 1], ranges[index])) {
            ranges[index - 1] = merge(ranges[index - 1], ranges[index])
            ranges.removeAt(index)
            index--
        }
        while (index < ranges.lastIndex && canMerge(ranges[index], ranges[index + 1])) {
            ranges[index] = merge(ranges[index], ranges[index + 1])
            ranges.removeAt(index + 1)
        }
    }

    /** 判断两条映射是否可合并：字节范围相邻且时间方向一致 */
    private fun canMerge(
        first: ByteTimeRange,
        second: ByteTimeRange,
    ): Boolean =
        second.byteStart <= first.byteEnd + MERGE_BYTE_GAP &&
            second.timeStartUs >= first.timeStartUs &&
            second.timeEndUs >= first.timeEndUs

    /** 合并两条映射，优先保留精确来源的时间信息 */
    private fun merge(
        first: ByteTimeRange,
        second: ByteTimeRange,
    ): ByteTimeRange {
        val useSecondTimes = second.isExact && !first.isExact
        return first.copy(
            byteEnd = maxOf(first.byteEnd, second.byteEnd),
            timeStartUs =
                if (useSecondTimes) {
                    second.timeStartUs
                } else {
                    first.timeStartUs
                },
            timeEndUs =
                if (useSecondTimes) {
                    second.timeEndUs
                } else {
                    maxOf(first.timeEndUs, second.timeEndUs)
                },
            eventTimeMs = maxOf(first.eventTimeMs, second.eventTimeMs),
            isExact = first.isExact || second.isExact,
        )
    }

    /** 将映射列表序列化为字节数组 */
    private fun serializeRanges(ranges: List<ByteTimeRange>): ByteArray? {
        if (ranges.isEmpty()) return null
        return try {
            val baos = ByteArrayOutputStream()
            DataOutputStream(baos).use { out ->
                out.writeInt(ranges.size)
                ranges.forEach {
                    out.writeLong(it.byteStart)
                    out.writeLong(it.byteEnd)
                    out.writeLong(it.timeStartUs)
                    out.writeLong(it.timeEndUs)
                    out.writeLong(it.eventTimeMs)
                    out.writeInt(if (it.isExact) 1 else 0)
                }
            }
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    /** 从字节数组反序列化映射列表 */
    private fun deserializeRanges(
        key: String,
        data: ByteArray,
    ): List<ByteTimeRange> =
        try {
            DataInputStream(ByteArrayInputStream(data)).use { input ->
                val count = input.readInt().coerceAtLeast(0).coerceAtMost(MAX_RANGES_PER_KEY)
                buildList(count) {
                    repeat(count) {
                        val byteStart = input.readLong()
                        val byteEnd = input.readLong()
                        val timeStartUs = input.readLong()
                        val timeEndUs = input.readLong()
                        val eventTimeMs = input.readLong()
                        val isExact = input.readInt() != 0
                        if (byteEnd > byteStart && timeEndUs > timeStartUs) {
                            add(
                                ByteTimeRange(
                                    cacheKey = key,
                                    byteStart = byteStart,
                                    byteEnd = byteEnd,
                                    timeStartUs = timeStartUs,
                                    timeEndUs = timeEndUs,
                                    eventTimeMs = eventTimeMs,
                                    isExact = isExact,
                                ),
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }

    /** 从加载事件解析缓存键：优先使用 dataSpec.key，否则由 URI 生成 */
    private fun cacheKeyFrom(loadEventInfo: LoadEventInfo): String? {
        loadEventInfo.dataSpec.key
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return loadEventInfo.dataSpec.uri
            .toString()
            .takeIf { it.isNotEmpty() }
            ?.let { MediaCache.key(it) }
    }

    /** 启动缓存轮询协程，周期性调用 [pollSpans] 获取最新缓存分段 */
    private fun startPolling(player: Player) {
        pollingJob?.cancel()
        pollingJob =
            mainScope.launch {
                while (isActive) {
                    val delayMs = pollSpans(player)
                    delay(delayMs)
                }
            }
    }

    /** 保留最近结果，重新启用展示时可立即恢复 */
    private fun setCachedRanges(ranges: List<CachedTimeRange>) {
        latestRanges = ranges
        publishRanges()
    }

    private fun publishRanges() {
        val visibleRanges =
            if (pollingEnabled) {
                latestRanges
            } else {
                emptyList()
            }
        if (_cachedRanges.value == visibleRanges) return
        _cachedRanges.value = visibleRanges
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    companion object {
        private const val RANGES_METADATA_KEY = "video_mapping_table"
        private const val MAX_RANGES_PER_KEY = 100
        private const val MERGE_BYTE_GAP = 16 * 1024L
        private const val READ_CONTINUITY_GAP = 64 * 1024L
        private const val READ_CONTINUITY_TIMEOUT_MS = 5_000L
        private const val READ_BATCH_MS = 50L

        /** 合并相邻分段 */
        private fun mergeNearbyRanges(ranges: List<CachedTimeRange>): List<CachedTimeRange> {
            if (ranges.size <= 1) return ranges
            val sorted = ranges.sortedBy { it.startMs }
            val merged = mutableListOf<CachedTimeRange>()
            var current = sorted.first()
            for (i in 1 until sorted.size) {
                val next = sorted[i]
                if (next.startMs <= current.endMs + 1000) {
                    current = current.copy(endMs = maxOf(current.endMs, next.endMs))
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            return merged
        }
    }
}
