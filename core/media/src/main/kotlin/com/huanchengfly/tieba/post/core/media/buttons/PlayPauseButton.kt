package com.huanchengfly.tieba.post.core.media.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.sharp.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.handlePlayPauseButtonAction
import androidx.media3.common.util.Util.shouldEnablePlayPauseButton
import androidx.media3.common.util.Util.shouldShowPlayButton
import androidx.media3.ui.compose.state.PlayerStateObserver
import androidx.media3.ui.compose.state.observeState
import com.huanchengfly.tieba.post.core.media.R
import androidx.media3.ui.compose.material3.R as media3UiR

/**
 * A Material3 [IconButton][androidx.compose.material3.IconButton] that plays or pauses the current
 * media item.
 *
 * When clicked, it will pause the [player] if it's currently playing, or play it otherwise. The
 * button's state (e.g., whether it's enabled and the current play/pause icon) is managed by a
 * [androidx.media3.ui.compose.state.PlayPauseButtonState] instance derived from the provided [player].
 *
 * @param player The [Player] to control.
 * @param modifier The [Modifier] to be applied to the button.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconButtonColors].
 * @param onClick The optional action to be performed when the button is clicked.
 */
@Composable
internal fun PlayPauseButton(
    player: Player?,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    onClick: (() -> Unit)? = null,
) {
    val state = rememberPlayPauseButtonState(player)
    IconButton(
        onClick = {
            state.onClick()
            onClick?.invoke()
        },
        modifier = modifier,
        enabled = state.isEnabled,
        colors = colors
    ) {
        with(state) {
            when {
                !isEnabled || playbackState == Player.STATE_BUFFERING -> CircularProgressIndicator()

                playbackState == Player.STATE_ENDED -> Icon(
                    imageVector = Icons.Sharp.Replay,
                    contentDescription = stringResource(R.string.btn_replay),
                    modifier = Modifier.size(36.dp),
                )

                else -> Icon(
                    imageVector = if (showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = stringResource(
                        id = if (showPlay) media3UiR.drawable.media3_icon_pause else media3UiR.string.playpause_button_pause
                    ),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

/**
 * Remembers the value of [PlayPauseButtonState] created based on the passed [Player] and launch a
 * coroutine to listen to [Player's][Player] changes. If the [Player] instance changes between
 * compositions, produce and remember a new value.
 */
@Composable
private fun rememberPlayPauseButtonState(player: Player?): PlayPauseButtonState {
    val playPauseButtonState = remember(player) { PlayPauseButtonState(player) }
    LaunchedEffect(player) { playPauseButtonState.observe() }
    return playPauseButtonState
}

/**
 * State that converts the necessary information from the [Player] to correctly deal with a UI
 * component representing a PlayPause button.
 *
 * @property[isEnabled] true if [player] is not `null`, [Player.COMMAND_PLAY_PAUSE] is available and
 *   we have something in the [Timeline][androidx.media3.common.Timeline] to play. See
 *   [shouldEnablePlayPauseButton] for more details.
 * @property[playbackState] the current playback [State][Player.State] of the player.
 */
@androidx.annotation.OptIn(UnstableApi::class)
private class PlayPauseButtonState(private val player: Player?) {
    var isEnabled by mutableStateOf(false)
        private set

    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set

    var showPlay by mutableStateOf(true)
        private set

    private val playerStateObserver: PlayerStateObserver? =
        player?.observeState(
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_PLAY_WHEN_READY_CHANGED,
            Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
        ) {
            isEnabled = shouldEnablePlayPauseButton(player)
            playbackState = player.playbackState
            showPlay = shouldShowPlayButton(player)
        }

    /**
     * Handles the interaction with the PlayPause button according to the current state of the
     * [Player].
     *
     * The [Player] update that follows can take a form of [Player.play], [Player.pause],
     * [Player.prepare] or [Player.seekToDefaultPosition].
     *
     * It will have no effect if no suitable player method is available to handle the play request.
     *
     * @see [androidx.media3.common.util.Util.handlePlayButtonAction]
     * @see [androidx.media3.common.util.Util.handlePauseButtonAction]
     * @see [androidx.media3.common.util.Util.shouldShowPlayButton]
     * @see [androidx.media3.common.Player.COMMAND_PLAY_PAUSE]
     * @see [androidx.media3.common.Player.COMMAND_GET_CURRENT_MEDIA_ITEM]
     */
    fun onClick() {
        handlePlayPauseButtonAction(player)
    }

    /**
     * Subscribes to updates from [Player.Events] and listens to
     * * [Player.EVENT_PLAYBACK_STATE_CHANGED] and [Player.EVENT_PLAY_WHEN_READY_CHANGED] in order to
     *   determine whether a play or a pause button should be presented on a UI element for playback
     *   control.
     * * [Player.EVENT_AVAILABLE_COMMANDS_CHANGED] in order to determine whether the button should be
     *   enabled, i.e. respond to user input.
     */
    suspend fun observe() {
        playerStateObserver?.observe()
    }
}
