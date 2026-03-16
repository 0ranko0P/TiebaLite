package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.TrackStopIndicatorSize
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.theme.FloatProducer
import kotlin.math.max
import kotlin.math.min

@Stable
private fun SliderColors.trackColor(enabled: Boolean, active: Boolean): Color =
    if (enabled) {
        if (active) activeTrackColor else inactiveTrackColor
    } else {
        if (active) disabledActiveTrackColor else disabledInactiveTrackColor
    }

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
fun VideoSeekBar(
    progress: FloatProducer,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondaryProgress: FloatProducer? = null,
    onSeek: (progress: Float) -> Unit = {},
    onSeekStopped: (stoppedProgress: Float) -> Unit = {},
    seekerPopup: @Composable () -> Unit = {},
    seekerDurationProvider: ((seekProgress: Float) -> String)? = null,
) {
    // if there is an ongoing drag, only dragging progress is evaluated.
    // when dragging finishes, given [progress] continues to be used.
    var onGoingDrag by remember { mutableStateOf(false) }
    var draggingProgress by remember { mutableFloatStateOf(0f) }

    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            when (it) {
                is DragInteraction.Start -> onGoingDrag = true
                is DragInteraction -> onGoingDrag = false
            }
        }
    }

    val state = remember {
        SliderState(onValueChangeFinished = { onSeekStopped(draggingProgress) })
    }
    state.onValueChange = {
        draggingProgress = it
        if (onGoingDrag) onSeek(it)
    }

    val bufferState = secondaryProgress?.let {
        remember { SliderState(onValueChangeFinished = null) }
    }
    bufferState?.value = secondaryProgress()

    if (onGoingDrag) {
        state.value = draggingProgress
    } else {
        state.value = progress()
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Column {
            // SEEK POPUP
            if (onGoingDrag) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (size == Size.Zero) 0f else 1f
                            val boxWidth = this@BoxWithConstraints.constraints.maxWidth
                            // popup seeker must center the actual seeker position. Therefore, we offset
                            // it negatively to the left.
                            translationX = (draggingProgress * boxWidth - size.width / 2)
                                .coerceIn(0f, (boxWidth - size.width))
                        }
                ) {
                    Box(modifier = Modifier.shadow(4.dp)) {
                        seekerPopup()
                    }

                    if (seekerDurationProvider != null) {
                        Text(
                            text = seekerDurationProvider(draggingProgress),
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

            val sliderColors = SliderDefaults.colors()
            Slider(
                state = state,
                enabled = enabled,
                colors = sliderColors,
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = sliderColors,
                        enabled = enabled,
                        thumbSize = ThumbSize
                    )
                },
                track = { sliderState ->
                    Track(sliderState, bufferValue = secondaryProgress, colors = sliderColors)
                }
            )
        }
    }
}

/**
 * The Default track for [VideoSeekBar]
 *
 * @param sliderState [SliderState] which is used to obtain the current active track.
 * @param bufferValue current buffer track
 * @param modifier the [Modifier] to be applied to the track.
 * @param enabled controls the enabled state of this slider. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param colors [SliderColors] that will be used to resolve the colors used for this track in
 *   different states. See [SliderDefaults.colors].
 */
@Composable
private fun Track(
    sliderState: SliderState,
    bufferValue: FloatProducer?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
) {
    TrackImpl(
        sliderState = sliderState,
        bufferValue = bufferValue,
        trackCornerSize = Dp.Unspecified,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        drawStopIndicator = {
            drawStopIndicator(
                offset = it,
                color = colors.trackColor(enabled, active = true),
                size = TrackStopIndicatorSize,
            )
        },
        thumbTrackGapSize = ThumbTrackGapSize,
        trackInsideCornerSize = TrackInsideCornerSize,
        isCentered = false,
    )
}

/**
 * compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Slider.kt
 *
 * commit 7631f88 'Merge "BasicTextField min size performance optimizations" into androidx-main'
 * on branch androidx-main
 *
 * 0Ranko0p changes:
 *   1. Drop drawTick (unnecessary for a VideoSeekBar)
 *   2. Add video buffer track
 */
@Composable
private fun TrackImpl(
    sliderState: SliderState,
    bufferValue: FloatProducer?,
    trackCornerSize: Dp,
    modifier: Modifier,
    enabled: Boolean,
    colors: SliderColors,
    drawStopIndicator: (DrawScope.(Offset) -> Unit)?,
    thumbTrackGapSize: Dp,
    trackInsideCornerSize: Dp,
    isCentered: Boolean,
) {
    val inactiveTrackColor = colors.trackColor(enabled = enabled, active = false)
    val activeTrackColor = colors.trackColor(enabled = enabled, active = true)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .then(
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val cornerSize =
                        if (trackCornerSize == Dp.Unspecified) {
                            placeable.height / 2
                        } else {
                            trackCornerSize.roundToPx()
                        }
                    layout(
                        width = placeable.width,
                        height = placeable.height,
                        alignmentLines = mapOf(CornerSizeAlignmentLine to cornerSize),
                    ) {
                        placeable.place(0, 0)
                    }
                }
            )
    ) {
        val cornerSize: Float =
            if (trackCornerSize == Dp.Unspecified) {
                size.height / 2
            } else {
                trackCornerSize.toPx()
            }
        drawTrack(
            tickFractions = floatArrayOf(),
            activeRangeStart = 0f,
            activeRangeEnd = sliderState.coercedValueAsFraction,
            bufferRangeEnd = bufferValue?.let {
                sliderState.valueRange.run {
                    calcFraction(start, endInclusive, bufferValue().coerceIn(start, endInclusive))
                }
            } ?: 0f,
            inactiveTrackColor = inactiveTrackColor,
            activeTrackColor = activeTrackColor,
            startThumbWidth = Dp.Hairline,
            startThumbHeight = Dp.Hairline,
            endThumbWidth = ThumbWidth,
            endThumbHeight = ThumbHeight,
            thumbTrackGapSize = thumbTrackGapSize,
            trackInsideCornerSize = trackInsideCornerSize,
            trackCornerSize = cornerSize.toDp(),
            drawStopIndicator = drawStopIndicator,
            isRangeSlider = false,
            enableCornerShrinking = false,
            isCentered = isCentered,
        )
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    bufferRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    startThumbWidth: Dp,
    startThumbHeight: Dp,
    endThumbWidth: Dp,
    endThumbHeight: Dp,
    thumbTrackGapSize: Dp,
    trackInsideCornerSize: Dp,
    trackCornerSize: Dp,
    drawStopIndicator: (DrawScope.(Offset) -> Unit)?,
    isRangeSlider: Boolean,
    enableCornerShrinking: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    isCentered: Boolean = false,
) {
    val isVertical = orientation == Vertical
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val isRtlHorizontal = isRtl && !isVertical
    val cornerSize = trackCornerSize.toPx()
    val sliderStart = 0f
    val sliderEnd = if (isVertical) size.height else size.width

    val isStartOnFirstOrLastStep =
        activeRangeStart == tickFractions.firstOrNull() ||
            activeRangeStart == tickFractions.lastOrNull()
    val isEndOnFirstOrLastStep =
        activeRangeEnd == tickFractions.firstOrNull() ||
            activeRangeEnd == tickFractions.lastOrNull()
    val sliderValueEnd =
        if (tickFractions.isNotEmpty() && !isEndOnFirstOrLastStep) {
            sliderStart +
                (sliderEnd - sliderStart - cornerSize * 2) * activeRangeEnd +
                cornerSize
        } else {
            sliderStart + (sliderEnd - sliderStart) * activeRangeEnd
        }
    val bufferValueEnd =
        if (tickFractions.isNotEmpty() && activeRangeEnd != tickFractions.lastOrNull()) {
            sliderStart + (sliderEnd - sliderStart - cornerSize * 2) * bufferRangeEnd + cornerSize
        } else {
            sliderStart + (sliderEnd - sliderStart) * bufferRangeEnd
        }
    val sliderValueStart =
        if (tickFractions.isNotEmpty() && !isStartOnFirstOrLastStep) {
            sliderStart +
                (sliderEnd - sliderStart - cornerSize * 2) * activeRangeStart +
                cornerSize
        } else {
            sliderStart + (sliderEnd - sliderStart) * activeRangeStart
        }

    val insideCornerSize = trackInsideCornerSize.toPx()
    var startGap = 0f
    var endGap = 0f
    if (thumbTrackGapSize > 0.dp) {
        if (isVertical) {
            startGap = startThumbHeight.toPx() / 2 + thumbTrackGapSize.toPx()
            endGap = endThumbHeight.toPx() / 2 + thumbTrackGapSize.toPx()
        } else {
            startGap = startThumbWidth.toPx() / 2 + thumbTrackGapSize.toPx()
            endGap = endThumbWidth.toPx() / 2 + thumbTrackGapSize.toPx()
        }
    }
    val centerAxis = if (isVertical) center.y else center.x

    // inactive track (centered or range slider)
    var rangeInactiveTrackThreshold = sliderStart + startGap
    if (!enableCornerShrinking || tickFractions.isNotEmpty()) {
        rangeInactiveTrackThreshold += cornerSize
    }
    val adjustedSliderValueEnd =
        if (isCentered) {
            min(sliderValueEnd, centerAxis)
        } else {
            sliderValueStart
        }
    if ((isCentered || isRangeSlider) && adjustedSliderValueEnd > rangeInactiveTrackThreshold) {
        val startCornerRadius = if (isRtlHorizontal) insideCornerSize else cornerSize
        val endCornerRadius = if (isRtlHorizontal) cornerSize else insideCornerSize
        val start = sliderStart
        val end = adjustedSliderValueEnd - startGap
        val trackOffset =
            if (isRtlHorizontal) {
                Offset(size.width - end, 0f)
            } else {
                Offset(0f, 0f)
            }
        val trackSize =
            if (isVertical) {
                Size(size.width, end - start)
            } else {
                Size(end - start, size.height)
            }
        drawTrackPath(
            orientation,
            trackOffset,
            trackSize,
            inactiveTrackColor,
            startCornerRadius,
            endCornerRadius,
        )
        val stopIndicatorOffset =
            if (isVertical) {
                Offset(center.x, start + cornerSize)
            } else if (isRtl) {
                Offset(size.width - start - cornerSize, center.y)
            } else {
                Offset(start + cornerSize, center.y)
            }
        drawStopIndicator?.invoke(this, stopIndicatorOffset)
    }
    // inactive track
    var inactiveTrackThreshold = sliderEnd - endGap
    if (!enableCornerShrinking || tickFractions.isNotEmpty()) {
        inactiveTrackThreshold -= cornerSize
    }
    val adjustedSliderValueStart =
        if (isCentered) {
            max(sliderValueEnd, centerAxis)
        } else {
            sliderValueEnd
        }
    if (adjustedSliderValueStart < inactiveTrackThreshold) {
        val startCornerRadius = if (isRtlHorizontal) cornerSize else insideCornerSize
        val endCornerRadius = if (isRtlHorizontal) insideCornerSize else cornerSize
        val start = adjustedSliderValueStart + endGap
        val end = sliderEnd
        val inactiveTrackWidth = end - start
        val trackOffset =
            if (isVertical) {
                Offset(0f, start)
            } else if (isRtl) {
                Offset(0f, 0f)
            } else {
                Offset(start, 0f)
            }
        val size =
            if (isVertical) {
                Size(size.width, inactiveTrackWidth)
            } else if (isRtl && !isRangeSlider) {
                Size(size.width - start, size.height)
            } else {
                Size(inactiveTrackWidth, size.height)
            }
        drawTrackPath(
            orientation,
            trackOffset,
            size,
            inactiveTrackColor,
            startCornerRadius,
            endCornerRadius,
        )
        val stopIndicatorOffset =
            if (isVertical) {
                Offset(center.x, end - cornerSize)
            } else if (isRtl) {
                Offset(cornerSize, center.y)
            } else {
                Offset(end - cornerSize, center.y)
            }

        // buffer track
        val bufferTrackStart = start
        val bufferTrackEnd = bufferValueEnd
        val bufferTrackWidth = bufferTrackEnd - bufferTrackStart
        if (bufferTrackWidth > 0f) {
            val endCornerRadius = if (bufferRangeEnd >= 0.99f) endCornerRadius else insideCornerSize
            val bufferOffset = if (isVertical) {
                Offset(0f, bufferTrackStart)
            } else if (isRtl) {
                Offset(size.width - bufferTrackEnd, 0f)
            } else {
                Offset(bufferTrackStart, 0f)
            }

            val bufferSize =
                if (isVertical) {
                    Size(size.width, bufferTrackWidth)
                } else if (isRtl && !isCentered && !isRangeSlider) {
                    Size(bufferTrackEnd, size.height)
                } else {
                    Size(bufferTrackWidth, size.height)
                }

            drawTrackPath(
                orientation,
                bufferOffset,
                bufferSize,
                BufferTrackColor,
                startCornerRadius,
                endCornerRadius
            )
        }

        drawStopIndicator?.invoke(this, stopIndicatorOffset)
    }

    // active track
    val activeTrackStart =
        if (isCentered) {
            adjustedSliderValueEnd + if (adjustedSliderValueEnd < centerAxis) startGap else 0f
        } else if (isRangeSlider) {
            sliderValueStart + startGap
        } else {
            0f
        }
    val activeTrackEnd =
        if (isCentered) {
            adjustedSliderValueStart - if (adjustedSliderValueStart > centerAxis) endGap else 0f
        } else {
            sliderValueEnd - endGap
        }
    val startCornerRadius =
        if (isRtlHorizontal || isCentered || isRangeSlider) insideCornerSize else cornerSize
    val endCornerRadius =
        if (isRtlHorizontal && !isCentered && !isRangeSlider) cornerSize else insideCornerSize
    val activeTrackWidth =
        if (isRtlHorizontal && !isCentered && !isRangeSlider) activeTrackEnd
        else activeTrackEnd - activeTrackStart

    val activeTrackThreshold =
        if (!enableCornerShrinking || tickFractions.isNotEmpty()) startCornerRadius else 0f
    if (activeTrackWidth > activeTrackThreshold) {
        val trackOffset =
            if (isVertical) {
                Offset(0f, activeTrackStart)
            } else if (isRtl) {
                Offset(size.width - activeTrackEnd, 0f)
            } else {
                Offset(activeTrackStart, 0f)
            }

        val size =
            if (isVertical) {
                Size(size.width, activeTrackWidth)
            } else if (isRtl && !isCentered && !isRangeSlider) {
                Size(activeTrackEnd, size.height)
            } else {
                Size(activeTrackWidth, size.height)
            }
        drawTrackPath(
            orientation,
            trackOffset,
            size,
            activeTrackColor,
            startCornerRadius,
            endCornerRadius,
        )
    }
}

private fun DrawScope.drawTrackPath(
    orientation: Orientation,
    offset: Offset,
    size: Size,
    color: Color,
    startCornerRadius: Float,
    endCornerRadius: Float,
) {
    val startCorner = CornerRadius(startCornerRadius, startCornerRadius)
    val endCorner = CornerRadius(endCornerRadius, endCornerRadius)
    val track =
        if (orientation == Vertical) {
            RoundRect(
                rect = Rect(offset, size = Size(size.width, size.height)),
                topLeft = startCorner,
                topRight = startCorner,
                bottomRight = endCorner,
                bottomLeft = endCorner,
            )
        } else {
            RoundRect(
                rect = Rect(offset, size = Size(size.width, size.height)),
                topLeft = startCorner,
                topRight = endCorner,
                bottomRight = endCorner,
                bottomLeft = startCorner,
            )
        }
    trackPath.addRoundRect(track)
    drawPath(trackPath, color)
    trackPath.rewind()
}

private val BufferTrackColor = Color.White.copy(alpha = 0.8f)

private val TrackHeight = 16.dp
private val ThumbWidth = 4.dp
private val ThumbHeight = 44.dp // SliderTokens.HandleHeight
private val ThumbSize = DpSize(ThumbWidth, ThumbHeight)
private val TrackInsideCornerSize: Dp = 2.dp
private val ThumbTrackGapSize: Dp = 4.dp // SliderTokens.ActiveHandleLeadingSpace (6dp)

private val CornerSizeAlignmentLine = VerticalAlignmentLine(::min)

private val trackPath = Path()
