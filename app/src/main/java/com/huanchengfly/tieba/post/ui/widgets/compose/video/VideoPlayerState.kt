package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.os.Parcelable
import androidx.media3.common.Player
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoPlayerState(
    val thumbnailUrl: String? = null,
    val startedPlay: Boolean = false,
    val isPlaying: Boolean = false,
    val controlsVisible: Boolean = false,
    val controlsEnabled: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val duration: Long = 1L,
    val currentPosition: Long = 1L,
    val secondaryProgress: Long = 1L,
    val videoSize: Pair<Float, Float> = 1920f to 1080f,
    val draggingProgress: DraggingProgress? = null,
    @Player.State
    val playbackState: Int = Player.STATE_IDLE,
    val quickSeekAction: QuickSeekAction = QuickSeekAction.none(),
    val isFullScreen: Boolean = false
) : Parcelable