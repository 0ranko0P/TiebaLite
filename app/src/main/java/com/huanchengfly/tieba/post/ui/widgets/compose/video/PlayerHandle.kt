package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.view.Window
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.StateFlow

/** UI 只依赖该接口，不能绕过控制器直接访问播放器核心或播放仲裁器 */
interface PlayerHandle {
    val controllerId: Long
    val syncGroupId: Long
    val state: StateFlow<PlayerUiState>
    val displayMode: StateFlow<PlayerDisplayMode>
    val seekPreview: StateFlow<SeekPreview?>
    val levelChange: StateFlow<LevelChange?>
    val cachedRanges: StateFlow<List<CachedTimeRange>>
    val contentLengthBytes: StateFlow<Long>
    val pipSupported: StateFlow<Boolean>
    val isReleased: Boolean
    val requestPip: () -> Unit

    fun initialize()

    fun release()

    fun play()

    fun replay()

    fun pause()

    fun togglePlaying()

    fun seekTo(position: Long)

    fun seekPreviewTo(position: Long)

    fun seekForward()

    fun seekBackward()

    fun clearQuickSeek()

    fun setSpeed(speed: Float)

    fun speed(): Float

    fun positionMs(): Long

    fun sourceUrl(): String?

    fun setSource(source: PlayerSource)

    fun setProgressTransitioning(transitioning: Boolean)

    fun setThumbnailUrl(url: String?)

    fun showControls(autoHide: Boolean = true)

    fun hideControls()

    fun toggleLock()

    fun toggleLockOverlay()

    fun setSeekPreview(seekPreview: SeekPreview?)

    fun setLevelChange(adjustment: LevelChange?)

    fun bindWindow(window: Window?)

    fun getVolume(): Float

    fun setVolume(level: Float)

    fun getBrightness(): Float

    fun setBrightness(level: Float)

    fun configurePip(
        supported: Boolean,
        onRequest: () -> Unit,
    )

    fun setDisplayMode(mode: PlayerDisplayMode)

    fun pauseForLifecycle(shouldSkip: Boolean = false)

    fun resumeFromLifecycle()

    fun toggleFullscreen()

    fun rotateFullscreen(inLandscape: Boolean)

    fun setFullscreenListener(listener: FullscreenListener?)

    fun prepareFullscreenHandoff()

    fun claimSurface()

    fun releaseSurface()

    fun transferSurfaceToInline()

    fun onPlaybackEvicted(callback: () -> Unit)

    fun clearPlaybackEviction()

    fun stopMediaService()

    fun <T> selectState(filter: (PlayerUiState) -> T): T
}

/** 包内渲染接口；Compose 渲染层通过它读取共享播放器和 Surface 仲裁状态 */
internal interface PlayerRenderer {
    val player: ExoPlayer
    val previewPlayer: Player
    val surfaceOwnerId: StateFlow<Long?>
    val surfaceRenderingEnabled: StateFlow<Boolean>
}
