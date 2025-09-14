package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.common.Player

@Composable
fun MediaControlButtons(
    modifier: Modifier = Modifier
) {
    val controller = LocalVideoPlayerController.current
    val controlsEnabled by controller.collect { controlsEnabled }

    if (controlsEnabled) {
        Box(
            modifier = modifier.buttonVisibility(controller),
            contentAlignment = Alignment.Center
        ) {
            PlayPauseButton()
        }
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier = Modifier) {
    val controller = LocalVideoPlayerController.current

    val isPlaying by controller.collect { isPlaying }
    val playbackState by controller.collect { playbackState }

    IconButton(
        onClick = { controller.togglePlaying() },
        modifier = modifier
    ) {
        if (isPlaying) {
            ShadowedIcon(icon = Icons.Filled.Pause)
        } else {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    ShadowedIcon(icon = Icons.Filled.Restore)
                }

                Player.STATE_BUFFERING -> {
                    CircularProgressIndicator()
                }

                else -> {
                    ShadowedIcon(icon = Icons.Filled.PlayArrow)
                }
            }
        }
    }
}

// Same with Modifier.alpha() + Modifier.background(), but background is
// animated, draw manually to avoid unnecessary recompositions
private fun Modifier.buttonVisibility(controller: DefaultVideoPlayerController) = composed {
    // Dictates the direction of appear animation.
    // If controlsVisible is true, appear animation needs to be triggered.
    val controlsVisible by controller.collect { controlsVisible }

    val appearAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "ControlButtonAlphaAnimation"
    )

    Modifier
        .drawWithCache {
            onDrawBehind { drawRect(Color.Black.copy(appearAlpha * 0.6f), size = size) }
        }
        .graphicsLayer {
            alpha = appearAlpha
            translationY = if (alpha == 0f) size.height else 0f
        }
}
