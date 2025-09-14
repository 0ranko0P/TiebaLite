package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.huanchengfly.tieba.post.ui.widgets.compose.LinearProgressIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.getDurationString

@SuppressLint("ComposableLambdaParameterPosition", "ComposableLambdaParameterNaming")
@Composable
fun SeekBar(
    progress: Long,
    max: Long,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    secondaryProgress: Long? = null,
    onSeek: (progress: Long) -> Unit = {},
    onSeekStarted: (startedProgress: Long) -> Unit = {},
    onSeekStopped: (stoppedProgress: Long) -> Unit = {},
    seekerPopup: @Composable () -> Unit = {},
    showSeekerDuration: Boolean = true,
    color: Color = ProgressIndicatorDefaults.linearColor,
    secondaryColor: Color = Color.White.copy(alpha = 0.6f)
) {
    // if there is an ongoing drag, only dragging progress is evaluated.
    // when dragging finishes, given [progress] continues to be used.
    var onGoingDrag by remember { mutableStateOf(false) }
    val indicatorSize = if (onGoingDrag) 24.dp else 16.dp
    val animatedIndicatorSize by animateDpAsState(
        targetValue = indicatorSize,
        animationSpec = tween(durationMillis = 100),
        label = "indicatorSize"
    )

    BoxWithConstraints(modifier = modifier) {
        if (progress >= max) return@BoxWithConstraints

        val boxWidth = constraints.maxWidth.toFloat()

        val percentage = progress.coerceAtMost(max).toFloat() / max.toFloat()

        // Indicator should be at "percentage" but dragging can change that.
        // This state keeps track of current dragging position.
        var indicatorOffsetByDragState by remember { mutableStateOf(Offset.Zero) }

        val finalIndicatorOffset = remember(indicatorOffsetByDragState, percentage) {
            val finalIndicatorPositionX = if (onGoingDrag) {
                indicatorOffsetByDragState.x
            } else {
                percentage * boxWidth
            }
            Offset(x = finalIndicatorPositionX.coerceIn(0f, boxWidth), y = 0f)
        }

        Column {
            // SEEK POPUP
            if (onGoingDrag) {
                var popupSize by remember { mutableStateOf(IntSize.Zero) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .offset {
                            // popup seeker must center the actual seeker position. Therefore, we offset
                            // it negatively to the left.
                            IntOffset(
                                x = (finalIndicatorOffset.x - popupSize.width / 2)
                                    .coerceIn(0f, (boxWidth - popupSize.width)).fastRoundToInt(),
                                y = 0
                            )
                        }
                        .alpha(if (popupSize == IntSize.Zero) 0f else 1f)
                        .onGloballyPositioned {
                            if (popupSize != it.size) {
                                popupSize = it.size
                            }
                        }
                ) {
                    Box(modifier = Modifier.shadow(4.dp)) {
                        seekerPopup()
                    }

                    if (showSeekerDuration) {
                        val indicatorProgressDurationString = getDurationString(
                            ((finalIndicatorOffset.x / boxWidth) * max).toLong(),
                            false
                        )

                        Text(
                            text = indicatorProgressDurationString,
                            style = TextStyle(
                                shadow = Shadow(
                                    blurRadius = 8f,
                                    offset = Offset(2f, 2f)
                                )
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // SECONDARY PROGRESS
                if (secondaryProgress != null) {
                    LinearProgressIndicator(
                        progress = {
                            secondaryProgress.coerceAtMost(max) / max.coerceAtLeast(1L).toFloat()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = secondaryColor,
                        trackColor = MaterialTheme.colorScheme.secondary,
                    )
                }

                // MAIN PROGRESS
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = Color.Transparent,
                )

                // SEEK INDICATOR
                if (enabled) {
                    val draggableState = rememberDraggableState(onDelta = { dx ->
                        indicatorOffsetByDragState = Offset(
                            x = (indicatorOffsetByDragState.x + dx),
                            y = indicatorOffsetByDragState.y
                        )

                        val currentProgress =
                            (indicatorOffsetByDragState.x / boxWidth) * max
                        onSeek(currentProgress.toLong())
                    })

                    Row(modifier = Modifier
                        .matchParentSize()
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            startDragImmediately = true,
                            onDragStarted = { downPosition ->
                                onGoingDrag = true
                                indicatorOffsetByDragState =
                                    indicatorOffsetByDragState.copy(x = downPosition.x)
                                val newProgress =
                                    (indicatorOffsetByDragState.x / boxWidth) * max
                                onSeekStarted(newProgress.toLong())
                            },
                            onDragStopped = {
                                val newProgress =
                                    (indicatorOffsetByDragState.x / boxWidth) * max
                                onSeekStopped(newProgress.toLong())
                                indicatorOffsetByDragState = Offset.Zero
                                onGoingDrag = false
                            }
                        )
                    ) {
                        Indicator(
                            modifier = Modifier
                                .size(animatedIndicatorSize)
                                .offset {
                                    IntOffset(
                                        x = (finalIndicatorOffset.x - indicatorSize.toPx() / 2).fastRoundToInt(),
                                        y = finalIndicatorOffset.y.fastRoundToInt()
                                    )
                                }
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Indicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val radius = size.height / 2
        drawCircle(color, radius)
    }
}