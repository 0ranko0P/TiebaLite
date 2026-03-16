package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.MediaCache
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.FlowDebouncer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface OnFullScreenModeChangedListener {
    fun onFullScreenModeChanged()
}

internal class DefaultVideoPlayerController(
    private val context: Context,
    private val initialState: VideoPlayerState,
) : VideoPlayerController {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val released = AtomicBoolean(false)

    private var fullScreenModeChangedListener: OnFullScreenModeChangedListener? = null

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<VideoPlayerState>
        get() = _state.asStateFlow()

    inline fun <T> currentState(filter: (VideoPlayerState) -> T): T {
        return filter(_state.value)
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun <T> collect(filter: VideoPlayerState.() -> T): State<T> {
        return remember(filter) {
            _state.map { it.filter() }
        }.collectAsState(
            initial = _state.value.filter()
        )
    }

    private lateinit var source: VideoPlayerSource

    private var autoHideControllerJob: Job? = null

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.set {
                copy(playbackState = playbackState)
            }
            if (playbackState == Player.STATE_ENDED) {
                showControls(autoHide = false)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.set {
                copy(isPlaying = isPlaying)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            context.toastShort(context.getString(R.string.error_tip) + error.errorCodeName)
        }
    }

    /**
     * Internal exoPlayer instance
     */
    private val _exoPlayer: Lazy<ExoPlayer> = lazy { createExoPlayer() }
    val exoPlayer: ExoPlayer by _exoPlayer

    private fun createExoPlayer() = ExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = initialState.isPlaying
            addListener(playerListener)
        }

    /**
     * Not so efficient way of showing preview in video slider.
     */
    private val _previewExoPlayer: Lazy<ExoPlayer> = lazy { createPreviewExoPlayer() }
    val previewExoPlayer: ExoPlayer by _previewExoPlayer

    private fun createPreviewExoPlayer() = ExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = false
        }

    private val previewSeekDebouncer = FlowDebouncer<Long>(200L)

    init {
        exoPlayer.playWhenReady = initialState.isPlaying

        coroutineScope.launch {
            previewSeekDebouncer.collect { position ->
                previewExoPlayer.seekTo(position)
            }
        }
    }

    fun initialize() {
        require(!released.get())
        val currentState = _state.value
        exoPlayer.playWhenReady = currentState.isPlaying
        if (this::source.isInitialized) {
            setSource(source)
        }
        prepare()
    }

    override fun play() {
        Util.handlePlayButtonAction(exoPlayer)
        autoHideControls()
    }

    override fun pause() {
        if (exoPlayer.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            exoPlayer.playWhenReady = false
        }
    }

    override fun togglePlaying() {
        if (Util.shouldShowPlayButton(exoPlayer)) play() else pause()
    }

    override fun quickSeekForward() {
        if (_state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(target)
        _state.set { copy(quickSeekAction = QuickSeekAction.forward()) }
    }

    override fun quickSeekRewind() {
        if (_state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
        exoPlayer.seekTo(target)
        _state.set { copy(quickSeekAction = QuickSeekAction.rewind()) }
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun setSource(source: VideoPlayerSource) {
        this.source = source
        prepare()
    }

    fun enableGestures(isEnabled: Boolean) {
        _state.set { copy(gesturesEnabled = isEnabled) }
    }

    fun enableControls(enabled: Boolean) {
        _state.set { copy(controlsEnabled = enabled) }
    }

    fun showControls(autoHide: Boolean = true) {
        _state.set { copy(controlsVisible = true) }
        if (autoHide) {
            autoHideControls()
        } else {
            cancelAutoHideControls()
        }
    }

    private fun cancelAutoHideControls() {
        autoHideControllerJob?.cancel()
    }

    private fun autoHideControls() {
        cancelAutoHideControls()
        autoHideControllerJob = coroutineScope.launch {
            delay(2000)
            hideControls()
        }
    }

    fun hideControls() {
        _state.set { copy(controlsVisible = false) }
    }

    fun setDraggingProgress(draggingProgress: DraggingProgress?) {
        _state.set { copy(draggingProgress = draggingProgress) }
    }

    fun setQuickSeekAction(quickSeekAction: QuickSeekAction) {
        _state.set { copy(quickSeekAction = quickSeekAction) }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun prepare() {
        fun createVideoSource(): MediaSource {
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)

            return when (val source = source) {
                is VideoPlayerSource.Raw -> {
                    val uri = Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .path(source.resId.toString())
                        .build()

                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri))
                }

                is VideoPlayerSource.Network -> {
                    ProgressiveMediaSource.Factory(MediaCache.Factory(context))
                        .createMediaSource(MediaItem.fromUri(source.url))
                }
            }
        }

        exoPlayer.setMediaSource(createVideoSource())
        previewExoPlayer.setMediaSource(createVideoSource())

        exoPlayer.prepare()
        previewExoPlayer.prepare()
    }

    fun previewSeekTo(position: Long) {
        // position is very accurate. Thumbnail doesn't have to be.
        // Roll to the nearest "even" integer.
        val seconds = position.toInt() / 1000
        val nearestEven = (seconds - seconds.rem(2)).toLong()
        coroutineScope.launch {
            previewSeekDebouncer.put(nearestEven * 1000)
        }
    }

    override fun reset() {
        exoPlayer.stop()
        previewExoPlayer.stop()
    }

    override fun release() {
        coroutineScope.cancel()
        if (released.compareAndSet(false, true)) {
            _exoPlayer.release()
            _previewExoPlayer.release()
        }
    }

    fun setFullScreenModeChangedListener(listener: OnFullScreenModeChangedListener?) {
        this.fullScreenModeChangedListener = listener
    }

    override fun supportFullScreen(): Boolean {
        return fullScreenModeChangedListener != null
    }

    override fun toggleFullScreen() {
        fullScreenModeChangedListener?.onFullScreenModeChanged()
    }

    companion object {
        private fun Lazy<ExoPlayer>.release() {
            if (isInitialized()) {
                value.release()
            }
        }
    }
}