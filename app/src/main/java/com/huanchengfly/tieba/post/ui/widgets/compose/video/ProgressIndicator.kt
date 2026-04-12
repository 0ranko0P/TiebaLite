package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.ProgressStateWithTickCount
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.getDurationString
import kotlin.math.roundToLong

@UnstableApi
@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    state: ProgressStateWithTickCount,
    durationMs: Long,
    videoSize: Size?,
) {
    val controller = LocalVideoPlayerController.current
    val videoControlEnabled by controller.collect {
        controlsVisible && controlsEnabled
    }

    VideoSeekBar(
        progress = { state.currentPositionProgress },
        enabled = videoControlEnabled,
        modifier = modifier,
        onSeek = { seekProgress ->
            controller.showControls(autoHide = false)
            controller.previewSeekTo(position = (seekProgress * durationMs).roundToLong())
        },
        onSeekStopped = { stoppedProgress ->
            controller.showControls(autoHide = true)
            controller.seekTo(position = (stoppedProgress * durationMs).roundToLong())
        },
        secondaryProgress = { state.bufferedPositionProgress },
        seekerPopup = {
            val aspectRatio = videoSize?.run { width / height } ?: return@VideoSeekBar
            PlayerSurface(
                player = controller.previewExoPlayer,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp * aspectRatio)
                    .background(Color.DarkGray),
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW
            )
        },
        seekerDurationProvider = { seekProgress ->
            getDurationString((seekProgress * durationMs).roundToLong(), negativePrefix = false)
        }
    )
}