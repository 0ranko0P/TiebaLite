package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.MediaCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/** 断言当前线程为主线程，否则抛出 [IllegalStateException] */
internal fun assertMainThread() {
    check(Looper.myLooper() == Looper.getMainLooper()) { "Must be accessed on main thread" }
}

internal data class PlayerCoreState(
    @field:Player.State
    val playbackState: Int = Player.STATE_IDLE,
    val isPlaying: Boolean = false,
    val quickSeek: QuickSeek = QuickSeek.None,
    val playbackSpeed: Float = 1f,
    val playbackErrorCode: Int? = null,
)

/** 共享播放器核心；只负责 ExoPlayer、缓存和进度，不管理 Compose/Surface owner 等上层策略 */
@MainThread
internal class PlayerCore(
    context: Context,
) {
    private val context: Context = context.applicationContext

    private fun isLoaded(source: PlayerSource?): Boolean = prepared && source != null && loadedSource?.url == source.url

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playbackState = MutableStateFlow(PlayerCoreState())
    val playbackState: StateFlow<PlayerCoreState> = _playbackState.asStateFlow()

    private val _diskCacheSourceActive = MutableStateFlow(false)
    val diskCacheSourceActive: StateFlow<Boolean> = _diskCacheSourceActive.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var preview: ExoPlayer? = null
    private var previewSeekJob: Job? = null
    private var lastPreviewPositionMs: Long = 0L
    private var released: Boolean = false
    private var source: PlayerSource? = null
    private var cacheKey: String? = null
    private var activeMediaCacheKey: String? = null
    private var mediaCacheSourceCurrentlyActive: Boolean = false
    private var previewMediaCacheSourceCurrentlyActive: Boolean = false
    private var diskCacheWriteCurrentlyActive: Boolean = false
    private var loadedSource: PlayerSource? = null
    private var prepared: Boolean = false
    private var resumeOnReady: Boolean = false
    private var playWhenReadyRequested: Boolean = false
    private var prepareOnSetRequested: Boolean = false
    private var needsRetry: Boolean = false

    private val cacheTracker = PlaybackCacheTracker(context)
    private val progressManager =
        PlaybackProgressManager(
            context = context,
            scope = scope,
            enabled = PlayerSettingsStore.state.value.saveVideoProgress,
        )
    private var pendingInitialPositionMs: Long? = null

    val cachedRanges: StateFlow<List<CachedTimeRange>> get() = cacheTracker.cachedRanges
    val contentLengthBytes: StateFlow<Long> get() = cacheTracker.contentLengthBytes
    val videoSize: StateFlow<Pair<Int, Int>> get() = _videoSize.asStateFlow()

    private val _videoSize = MutableStateFlow(0 to 0)

    /** 只同步核心播放状态、尺寸和错误类别；控制层显隐、Surface handoff 等策略由控制器派生 */
    private val listener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (needsRetry) return
                _playbackState.update { it.copy(playbackState = playbackState) }
                if (playbackState == Player.STATE_ENDED) {
                    progressManager.clear()
                    progressManager.stopTracking()
                }
                updateRangePolling()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                progressManager.onIsPlayingChanged(isPlaying)
                updateRangePolling()
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                updateRangePolling()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason != Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    progressManager.onPositionDiscontinuity()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                needsRetry = true
                prepared = false
                loadedSource = null
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        playbackErrorCode = error.errorCode,
                    )
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    _videoSize.value = videoSize.width to videoSize.height
                }
            }
        }

    fun playerOrNull(): ExoPlayer? {
        assertMainThread()
        return exoPlayer
    }

    @OptIn(UnstableApi::class)
    fun player(): ExoPlayer {
        assertMainThread()
        exoPlayer?.let { return it }
        check(!released) { "PlayerCore is released" }
        val settings = PlayerSettingsStore.state.value
        val loadControl =
            DefaultLoadControl
                .Builder()
                .apply {
                    setTargetBufferBytes(settings.forwardBufferSizeMb * 1024 * 1024)
                    if (settings.advancedBufferSettingsEnabled) {
                        val minBufferMs = settings.minBufferMs
                        val maxBufferMs = settings.maxBufferMs.coerceAtLeast(minBufferMs)
                        setBufferDurationsMs(
                            minBufferMs,
                            maxBufferMs,
                            settings.bufferForPlaybackMs.coerceAtMost(minBufferMs),
                            settings.bufferForPlaybackAfterRebufferMs.coerceAtMost(minBufferMs),
                        )
                        setBackBuffer(settings.backBufferDurationSec * 1000, true)
                        setPrioritizeTimeOverSizeThresholds(!settings.prioritizeCacheSize)
                    }
                }.build()
        return ExoPlayer
            .Builder(context.applicationContext)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            ).setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exoPlayer = it }
    }

    @OptIn(UnstableApi::class)
    fun previewPlayer(): ExoPlayer {
        assertMainThread()
        preview?.let { return it }
        check(!released) { "PlayerCore is released" }
        return ExoPlayer
            .Builder(context.applicationContext)
            .build()
            .apply {
                playWhenReady = false
                volume = 0f
            }.also {
                cacheTracker.attachToPreviewPlayer(it)
                preview = it
            }
    }

    /** 重复初始化会先移除旧监听；若核心因全屏/PiP 保活，则按既有播放意图继续工作 */
    @OptIn(UnstableApi::class)
    fun initialize() {
        assertMainThread()
        if (released) return
        val player = player()
        player.removeListener(listener)
        cacheTracker.attachToPlayer(player)
        player.addListener(listener)
        val currentSize = player.videoSize
        if (currentSize.width > 0 && currentSize.height > 0) {
            _videoSize.value = currentSize.width to currentSize.height
        }
        val currentSpeed = _playbackState.value.playbackSpeed
        player.playbackParameters = PlaybackParameters(currentSpeed)
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) {
            resumeOnReady = true
            playWhenReadyRequested = true
            if (isLoaded(source)) {
                updateRangePolling()
                return
            }
        } else {
            resumeOnReady = playWhenReadyRequested
        }
        player.playWhenReady = resumeOnReady
        if ((resumeOnReady || prepareOnSetRequested) && source != null) source?.let { prepare(it) }
        updateRangePolling()
    }

    /** 供全屏入口预热，避免首帧渲染时才准备播放源 */
    fun warmUp() {
        assertMainThread()
        if (released) return
        source?.let {
            if (!isLoaded(it)) prepare(it)
        }
    }

    /** 主渲染端切换前显式清空当前视频输出，避免新旧 Surface 在过渡期同时竞争同一播放器 */
    fun clearVideoOutput() {
        assertMainThread()
        if (released) return
        exoPlayer?.clearVideoSurface()
    }

    @OptIn(UnstableApi::class)
    fun release() {
        assertMainThread()
        if (released) return
        progressManager.saveNow(commit = true)
        progressManager.detach()
        released = true
        val releasingCacheKey = activeMediaCacheKey
        activeMediaCacheKey = null
        mediaCacheSourceCurrentlyActive = false
        previewMediaCacheSourceCurrentlyActive = false
        setDiskCacheWriteActive(false)
        cacheKey = null
        prepared = false
        loadedSource = null
        needsRetry = false
        val player = exoPlayer
        exoPlayer = null
        if (player != null) {
            runCatching {
                player.stop()
                player.removeListener(listener)
                cacheTracker.detachFromPlayer(player)
            }
            runCatching {
                player.release()
            }
        }
        previewSeekJob?.cancel()
        previewSeekJob = null
        runCatching { releasePreviewPlayer() }
        runCatching { cacheTracker.release() }
        releasingCacheKey?.let(MediaCache::unmarkActive)
        scope.cancel()
    }

    /** 播放错误后的首次播放会重新 prepare，避免复用已失败的 MediaSource */
    fun play() {
        assertMainThread()
        if (released) return
        playWhenReadyRequested = true
        resumeOnReady = true
        val player = exoPlayer ?: return
        if (needsRetry) {
            needsRetry = false
            _playbackState.update { it.copy(playbackErrorCode = null) }
            source?.let { prepare(it) }
        } else if (!isLoaded(source)) {
            source?.let { prepare(it) }
        }
        player.playWhenReady = true
    }

    fun pause() {
        assertMainThread()
        if (released) return
        playWhenReadyRequested = false
        resumeOnReady = false
        exoPlayer?.let { it.playWhenReady = false }
        _playbackState.update { it.copy(isPlaying = false) }
    }

    fun seekForward() {
        assertMainThread()
        if (_playbackState.value.quickSeek != QuickSeek.None) return
        val player = exoPlayer ?: return
        val duration = player.duration.takeIf { it > 0 } ?: return
        val fromMs = player.currentPosition.coerceAtLeast(0L)
        val targetMs = (fromMs + 10_000).coerceAtMost(duration)
        player.seekTo(targetMs)
        _playbackState.update { it.copy(quickSeek = QuickSeek.Forward) }
    }

    fun seekBackward() {
        assertMainThread()
        if (_playbackState.value.quickSeek != QuickSeek.None) return
        val player = exoPlayer ?: return
        val fromMs = player.currentPosition.coerceAtLeast(0L)
        val targetMs = (fromMs - 10_000).coerceAtLeast(0L)
        player.seekTo(targetMs)
        _playbackState.update { it.copy(quickSeek = QuickSeek.Rewind) }
    }

    fun clearQuickSeek() {
        assertMainThread()
        _playbackState.update { it.copy(quickSeek = QuickSeek.None) }
    }

    fun seekTo(position: Long) {
        assertMainThread()
        if (released) return
        val player = exoPlayer ?: return
        val durationMs = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val targetMs = position.coerceIn(0L, durationMs)
        player.seekTo(targetMs)
    }

    /** 播放速度写入核心状态，供播放器重建后恢复 */
    fun setSpeed(speed: Float) {
        assertMainThread()
        if (released) return
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    fun speed(): Float {
        assertMainThread()
        return _playbackState.value.playbackSpeed
    }

    fun positionMs(): Long {
        assertMainThread()
        return exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
    }

    /** 切源会重置错误和缓存/预览状态；可选择在播放器已创建时立即准备新源 */
    @OptIn(UnstableApi::class)
    fun setSource(
        source: PlayerSource,
        prepareOnSet: Boolean = true,
    ) {
        assertMainThread()
        if (released) return
        if (this.source?.url != source.url) progressManager.saveNow(commit = true)
        val resolvedInitialPositionMs = progressManager.resolveInitialPosition(source)
        val newCacheKey = MediaCache.key(source.url)
        cacheKey = newCacheKey
        val samePreparedMedia = isLoaded(source)
        pendingInitialPositionMs = if (samePreparedMedia) null else resolvedInitialPositionMs
        this.source = source
        if (!samePreparedMedia) {
            prepared = false
            loadedSource = null
            progressManager.detach()
        }
        needsRetry = false
        prepareOnSetRequested = prepareOnSet
        _playbackState.update {
            it.copy(
                playbackErrorCode = null,
                playbackSpeed =
                    if (samePreparedMedia) {
                        it.playbackSpeed
                    } else {
                        source.initialSpeed
                    },
            )
        }
        cacheTracker.setSource(newCacheKey)
        scope.launch {
            exoPlayer?.let { cacheTracker.pollSpans(it, force = true) }
        }
        updateRangePolling()
        previewSeekJob?.cancel()
        runCatching { releasePreviewPlayer() }
        exoPlayer?.let { player ->
            if (!samePreparedMedia) {
                player.stop()
                mediaCacheSourceCurrentlyActive = false
                setDiskCacheWriteActive(false)
                updateActiveMediaCacheKey(null)
                if (prepareOnSet) prepare(source)
            } else {
                prepareOnSetRequested = false
                if (source.initialPositionKind == InitialPositionKind.Explicit) {
                    val durationMs = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                    player.seekTo(resolvedInitialPositionMs.coerceIn(0L, durationMs))
                }
            }
        }
    }

    private fun refreshActiveMediaCacheKey() {
        val shouldBeActive = mediaCacheSourceCurrentlyActive || previewMediaCacheSourceCurrentlyActive
        updateActiveMediaCacheKey(if (shouldBeActive) cacheKey else null)
    }

    private fun setDiskCacheWriteActive(active: Boolean) {
        if (diskCacheWriteCurrentlyActive == active) return
        diskCacheWriteCurrentlyActive = active
        _diskCacheSourceActive.value = active
    }

    @OptIn(UnstableApi::class)
    private fun updateActiveMediaCacheKey(newCacheKey: String?) {
        if (activeMediaCacheKey == newCacheKey) return
        activeMediaCacheKey?.let(MediaCache::unmarkActive)
        newCacheKey?.let(MediaCache::markActive)
        activeMediaCacheKey = newCacheKey
    }

    fun setRangeCacheEnabled(enabled: Boolean) {
        assertMainThread()
        if (released) return
        cacheTracker.setRangeCacheEnabled(enabled)
        if (enabled) updateRangePolling()
    }

    fun setRangePollingEnabled(enabled: Boolean) {
        assertMainThread()
        if (released) return
        cacheTracker.setRangePollingEnabled(enabled)
        if (enabled) updateRangePolling()
    }

    fun setProgressTransitioning(transitioning: Boolean) {
        assertMainThread()
        if (released) return
        progressManager.setTransitioning(transitioning)
    }

    fun setProgressSavingEnabled(enabled: Boolean) {
        assertMainThread()
        if (released) return
        progressManager.setEnabled(enabled)
    }

    fun sourceUrl(): String? {
        assertMainThread()
        return source?.url
    }

    fun seekPreviewTo(position: Long) {
        assertMainThread()
        if (released) return
        preparePreviewPlayer()
        val seconds = (position / 1000L).toInt()
        val alignedSeconds = (seconds - seconds.rem(2)).toLong()
        val targetMs = alignedSeconds * 1000

        previewSeekJob?.cancel()
        previewSeekJob =
            scope.launch {
                val preview = preview ?: return@launch
                try {
                    // 错误也会唤醒协程，避免等待 ready/buffering 时卡住拖拽
                    withTimeout(5_000L) {
                        val state = preview.playbackState
                        if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                            suspendCancellableCoroutine { cont ->
                                val listener =
                                    object : Player.Listener {
                                        override fun onPlaybackStateChanged(playbackState: Int) {
                                            if (playbackState == Player.STATE_READY ||
                                                playbackState == Player.STATE_BUFFERING
                                            ) {
                                                preview.removeListener(this)
                                                if (cont.isActive) cont.resumeWith(Result.success(Unit))
                                            }
                                        }

                                        override fun onPlayerError(error: PlaybackException) {
                                            preview.removeListener(this)
                                            if (cont.isActive) cont.resumeWith(Result.success(Unit))
                                        }
                                    }
                                preview.addListener(listener)
                                cont.invokeOnCancellation { preview.removeListener(listener) }
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    return@launch
                } catch (_: Exception) {
                    return@launch
                }
                val clampedMs = targetMs.coerceIn(0L, (preview.duration - 1).coerceAtLeast(0L))
                if (lastPreviewPositionMs != clampedMs) {
                    preview.seekTo(clampedMs)
                    lastPreviewPositionMs = clampedMs
                }
            }
    }

    private fun updateRangePolling() {
        cacheTracker.updateRangePolling(exoPlayer)
    }

    /** prepare 前恢复位置和倍速，并按当前设置启用磁盘缓存和缓存追踪 */
    @OptIn(UnstableApi::class)
    private fun prepare(source: PlayerSource) {
        val player = exoPlayer ?: return
        val hasPendingInitialPosition = pendingInitialPositionMs != null
        val initialPositionMs =
            (pendingInitialPositionMs ?: player.currentPosition.takeIf { it > 0L } ?: 0L)
                .coerceAtLeast(0L)
        val playbackSpeed =
            _playbackState.value.playbackSpeed
                .takeIf { speed -> speed > 0f }
                ?: source.initialSpeed
        val diskCacheEnabled = PlayerSettingsStore.state.value.diskCacheEnabled
        player.stop()
        mediaCacheSourceCurrentlyActive = true
        setDiskCacheWriteActive(diskCacheEnabled)
        refreshActiveMediaCacheKey()
        cacheTracker.setRangeCacheEnabled(diskCacheEnabled)
        player.setMediaSource(mediaSource(source))
        progressManager.attach(player, source)
        if (hasPendingInitialPosition || initialPositionMs > 0L) player.seekTo(initialPositionMs)
        pendingInitialPositionMs = null
        player.playbackParameters = PlaybackParameters(playbackSpeed)
        player.playWhenReady = playWhenReadyRequested
        player.prepare()
        prepared = true
        loadedSource = source
        prepareOnSetRequested = false
    }

    @OptIn(UnstableApi::class)
    private fun preparePreviewPlayer() {
        val source = source ?: return
        val preview = previewPlayer()
        if (preview.playbackState == Player.STATE_IDLE) {
            val diskCacheEnabled = PlayerSettingsStore.state.value.diskCacheEnabled
            previewMediaCacheSourceCurrentlyActive = true
            refreshActiveMediaCacheKey()
            // 预览播放器也遵循当前磁盘缓存设置，避免关闭缓存后继续写入
            cacheTracker.setRangeCacheEnabled(diskCacheEnabled)
            preview.setMediaSource(mediaSource(source))
            preview.prepare()
        }
    }

    private fun releasePreviewPlayer() {
        val player = preview ?: return
        preview = null
        lastPreviewPositionMs = 0L
        runCatching { cacheTracker.detachFromPlayer(player) }
        runCatching { player.release() }
        previewMediaCacheSourceCurrentlyActive = false
        refreshActiveMediaCacheKey()
    }

    /** 创建 MediaSource，并按当前设置决定是否写入磁盘缓存 */
    @OptIn(UnstableApi::class)
    private fun mediaSource(source: PlayerSource): MediaSource {
        val diskCacheEnabled = PlayerSettingsStore.state.value.diskCacheEnabled
        return ProgressiveMediaSource
            .Factory(
                MediaCache.dataSourceFactory(
                    context = context,
                    readObserver = cacheTracker.readObserver,
                    disableDiskCache = !diskCacheEnabled,
                ),
            ).createMediaSource(
                MediaItem
                    .Builder()
                    .setUri(source.url)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .apply {
                                setTitle(source.title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.desc_video))
                                setArtist(source.artist?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name))
                                source.artworkUrl
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { setArtworkUri(Uri.parse(it)) }
                            }.build(),
                    ).build(),
            )
    }
}
