package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.TrackStopIndicatorSize
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** 视频播放进度条 */
@Composable
fun SeekBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackHeight: Dp,
    bufferProgress: Float = 0f,
    cachedRanges: List<CachedTimeRange> = emptyList(),
    durationMs: Long = 0L,
    onSeek: (progress: Float) -> Unit = {},
    onSeekEnd: (stoppedProgress: Float) -> Unit = {},
    previewPopup: @Composable () -> Unit = {},
    timeLabelProvider: ((progress: Float) -> String)? = null,
) {
    val onSeekState = rememberUpdatedState(onSeek)
    val onSeekEndState = rememberUpdatedState(onSeekEnd)
    val interactionSource = remember { MutableInteractionSource() }
    val state = remember { SliderState() }
    val thumbSize = DpSize(SliderThumbWidth, trackHeight * 3f)
    val colors = SliderDefaults.colors(activeTrackColor = TrackActiveColor, inactiveTrackColor = TrackInactiveTrackColor)

    DisposableEffect(state) {
        state.onValueChange = {
            state.value = it
            if (state.isDragging) onSeekState.value(it)
        }
        state.onValueChangeFinished = { onSeekEndState.value(state.value) }

        onDispose {
            state.onValueChange = null
            state.onValueChangeFinished = null
        }
    }

    // 拖拽时由 SliderState 维护位置，避免外部 tick 抢回滑块
    LaunchedEffect(progress) {
        if (!state.isDragging) state.value = progress
    }

    Column(modifier = modifier) {
        // 拖拽时显示预览弹窗和时间提示
        if (state.isDragging) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.shadow(4.dp)) {
                    previewPopup()
                }

                if (timeLabelProvider != null) {
                    Text(
                        text = timeLabelProvider(state.value),
                        color = Color.White,
                        style = shadowTextStyle(),
                    )
                }
            }
        }

        Slider(
            modifier = Modifier.height(thumbSize.height),
            state = state,
            enabled = true,
            colors = colors,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    enabled = true,
                    thumbSize = thumbSize,
                )
            },
            track = { sliderState ->
                Track(
                    progressEnd = sliderState.coercedValueAsFraction,
                    bufferProgress = bufferProgress,
                    cachedRanges = cachedRanges,
                    durationMs = durationMs,
                    trackHeight = trackHeight,
                )
            },
        )
    }
}

/** 自定义轨道绘制顺序，保证缓存/缓冲只显示在播放进度之后 */
@Composable
private fun Track(
    progressEnd: Float,
    bufferProgress: Float,
    cachedRanges: List<CachedTimeRange>,
    durationMs: Long,
    trackHeight: Dp,
) {
    val activeTrackColor = TrackActiveColor
    val bufferColor = TrackBufferColor
    val path = remember { Path() }
    val bufferPath = remember { Path() }

    fun DrawScope.drawTrack(
        progressEnd: Float,
        bufferEnd: Float,
        cachedRanges: List<CachedTimeRange>,
        durationMs: Long,
    ) {
        val corner = size.height / 2 // 轨道两端的圆角半径
        val trackEnd = size.width // 轨道总宽度
        val progressX = trackEnd * progressEnd // 播放进度结束位置
        val bufferX = trackEnd * bufferEnd // 缓冲进度结束位置
        val innerCorner = TrackInnerCorner.toPx()
        val thumbGap = SliderThumbWidth.toPx() / 2 + ThumbTrackGap.toPx() // 滑块与轨道末端的间隙

        // 1. 非活跃轨道
        val inactiveX = progressX + thumbGap
        val inactiveWidth = trackEnd - inactiveX
        val inactiveLimit = trackEnd - thumbGap - corner
        if (progressX < inactiveLimit && inactiveWidth > 0f) {
            drawSpan(
                path = path,
                offset = Offset(inactiveX, 0f),
                size = Size(inactiveWidth, size.height),
                color = TrackInactiveTrackColor,
                startRadius = innerCorner,
                endRadius = corner,
            )
        }

        // 2. 缓冲轨道
        bufferPath.rewind()

        fun addBuffer(
            startX: Float,
            endX: Float,
        ) {
            val drawStart = max(startX, inactiveX)
            val drawEnd = min(endX, trackEnd)
            val spanWidth = drawEnd - drawStart
            if (spanWidth > 0f) {
                val endRadius =
                    if (drawEnd >= trackEnd - thumbGap - 1f) {
                        corner
                    } else {
                        innerCorner
                    }
                addSpan(
                    path = bufferPath,
                    offset = Offset(drawStart, 0f),
                    size = Size(spanWidth, size.height),
                    startRadius = innerCorner,
                    endRadius = endRadius,
                )
            }
        }

        if (durationMs > 0 && cachedRanges.isNotEmpty()) {
            cachedRanges.forEach { segment ->
                addBuffer(
                    trackEnd * (segment.startMs.toFloat() / durationMs.toFloat()),
                    trackEnd * (segment.endMs.toFloat() / durationMs.toFloat()),
                )
            }
        }

        if (bufferX > 0f) addBuffer(0f, bufferX)

        if (progressX < inactiveLimit && !bufferPath.isEmpty) drawPath(bufferPath, bufferColor)

        // 3. 终止指示器
        if (inactiveWidth > 0f) {
            drawStopIndicator(
                offset = Offset(trackEnd - corner, center.y),
                color = activeTrackColor,
                size = TrackStopIndicatorSize,
            )
        }

        // 4. 活跃轨道
        val activeTrackWidth = progressX - thumbGap
        if (activeTrackWidth > 0f) {
            drawSpan(
                path = path,
                offset = Offset(0f, 0f),
                size = Size(activeTrackWidth, size.height),
                color = activeTrackColor,
                startRadius = corner,
                endRadius = innerCorner,
            )
        }
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(trackHeight),
    ) {
        drawTrack(
            progressEnd = progressEnd,
            bufferEnd = bufferProgress.coerceIn(0f, 1f),
            cachedRanges = cachedRanges,
            durationMs = durationMs,
        )
    }
}

/** 添加圆角矩形轨道段 */
private fun addSpan(
    path: Path,
    offset: Offset,
    size: Size,
    startRadius: Float,
    endRadius: Float,
) {
    val start = CornerRadius(startRadius, startRadius)
    val end = CornerRadius(endRadius, endRadius)
    path.addRoundRect(
        RoundRect(
            rect = Rect(offset, size),
            topLeft = start,
            topRight = end,
            bottomRight = end,
            bottomLeft = start,
        ),
    )
}

/** 复用 Path 对象，减少进度条高频重绘时的分配 */
private fun DrawScope.drawSpan(
    path: Path,
    offset: Offset,
    size: Size,
    color: Color,
    startRadius: Float,
    endRadius: Float,
) {
    addSpan(path, offset, size, startRadius, endRadius)
    drawPath(path, color)
    path.rewind()
}

internal val SliderThumbWidth = 4.dp // 滑块宽度
private val ThumbTrackGap: Dp = 6.dp // 滑块与轨道的间距
private val TrackInnerCorner: Dp = 2.dp // 轨道段之间的圆角大小
private val TrackInactiveTrackColor = Color.White // 非活跃轨道颜色
private val TrackActiveColor // 活跃轨道颜色（跟随主题主色）
    @Composable get() = MaterialTheme.colorScheme.primary
private val TrackBufferColor // 缓冲轨道颜色
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer
