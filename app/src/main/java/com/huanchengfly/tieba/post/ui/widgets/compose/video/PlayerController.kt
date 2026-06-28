package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.Context
import android.content.pm.ActivityInfo
import android.view.Window
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.services.PlaybackService
import com.huanchengfly.tieba.post.toastShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** 同一同步组的控制器共享底层播放器；本层把核心状态映射成 UI 状态并协调 Surface/全屏/PiP */
internal class PlayerController(
    private val context: Context,
    private val initialModel: PlayerUiState,
    private val initialDisplayMode: PlayerDisplayMode = PlayerDisplayMode.Inline,
    controllerId: Long = nextControllerId(),
    override val syncGroupId: Long = nextHandleId.getAndIncrement(),
    private val resumePlaybackState: MutableState<Boolean> = mutableStateOf(false),
) : PlayerHandle,
    PlayerRenderer {
    companion object {
        private val nextHandleId = AtomicLong(1)

        fun nextControllerId(): Long = nextHandleId.getAndIncrement()

        @StringRes
        internal fun playbackErrorTextRes(errorCode: Int): Int =
            when (errorCode) {
                2001 -> R.string.playback_error_network_connection_failed
                2002 -> R.string.playback_error_network_connection_timeout
                in 1000..1999 -> R.string.playback_error_misc
                in 2000..2999 -> R.string.playback_error_io
                in 3000..3999 -> R.string.playback_error_parsing
                in 4000..4999 -> R.string.playback_error_decoding
                in 5000..5999 -> R.string.playback_error_audio_track
                in 6000..6999 -> R.string.playback_error_drm
                else -> R.string.playback_error_unknown
            }
    }

    override val controllerId: Long = controllerId

    private var fullscreenListener: FullscreenListener? = null

    private var released: Boolean = false

    private val _displayMode = MutableStateFlow(initialDisplayMode)
    override val displayMode: StateFlow<PlayerDisplayMode> = _displayMode.asStateFlow()

    private val modelState = MutableStateFlow(initialModel)
    private val _seekPreview = MutableStateFlow<SeekPreview?>(null)
    private val _levelChange = MutableStateFlow<LevelChange?>(null)
    private val core: PlayerCore = PlayerSessionRegistry.acquire(syncGroupId, context)
    private val brightnessVolumeController = BrightnessVolumeController(context)
    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var playbackJob: Job? = null
    private var settingsJob: Job? = null
    private var segmentDrawRequested: Boolean = false
    private val controlsAutoHide = AutoHideTimer()
    private val lockOverlayAutoHide = AutoHideTimer()
    private var lastShownPlaybackErrorCode: Int? = null

    private val _pipSupported = MutableStateFlow(false)
    override val pipSupported: StateFlow<Boolean> = _pipSupported.asStateFlow()
    private val surfaceOwnerState = PlayerSessionRegistry.surfaceOwner(syncGroupId)
    private val _surfaceRenderingEnabled = MutableStateFlow(true)

    override var requestPip: () -> Unit = {}
        private set

    private var keepPlaybackOnRelease: Boolean = false

    override val state: StateFlow<PlayerUiState> = modelState
    override val seekPreview: StateFlow<SeekPreview?> = _seekPreview.asStateFlow()
    override val levelChange: StateFlow<LevelChange?> = _levelChange.asStateFlow()
    override val cachedRanges: StateFlow<List<CachedTimeRange>> get() = core.cachedRanges
    override val contentLengthBytes: StateFlow<Long> get() = core.contentLengthBytes
    override val isReleased: Boolean get() = released
    override val surfaceOwnerId: StateFlow<Long?> = surfaceOwnerState
    override val surfaceRenderingEnabled: StateFlow<Boolean> = _surfaceRenderingEnabled.asStateFlow()

    override fun <T> selectState(filter: (PlayerUiState) -> T): T = filter(state.value)

    /** 仅在当前控制器仍持有渲染所有权时清空输出，避免旧节点卸载时误清掉新 Surface */
    private fun clearVideoOutputIfOwner() {
        if (released) return
        if (surfaceOwnerState.value == controllerId || (_displayMode.value == PlayerDisplayMode.Inline && _surfaceRenderingEnabled.value)) {
            core.clearVideoOutput()
        }
    }

    /** 根据同步组 owner 状态同步当前控制器的渲染资格；内嵌实例会在 owner 为空时主动认领。 */
    private fun syncSurfaceRendering() {
        if (released) return
        val ownerId = surfaceOwnerState.value
        if (_displayMode.value == PlayerDisplayMode.Inline && ownerId == null) {
            PlayerSessionRegistry.claimSurface(syncGroupId, controllerId)
            _surfaceRenderingEnabled.value = true
            return
        }
        _surfaceRenderingEnabled.value = ownerId == null || ownerId == controllerId
    }

    override fun seekTo(position: Long) {
        core.seekTo(position)
    }

    override val player: ExoPlayer
        get() = core.player()

    override val previewPlayer: Player
        get() = core.previewPlayer()

    /** 进入全屏前冻结进度过渡并隐藏当前内嵌渲染端，音频意图交由共享核心延续 */
    override fun prepareFullscreenHandoff() {
        assertMainThread()
        if (released) return
        keepPlaybackOnRelease = true
        core.setProgressTransitioning(true)
        clearVideoOutputIfOwner()
        _surfaceRenderingEnabled.value = false
    }

    /** 全屏/PiP 页面显式 claim 后，当前控制器立刻成为同步组的活跃渲染端 */
    override fun claimSurface() {
        PlayerSessionRegistry.claimSurface(syncGroupId, controllerId)
        _surfaceRenderingEnabled.value = true
    }

    /** 当前控制器离场时释放 owner 声明，允许其他容器或后续内嵌实例接手 */
    override fun releaseSurface() {
        clearVideoOutputIfOwner()
        PlayerSessionRegistry.releaseSurface(syncGroupId, controllerId)
        _surfaceRenderingEnabled.value = false
    }

    /** 全屏/PiP 退出时释放当前 owner，由恢复后的内嵌控制器按同步组主动 claim。 */
    override fun transferSurfaceToInline() {
        assertMainThread()
        clearVideoOutputIfOwner()
        _surfaceRenderingEnabled.value = false
        PlayerSessionRegistry.transferSurfaceToInline(syncGroupId)
    }

    override fun setSource(source: PlayerSource) {
        val prepareOnSet = _displayMode.value != PlayerDisplayMode.Inline || state.value.isPlaying
        core.setSource(source, prepareOnSet = prepareOnSet)
    }

    override fun setProgressTransitioning(transitioning: Boolean) {
        core.setProgressTransitioning(transitioning)
    }

    override fun setSpeed(speed: Float) {
        core.setSpeed(speed)
    }

    override fun speed(): Float = core.speed()

    override fun positionMs(): Long = core.positionMs()

    override fun sourceUrl(): String? = core.sourceUrl()

    override fun seekForward() {
        core.seekForward()
    }

    override fun seekBackward() {
        core.seekBackward()
    }

    override fun seekPreviewTo(position: Long) {
        core.seekPreviewTo(position)
    }

    /** 全屏模式先预热播放源；随后持续把核心状态同步到 UI 状态流 */
    override fun initialize() {
        assertMainThread()
        if (released) return
        syncSurfaceRendering()
        if (_displayMode.value == PlayerDisplayMode.Fullscreen) core.warmUp()
        playbackJob?.cancel()
        playbackJob =
            controllerScope.launch {
                launch {
                    core.diskCacheSourceActive.collect {
                        refreshSegmentPolling()
                    }
                }
                launch {
                    core.videoSize.collect { (w, h) ->
                        if (w > 0 && h > 0) modelState.update { it.copy(videoWidth = w, videoHeight = h) }
                    }
                }
                core.playbackState.collect { playbackState ->
                    val isEnded = playbackState.playbackState == Player.STATE_ENDED
                    val playbackErrorCode = playbackState.playbackErrorCode
                    val hasPlaybackError = playbackErrorCode != null
                    val hasResumedFromEnded =
                        !isEnded && !hasPlaybackError &&
                            (playbackState.isPlaying || playbackState.playbackState == Player.STATE_BUFFERING)
                    modelState.update { current ->
                        current.copy(
                            playbackState = playbackState.playbackState,
                            isPlaying = playbackState.isPlaying,
                            quickSeek = playbackState.quickSeek,
                            playbackSpeed = playbackState.playbackSpeed,
                            hasPlaybackError = hasPlaybackError,
                            wasEnded =
                                when {
                                    isEnded -> true
                                    current.wasEnded && hasResumedFromEnded -> false
                                    else -> current.wasEnded
                                },
                        )
                    }
                    if (playbackErrorCode != null) {
                        showPlaybackErrorToast(playbackErrorCode)
                    } else {
                        lastShownPlaybackErrorCode = null
                    }
                    if (isEnded || hasPlaybackError) showControls(autoHide = false)
                }
            }
        core.initialize()
    }

    private fun showPlaybackErrorToast(errorCode: Int) {
        if (lastShownPlaybackErrorCode == errorCode) return
        lastShownPlaybackErrorCode = errorCode
        context.toastShort("[$errorCode] ${context.getString(playbackErrorTextRes(errorCode))}")
    }

    /** 内嵌释放后停播；全屏/PiP handoff 或后台播放时允许共享核心与前台服务继续存活 */
    override fun release() {
        assertMainThread()
        if (released) return
        val isInline = _displayMode.value == PlayerDisplayMode.Inline
        val shouldPauseCore = isInline && !keepPlaybackOnRelease
        val backgroundPlay = state.value.settings.backgroundPlayEnabled && !isInline
        val shouldKeepService = keepPlaybackOnRelease || (backgroundPlay && PlaybackService.isActiveGroup(syncGroupId))
        val shouldStopService = !shouldKeepService && !keepPlaybackOnRelease
        if (shouldPauseCore) core.pause()
        if (shouldStopService) stopMediaService()
        clearVideoOutputIfOwner()
        released = true
        _surfaceRenderingEnabled.value = false
        PlayerSessionRegistry.releaseSurface(syncGroupId, controllerId)
        PlayerSessionRegistry.releasePlayback(controllerId)
        brightnessVolumeController.restoreBrightness()
        controlsAutoHide.cancel()
        lockOverlayAutoHide.cancel()
        playbackJob?.cancel()
        settingsJob?.cancel()
        controllerScope.cancel()
        brightnessVolumeController.bindWindow(null)
        PlayerSessionRegistry.release(syncGroupId, keepAlive = shouldKeepService)
    }

    /** 全屏/PiP 播放前声明播放所有权，防止多个控制器同时恢复 */
    override fun play() {
        assertMainThread()
        if (released) return
        play(replayFromStart = state.value.wasEnded)
    }

    override fun replay() {
        assertMainThread()
        if (released) return
        play(replayFromStart = true)
    }

    private fun play(replayFromStart: Boolean) {
        if (replayFromStart) {
            core.seekTo(0L)
            modelState.update { it.copy(wasEnded = false) }
        }
        if (_displayMode.value != PlayerDisplayMode.Inline) PlayerSessionRegistry.claimPlayback(controllerId)
        core.play()
        maybeStartMediaService()
        scheduleControlsAutoHideIfPlaying()
    }

    override fun pause() {
        assertMainThread()
        if (released) return
        core.pause()
        controlsAutoHide.cancel()
        showControls(autoHide = false)
    }

    override fun togglePlaying() {
        assertMainThread()
        if (state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /** 锁定状态下忽略展示请求，避免误触后显示完整控制层 */
    override fun showControls(autoHide: Boolean) {
        assertMainThread()
        if (state.value.isLocked) return
        modelState.update { it.copy(controlsShown = true) }
        refreshSegmentPolling()
        if (autoHide) {
            scheduleControlsAutoHideIfPlaying()
        } else {
            controlsAutoHide.cancel()
        }
    }

    override fun hideControls() {
        assertMainThread()
        modelState.update { it.copy(controlsShown = false) }
        refreshSegmentPolling()
    }

    override fun toggleLock() {
        assertMainThread()
        val locked = !state.value.isLocked
        modelState.update {
            it.copy(
                isLocked = locked,
                controlsShown = !locked,
                lockOverlayShown = locked,
            )
        }
        refreshSegmentPolling()
        if (locked) {
            controlsAutoHide.cancel()
            scheduleLockOverlayAutoHide()
        } else {
            lockOverlayAutoHide.cancel()
            scheduleControlsAutoHideIfPlaying()
        }
    }

    override fun toggleLockOverlay() {
        assertMainThread()
        if (!state.value.isLocked) return
        val visible = !state.value.lockOverlayShown
        modelState.update { it.copy(lockOverlayShown = visible) }
        if (visible) {
            scheduleLockOverlayAutoHide()
        } else {
            lockOverlayAutoHide.cancel()
        }
    }

    override fun setThumbnailUrl(url: String?) {
        assertMainThread()
        modelState.update { it.copy(thumbnail = url) }
    }

    override fun setSeekPreview(seekPreview: SeekPreview?) {
        assertMainThread()
        _seekPreview.value = seekPreview
    }

    override fun setLevelChange(adjustment: LevelChange?) {
        assertMainThread()
        _levelChange.value = adjustment
    }

    override fun clearQuickSeek() {
        core.clearQuickSeek()
    }

    override fun bindWindow(window: Window?) {
        assertMainThread()
        brightnessVolumeController.bindWindow(window)
    }

    override fun getVolume(): Float {
        assertMainThread()
        return brightnessVolumeController.getVolume()
    }

    override fun setVolume(level: Float) {
        assertMainThread()
        brightnessVolumeController.setVolume(level)
    }

    override fun getBrightness(): Float {
        assertMainThread()
        return brightnessVolumeController.getBrightness()
    }

    override fun setBrightness(level: Float) {
        assertMainThread()
        brightnessVolumeController.setBrightness(level)
    }

    /** 设置变更会同步 UI，并刷新后台播放服务和分段缓存显示请求 */
    internal fun observeSettings() {
        assertMainThread()
        settingsJob?.cancel()
        settingsJob =
            controllerScope.launch {
                PlayerSettingsStore.state.collect { settings ->
                    val prev = modelState.value
                    modelState.update { it.copy(settings = settings) }
                    if (settings.saveVideoProgress != prev.settings.saveVideoProgress) {
                        core.setProgressSavingEnabled(settings.saveVideoProgress)
                    }
                    if (settings.backgroundPlayEnabled != prev.settings.backgroundPlayEnabled) {
                        if (settings.backgroundPlayEnabled) {
                            maybeStartMediaService()
                        } else {
                            stopMediaService()
                        }
                    }
                    // 用户开关只表达显示请求；实际展示还取决于核心是否正在使用磁盘缓存源
                    val drawSegmentEnabled = settings.diskCacheEnabled && settings.drawSegmentedCache
                    if (drawSegmentEnabled != segmentDrawRequested) {
                        segmentDrawRequested = drawSegmentEnabled
                        refreshSegmentPolling()
                    }
                }
            }
    }

    private fun isForegroundServiceStartEligible(): Boolean {
        if (!state.value.settings.backgroundPlayEnabled) return false
        return core.playerOrNull() != null
    }

    private fun maybeStartMediaService() {
        if (!isForegroundServiceStartEligible()) return
        startMediaService()
    }

    private fun startMediaService() {
        try {
            PlaybackService.start(context, syncGroupId)
        } catch (e: IllegalStateException) {
        }
    }

    override fun stopMediaService() {
        val activePlayer = core.playerOrNull() ?: return
        PlaybackService.stopFor(context, activePlayer)
    }

    override fun setDisplayMode(mode: PlayerDisplayMode) {
        assertMainThread()
        _displayMode.value = mode
        if (mode == PlayerDisplayMode.Inline) {
            syncSurfaceRendering()
        } else {
            _surfaceRenderingEnabled.value = surfaceOwnerState.value.let { it == null || it == controllerId }
        }
        if (state.value.settings.backgroundPlayEnabled) maybeStartMediaService()
    }

    override fun pauseForLifecycle(shouldSkip: Boolean) {
        assertMainThread()
        if (released || shouldSkip) return
        val current = state.value
        if (!current.settings.backgroundPlayEnabled) {
            resumePlaybackState.value = current.isPlaying
            pause()
        }
    }

    override fun resumeFromLifecycle() {
        assertMainThread()
        if (released) return
        if (resumePlaybackState.value) {
            resumePlaybackState.value = false
            play()
        }
    }

    override fun configurePip(
        supported: Boolean,
        onRequest: () -> Unit,
    ) {
        assertMainThread()
        _pipSupported.value = supported
        requestPip = onRequest
    }

    override fun toggleFullscreen() {
        assertMainThread()
        fullscreenListener?.onFullscreenCommand(FullscreenCommand.Toggle)
    }

    override fun rotateFullscreen(inLandscape: Boolean) {
        assertMainThread()
        val orientation = if (inLandscape) ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        fullscreenListener?.onFullscreenCommand(FullscreenCommand.Rotate(orientation))
    }

    override fun setFullscreenListener(listener: FullscreenListener?) {
        assertMainThread()
        fullscreenListener = listener
    }

    override fun onPlaybackEvicted(callback: () -> Unit) {
        PlayerSessionRegistry.onPlaybackEvicted(controllerId, callback)
    }

    override fun clearPlaybackEviction() {
        PlayerSessionRegistry.clearPlaybackEviction(controllerId)
    }

    /** 刷新分段缓存轮询：仅当用户请求展示、核心正在使用磁盘缓存源、且控制层可见未锁定时启用轮询 */
    private fun refreshSegmentPolling() {
        core.setRangePollingEnabled(
            segmentDrawRequested &&
                core.diskCacheSourceActive.value &&
                state.value.controlsShown &&
                !state.value.isLocked,
        )
    }

    private fun scheduleControlsAutoHideIfPlaying() {
        controlsAutoHide.schedule {
            if (state.value.isPlaying) hideControls()
        }
    }

    private fun scheduleLockOverlayAutoHide() {
        lockOverlayAutoHide.schedule {
            modelState.update { it.copy(lockOverlayShown = false) }
        }
    }

    /** 自动隐藏计时器 */
    private inner class AutoHideTimer(
        private val delayMs: Long = 4000L,
    ) {
        private var job: Job? = null

        fun schedule(onFire: suspend () -> Unit) {
            cancel()
            job =
                controllerScope.launch {
                    delay(delayMs)
                    onFire()
                }
        }

        fun cancel() {
            job?.cancel()
            job = null
        }
    }
}
