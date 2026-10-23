package com.huanchengfly.tieba.post.core.media.state

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.compose.state.PlayerStateObserver
import androidx.media3.ui.compose.state.observeState
import com.huanchengfly.tieba.post.core.media.util.MediaUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val CONTROLS_VISIBILITY_TIMEOUT_MS = 3000L

internal val LocalPlayerGestureState = staticCompositionLocalOf<PlayerGestureState?> { null }

internal enum class VerticalGesture {
    BRIGHTNESS,
    VOLUME,
    None,
}

/**
 * Remember the value of [PlayerGestureState] created based on the passed [Player] and launch a
 * coroutine to listen to [Player's][Player] changes. If the [Player] instance changes between
 * compositions, produce and remember a new value.
 */
@Composable
fun rememberPlayerGestureState(player: Player?): PlayerGestureState {
    val coroutineScope = rememberCoroutineScope()
    val state = remember(player, coroutineScope) {
        PlayerGestureState(player, coroutineScope)
    }

    LaunchedEffect(player) { state.observe() }
    LaunchedVideoWindowEffect(gestureState = state)
    return state
}

@Composable
private fun LaunchedVideoWindowEffect(gestureState: PlayerGestureState) {
    val activity = LocalActivity.current ?: throw NullPointerException("Activity not found!")
    val (insetsController, window) = remember(activity) {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } to activity.window
    }

    LaunchedEffect(gestureState.controlsVisible) {
        if (gestureState.controlsVisible) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(window, gestureState.isPlaying) {
        if (gestureState.isPlaying) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/**
 * State that holds gesture interactions and visibility of the player controls.
 *
 * In most cases, this will be created via [rememberPlayerGestureState].
 *
 * @param[player] [Player] object that operates as a state provider.
 * @param scope Coroutine scope whose context is used to launch the gesture visibility update job.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Stable
class PlayerGestureState(
    private val player: Player?,
    private val scope: CoroutineScope,
) {

    var isEnabled by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var controlsVisible: Boolean by mutableStateOf(true)
        private set

    internal var draggingProgress: DraggingProgress? by mutableStateOf(null)

    internal var quickSeekAction: QuickSeekDirection by mutableStateOf(QuickSeekDirection.None)

    internal var verticalGesture: VerticalGesture by mutableStateOf(VerticalGesture.None)

    private var autoHideControlsJob: Job? = null

    private val playerStateObserver: PlayerStateObserver? =
        player?.observeState(
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
            Player.EVENT_IS_PLAYING_CHANGED,
        ) {
            if (player.playbackState == Player.STATE_ENDED) {
                showControls(autoHide = false)
            } else if (isPlaying != player.isPlaying && controlsVisible) {
                autoHideControls(timeout = CONTROLS_VISIBILITY_TIMEOUT_MS / 2)
            }
            isPlaying = player.isPlaying
            isEnabled = player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM) &&
                    player.duration.let { it != C.TIME_UNSET && it > 0 }
        }

    fun showControls(autoHide: Boolean = true) {
        controlsVisible = true
        if (autoHide) {
            autoHideControls()
        } else {
            autoHideControlsJob?.cancel()
        }
    }

    fun hideControls() {
        autoHideControlsJob?.cancel()
        controlsVisible = false
    }

    fun autoHideControls(timeout: Long = CONTROLS_VISIBILITY_TIMEOUT_MS) {
        autoHideControlsJob?.cancel()
        if (timeout > 0 && timeout < Long.MAX_VALUE) {
            autoHideControlsJob = scope.launch {
                delay(timeout)
                controlsVisible = false
            }
        }
    }

    fun quickSeekForward() {
        if (quickSeekAction == QuickSeekDirection.None && isEnabled) {
            val target = (player!!.currentPosition + 10_000).coerceAtMost(player.duration)
            MediaUtil.handleSeekToAction(player, target)
            quickSeekAction = QuickSeekDirection.Forward
        }
    }

    fun quickSeekRewind() {
        if (quickSeekAction == QuickSeekDirection.None && isEnabled) {
            val target = (player!!.currentPosition - 10_000).coerceAtLeast(0)
            MediaUtil.handleSeekToAction(player, target)
            quickSeekAction = QuickSeekDirection.Rewind
        }
    }

    fun onSeekGestureStart(): Pair<Long, Long> = player?.run {
        Util.handlePauseButtonAction(player)
        hideControls()
        if (player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) {
            player.currentPosition to player.duration
        } else {
            null
        }
    } ?: Pair(0, C.TIME_UNSET)

    fun onSeekGesture(positionMs: Long) = MediaUtil.handleSeekToAction(player, positionMs)

    fun onSeekGestureEnd(resume: Boolean) {
        if (resume) Util.handlePlayButtonAction(player)
    }

    /**
     * Subscribes to updates from [Player.Events] and listens to
     * * [Player.EVENT_PLAYBACK_STATE_CHANGED] in order to determine the visibility control panel.
     * * [Player.EVENT_AVAILABLE_COMMANDS_CHANGED] in order to determine whether the UI element
     *   responsible for quick seek gesture should be enabled.
     */
    suspend fun observe() {
        playerStateObserver?.observe()
    }
}