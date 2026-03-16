package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.os.Parcelable
import androidx.media3.common.Player
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoPlayerState(
    val thumbnailUrl: String? = null,
    val isPlaying: Boolean = false,
    val controlsVisible: Boolean = false,
    val controlsEnabled: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val draggingProgress: DraggingProgress? = null,
    @field:Player.State
    val playbackState: Int = Player.STATE_IDLE,
    val quickSeekAction: QuickSeekAction = QuickSeekAction.none(),
    val isFullScreen: Boolean = false
) : Parcelable