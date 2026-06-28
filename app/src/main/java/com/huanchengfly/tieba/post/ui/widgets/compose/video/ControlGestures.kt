package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 手势叠加层：组合快进动画、Seek 提示、音量/亮度提示和手势检测 */
@Composable
internal fun GestureOverlay(
    modifier: Modifier = Modifier,
    durationProvider: () -> Long,
    positionProvider: () -> Long,
    player: PlayerHandle,
    spec: ControlSpec,
) {
    val state by player.state.collectAsStateWithLifecycle()
    val quickSeek = state.quickSeek
    val seekPreview by player.seekPreview.collectAsStateWithLifecycle()
    val levelChange by player.levelChange.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        QuickSeekOverlay(
            quickSeek = quickSeek,
            onAnimationEnd = player::clearQuickSeek,
        )
        SeekPreviewText(
            seekPreview = seekPreview,
            textSize = spec.centerTextSize,
        )
        LevelIndicator(
            adjustment = levelChange,
            textSize = spec.centerTextSize,
        )
        GestureLayer(
            durationProvider = durationProvider,
            positionProvider = positionProvider,
            player = player,
        )
    }
}

/** 统一处理手势，避免控制栏与视频 Surface 分散消费触摸事件 */
@Composable
private fun GestureLayer(
    durationProvider: () -> Long,
    positionProvider: () -> Long,
    player: PlayerHandle,
) {
    var isFastForwarding by remember { mutableStateOf(false) }
    var savedSpeed by remember { mutableFloatStateOf(SPEED_NORMAL) }
    var fastForwardSpeedLabel by remember { mutableStateOf<String?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(player) {
                    var seekOffset = Offset.Zero
                    var duration: Long = 0
                    var currentPosition: Long = 0
                    var seekTarget: Long? = null
                    var levelType = LevelType.Volume
                    var startLevel = 0f
                    var levelDrag = 0f

                    fun resetState() {
                        seekOffset = Offset.Zero
                        seekTarget = null
                        player.setSeekPreview(null)
                    }

                    detectPlayerGestures(
                        onTap = { if (player.selectState { it.controlsShown }) player.hideControls() else player.showControls() },
                        onDoubleTap = { pos ->
                            val durationNow = durationProvider()
                            val seekEnabled = player.selectState { it.settings.doubleTapSeekEnabled }
                            val pauseEnabled = player.selectState { it.settings.doubleTapPauseEnabled }
                            // 启用双击暂停时保留中间区域，降低左右快进/快退误触
                            val leftBoundary = size.width * if (pauseEnabled) 0.35f else 0.5f
                            val rightBoundary = size.width * if (pauseEnabled) 0.65f else 0.5f
                            val onLeft = pos.x < leftBoundary
                            val onRight = pos.x > rightBoundary
                            // 短视频禁用
                            val canUseDoubleTapSeek = seekEnabled && durationNow > 30_000L

                            when {
                                canUseDoubleTapSeek && onLeft -> {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    player.seekBackward()
                                }

                                canUseDoubleTapSeek && onRight -> {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    player.seekForward()
                                }

                                pauseEnabled -> {
                                    player.togglePlaying()
                                }
                            }
                        },
                        onDragStart = {
                            currentPosition = positionProvider()
                            duration = durationProvider()
                            resetState()
                        },
                        onDragEnd = {
                            seekTarget?.let { player.seekTo(it) }
                            resetState()
                        },
                        onDrag = { dragAmount: Float ->
                            seekOffset += Offset(x = dragAmount, y = 0f)

                            val seek =
                                getSeekTarget(
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    dragOffset = seekOffset.x,
                                    viewWidth = size.width,
                                )
                            seekTarget = seek.targetMs.toLong()

                            player.setSeekPreview(
                                SeekPreview(
                                    targetMs = seek.targetMs,
                                    deltaMs = seek.deltaMs,
                                ),
                            )
                            player.seekPreviewTo(seek.targetMs.toLong())
                        },
                        canStartHorizontalDrag = {
                            player.selectState { it.settings.horizontalSwipeSeekEnabled } && durationProvider() > 0L
                        },
                        onVerticalDragStart = { startOffset ->
                            val volumeEnabled = player.selectState { it.settings.volumeControlEnabled }
                            val brightnessEnabled = player.selectState { it.settings.brightnessControlEnabled }

                            levelDrag = 0f
                            // 左侧调亮度、右侧调音量；禁用其中一项时自动使用另一项
                            levelType =
                                when {
                                    !brightnessEnabled -> LevelType.Volume
                                    !volumeEnabled -> LevelType.Brightness
                                    startOffset.x < size.width * 0.5f -> LevelType.Brightness
                                    else -> LevelType.Volume
                                }
                            startLevel =
                                when (levelType) {
                                    LevelType.Volume -> player.getVolume()
                                    LevelType.Brightness -> player.getBrightness()
                                }
                            if (volumeEnabled || brightnessEnabled) {
                                player.setLevelChange(
                                    LevelChange(
                                        type = levelType,
                                        level = startLevel,
                                    ),
                                )
                            }
                        },
                        onVerticalDragEnd = {
                            player.setLevelChange(null)
                        },
                        onVerticalDrag = { dragAmount ->
                            levelDrag += dragAmount
                            val delta = -levelDrag / size.height
                            val newLevel = (startLevel + delta).coerceIn(0f, 1f)

                            when (levelType) {
                                LevelType.Volume -> {
                                    if (player.selectState { it.settings.volumeControlEnabled }) player.setVolume(newLevel)
                                }

                                LevelType.Brightness -> {
                                    if (player.selectState { it.settings.brightnessControlEnabled }) player.setBrightness(newLevel)
                                }
                            }

                            if (player.selectState { v -> v.settings.volumeControlEnabled || v.settings.brightnessControlEnabled }) {
                                player.setLevelChange(
                                    LevelChange(
                                        type = levelType,
                                        level = newLevel,
                                    ),
                                )
                            }
                        },
                        canStartVerticalDrag = {
                            player.selectState { it.settings.volumeControlEnabled || it.settings.brightnessControlEnabled }
                        },
                        onLongPressStart = {
                            val speed = player.selectState { it.settings.longPressSpeed }
                            if (speed != SPEED_OFF) {
                                savedSpeed = player.speed()
                                // auto_double 在当前倍速上翻倍，其余设置使用固定倍速
                                val targetSpeed =
                                    if (speed == SPEED_AUTO) {
                                        (savedSpeed * 2f).coerceAtLeast(0.25f)
                                    } else {
                                        speed.toFloat()
                                    }
                                fastForwardSpeedLabel = formatSpeed(targetSpeed)
                                player.setSpeed(targetSpeed)
                                isFastForwarding = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onLongPressEnd = {
                            if (isFastForwarding) {
                                player.setSpeed(savedSpeed)
                                isFastForwarding = false
                            }
                        },
                    )
                },
    ) {
        // 长按倍速时显示当前临时倍速
        if (isFastForwarding) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.20f))
                Text(
                    text = stringResource(R.string.speed_playing, fastForwardSpeedLabel.orEmpty()),
                    color = Color.White,
                    style = shadowTextStyle(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

/** 手势状态机：超过 touch slop 或长按超时后再锁定类型，避免拖拽、单击、双击和长按互相干扰 */
suspend fun PointerInputScope.detectPlayerGestures(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onVerticalDragStart: (Offset) -> Unit,
    onVerticalDragEnd: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    canStartHorizontalDrag: () -> Boolean,
    canStartVerticalDrag: () -> Boolean,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
) {
    coroutineScope {
        val touchSlop = viewConfiguration.touchSlop
        val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis
        var longPressActive = false
        var tapJob: Job? = null

        try {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downPos = down.position
                val pointerId = down.id

                var gestureType: Int = TYPE_UNDECIDED
                var longPressTriggered = false

                val longPressJob =
                    launch {
                        delay(500L)
                        longPressTriggered = true
                        longPressActive = true
                        gestureType = TYPE_LONG_PRESS
                        tapJob?.cancel()
                        tapJob = null
                        onLongPressStart()
                    }

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: continue

                    if (!change.pressed) {
                        longPressJob.cancel()
                        when (gestureType) {
                            TYPE_LONG_PRESS -> {
                                if (longPressTriggered) {
                                    change.consume()
                                    onLongPressEnd()
                                    longPressActive = false
                                }
                            }

                            TYPE_HORIZONTAL_DRAG -> {
                                onDragEnd()
                            }

                            TYPE_VERTICAL_DRAG -> {
                                onVerticalDragEnd()
                            }

                            TYPE_UNDECIDED -> {
                                if (!longPressTriggered) {
                                    val prevJob = tapJob
                                    if (prevJob != null && prevJob.isActive) {
                                        prevJob.cancel()
                                        tapJob = null
                                        onDoubleTap(downPos)
                                    } else {
                                        tapJob =
                                            launch {
                                                delay(doubleTapTimeoutMs)
                                                onTap(downPos)
                                                tapJob = null
                                            }
                                    }
                                }
                            }
                        }
                        break
                    }

                    val dx = change.position.x - downPos.x
                    val dy = change.position.y - downPos.y

                    if (gestureType == TYPE_UNDECIDED && !longPressTriggered) {
                        val distanceSq = dx * dx + dy * dy
                        if (distanceSq > touchSlop * touchSlop) {
                            longPressJob.cancel()
                            tapJob?.cancel()
                            tapJob = null
                            gestureType =
                                if (abs(dx) > abs(dy)) {
                                    if (!canStartHorizontalDrag()) {
                                        TYPE_CANCELLED
                                    } else {
                                        onDragStart(downPos)
                                        TYPE_HORIZONTAL_DRAG
                                    }
                                } else if (!canStartVerticalDrag()) {
                                    TYPE_CANCELLED
                                } else {
                                    onVerticalDragStart(downPos)
                                    TYPE_VERTICAL_DRAG
                                }
                        }
                    }

                    when (gestureType) {
                        TYPE_HORIZONTAL_DRAG -> {
                            onDrag(change.positionChange().x)
                            change.consume()
                        }

                        TYPE_VERTICAL_DRAG -> {
                            val posChange = change.positionChange()
                            onVerticalDrag(posChange.y)
                            if (posChange != Offset.Zero) change.consume()
                        }

                        TYPE_LONG_PRESS -> {
                            change.consume()
                        }
                    }
                }
            }
        } finally {
            tapJob?.cancel()
            if (longPressActive) onLongPressEnd()
        }
    }
}

/** 水平拖拽 Seek 的目标位置 */
private data class SeekTarget(
    val targetMs: Float,
    val deltaMs: Float,
)

/** 短视频将整段时长映射到拖拽宽度 */
private fun getSeekTarget(
    currentPosition: Long,
    duration: Long,
    dragOffset: Float,
    viewWidth: Int,
): SeekTarget {
    val boundedDuration = duration.coerceAtLeast(0L)
    val deltaMs =
        if (viewWidth > 0 && boundedDuration > 0L) {
            boundedDuration.coerceAtMost(60_000L).toFloat() * dragOffset / viewWidth
        } else {
            0f
        }

    val targetMs = (currentPosition + deltaMs).coerceIn(0f, boundedDuration.toFloat())
    return SeekTarget(
        targetMs = targetMs,
        deltaMs = targetMs - currentPosition,
    )
}

private const val TYPE_UNDECIDED = 0 // 未确定的手势
private const val TYPE_HORIZONTAL_DRAG = 1 // 水平拖拽（快进/快退）
private const val TYPE_VERTICAL_DRAG = 2 // 垂直拖拽（音量/亮度）
private const val TYPE_LONG_PRESS = 3 // 长按（倍速播放）
private const val TYPE_CANCELLED = 4 // 手势不可用，不消费事件

/** 双击快进/快退动画层，播放完成后自动清除 */
@Composable
private fun QuickSeekOverlay(
    quickSeek: QuickSeek,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rewindAlpha = remember { Animatable(0f) }
    val forwardAlpha = remember { Animatable(0f) }

    LaunchedEffect(quickSeek) {
        when (quickSeek) {
            QuickSeek.Rewind -> rewindAlpha
            QuickSeek.Forward -> forwardAlpha
            else -> null
        }?.let { animatable ->
            animatable.animateTo(1f, animationSpec = tween(200))
            animatable.animateTo(0f, animationSpec = tween(200))
            onAnimationEnd()
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        ) {
            ShadowedIcon(
                painter = painterResource(R.drawable.ic_keyboard_double_arrow_left),
                modifier =
                    Modifier
                        .size(48.dp)
                        .alpha(rewindAlpha.value)
                        .align(Alignment.Center),
            )
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        ) {
            ShadowedIcon(
                painter = painterResource(R.drawable.ic_keyboard_double_arrow_right),
                modifier =
                    Modifier
                        .size(48.dp)
                        .alpha(forwardAlpha.value)
                        .align(Alignment.Center),
            )
        }
    }
}

/** 水平拖拽 Seek 时在屏幕中央显示目标时间 */
@Composable
private fun BoxScope.SeekPreviewText(
    modifier: Modifier = Modifier,
    seekPreview: SeekPreview?,
    textSize: TextUnit,
) {
    if (seekPreview != null) {
        val text =
            remember(seekPreview.targetMs.toLong(), seekPreview.deltaMs.toLong()) {
                formatSeekPreviewText(seekPreview)
            }

        Text(
            text = text,
            style =
                shadowTextStyle(
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                ),
            color = Color.White,
            modifier = modifier.align(Alignment.Center),
        )
    }
}

private fun formatSeekPreviewText(seekPreview: SeekPreview): String {
    val prefix = if (seekPreview.deltaMs < 0) "-" else "+"
    val diffText = getDurationString(abs(seekPreview.deltaMs).toLong())
    val finalText = getDurationString(seekPreview.targetMs.toLong())
    return "[$prefix$diffText] $finalText"
}

/** 垂直拖拽音量/亮度时在屏幕中央显示图标和百分比 */
@Composable
private fun BoxScope.LevelIndicator(
    modifier: Modifier = Modifier,
    adjustment: LevelChange?,
    textSize: TextUnit,
) {
    if (adjustment != null) {
        val painter =
            when (adjustment.type) {
                LevelType.Volume -> painterResource(R.drawable.ic_volume_up)
                LevelType.Brightness -> painterResource(R.drawable.ic_brightness_5)
            }

        Row(
            modifier = modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ShadowedIcon(
                painter = painter,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "${(adjustment.level * 100).toInt()}%",
                style =
                    shadowTextStyle(
                        fontSize = textSize,
                        fontWeight = FontWeight.Bold,
                    ),
                color = Color.White,
            )
        }
    }
}
