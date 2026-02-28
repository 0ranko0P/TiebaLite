package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.ui.compose.PlayerSurface
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.getDurationString

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier
) {
    val controller = LocalVideoPlayerController.current
    val videoPlayerUiState by controller.collect()

    with(videoPlayerUiState) {
        VideoSeekBar(
            progress = { currentPosition / duration.toFloat() },
            enabled = controlsVisible && controlsEnabled,
            modifier = modifier,
            onSeek = { seekProgress ->
                controller.showControls(autoHide = false)
                controller.previewSeekTo(position = (seekProgress * duration).toLong())
            },
            onSeekStopped = { stoppedProgress ->
                controller.showControls(autoHide = true)
                controller.seekTo(position = (stoppedProgress * duration).toLong())
            },
            secondaryProgress = { secondaryProgress / duration.toFloat() },
            seekerPopup = {
                PlayerSurface(
                    player = controller.previewExoPlayer,
                    modifier = Modifier
                        .height(48.dp)
                        .width(48.dp * videoSize.first / videoSize.second)
                        .background(Color.DarkGray)
                )
            },
            seekerDurationProvider = { seekProgress ->
                getDurationString((seekProgress * duration).toLong(), negativePrefix = false)
            }
        )
    }
}