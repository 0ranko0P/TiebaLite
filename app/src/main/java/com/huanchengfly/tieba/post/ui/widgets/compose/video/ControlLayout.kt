package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.PresentationState
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToLong

/** 控制层尺寸规格，所有尺寸按播放器容器统一缩放 */
@Immutable
internal class ControlSpec(
    val horizontalGap: Dp,
    val verticalGap: Dp,
    val textSize: TextUnit,
    val centerTextSize: TextUnit,
    val iconSize: Dp,
    val buttonSize: Dp,
    val trackHeight: Dp,
    val centerButtonSize: Dp,
    val centerIconSize: Dp,
)

/** 根据播放器容器尺寸计算控制层缩放比例，保证内嵌和全屏播放器都按自身画布缩放 */
private fun getControlScale(
    playerWidth: Dp,
    playerHeight: Dp,
): Float {
    val longSideDp = maxOf(playerWidth.value, playerHeight.value)
    val shortSideDp = minOf(playerWidth.value, playerHeight.value)

    return minOf(
        longSideDp / 640f,
        shortSideDp / 360f,
    ).pow(0.2f).coerceIn(0.5f, 1.5f)
}

private fun Float.scaledDp(scale: Float): Dp = (this * scale).dp

private fun Float.scaledSp(scale: Float): TextUnit = (this * scale).sp

private val EnterSpec =
    tween<Float>(
        durationMillis = 160,
        easing = FastOutSlowInEasing,
    )

private val ExitSpec =
    tween<Float>(
        durationMillis = 100,
        easing = LinearOutSlowInEasing,
    )

private fun enterTransition() =
    fadeIn(animationSpec = EnterSpec) +
        scaleIn(
            animationSpec = EnterSpec,
            initialScale = 1.08f,
        )

private fun exitTransition() =
    fadeOut(animationSpec = ExitSpec) +
        scaleOut(
            animationSpec = ExitSpec,
            targetScale = 1.08f,
        )

private fun Modifier.resetControlsAutoHideOnInput(
    player: PlayerHandle,
    enabled: Boolean,
): Modifier =
    if (enabled) {
        pointerInput(player) { detectControlsAutoHideResetInput(player) }
    } else {
        this
    }

private suspend fun PointerInputScope.detectControlsAutoHideResetInput(player: PlayerHandle) {
    coroutineScope {
        awaitEachGesture {
            awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            player.showControls()

            val pressedResetJob =
                launch {
                    while (isActive) {
                        delay(1000L)
                        player.showControls()
                    }
                }

            try {
                do {
                } while (awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed })

                player.showControls()
            } finally {
                pressedResetJob.cancel()
            }
        }
    }
}

/** 按缩放比例生成控制层尺寸 */
private fun getControlSpec(
    scale: Float,
    verticalGapScale: Float,
): ControlSpec =
    ControlSpec(
        horizontalGap = 16f.scaledDp(scale),
        verticalGap = 16f.scaledDp(verticalGapScale),
        textSize = 14f.scaledSp(scale),
        centerTextSize = 24f.scaledSp(scale),
        buttonSize = 56f.scaledDp(scale),
        iconSize = 24f.scaledDp(scale),
        trackHeight = 12f.scaledDp(scale),
        centerButtonSize = 96f.scaledDp(scale),
        centerIconSize = 64f.scaledDp(scale),
    )

/** 主控制层，根据播放器状态显示或隐藏控件；通过 [BoxWithConstraints] 获取播放器实际渲染尺寸 */
@UnstableApi
@Composable
internal fun BoxScope.ControlsOverlay(
    player: PlayerHandle,
    previewPlayerProvider: () -> Player,
    exoPlayer: ExoPlayer,
    isFullscreen: Boolean,
    presentationState: PresentationState,
    onVideoInfoClick: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.matchParentSize()) {
        val windowSize = LocalWindowInfo.current.containerDpSize
        val isLandscape = windowSize.width > windowSize.height
        val controlScale =
            remember(maxWidth, maxHeight) {
                getControlScale(
                    playerWidth = maxWidth,
                    playerHeight = maxHeight,
                )
            }
        val state by player.state.collectAsStateWithLifecycle()
        val seekPreview by player.seekPreview.collectAsStateWithLifecycle()
        val levelChange by player.levelChange.collectAsStateWithLifecycle()
        val pipSupported by player.pipSupported.collectAsStateWithLifecycle()
        val cachedRanges by player.cachedRanges.collectAsStateWithLifecycle()
        val controlsShown = state.controlsShown
        val isLocked = state.isLocked
        val lockOverlayShown = state.lockOverlayShown
        val lockButtonVisible =
            if (isLocked) {
                lockOverlayShown
            } else {
                controlsShown && isFullscreen
            }
        val verticalGapScale =
            if (maxWidth > maxHeight) {
                controlScale * maxHeight.value / maxWidth.value
            } else {
                controlScale
            }
        val spec =
            remember(controlScale, verticalGapScale) {
                getControlSpec(
                    scale = controlScale,
                    verticalGapScale = verticalGapScale,
                )
            }
        // 控件显示时以 30fps 轮询播放器；隐藏或锁定时降到 1fps
        val progressState =
            rememberSmoothProgressState(
                player = exoPlayer,
                frameUpdatesEnabled = controlsShown && !isLocked,
            )
        if (isLocked) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickableNoIndication { player.toggleLockOverlay() },
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .resetControlsAutoHideOnInput(
                            player = player,
                            enabled = controlsShown && !isLocked,
                        ),
            ) {
                if (isFullscreen) {
                    GestureOverlay(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .padding(vertical = spec.verticalGap),
                        durationProvider = { progressState.durationMs },
                        positionProvider = { player.positionMs() },
                        player = player,
                        spec = spec,
                    )
                }

                AnimatedVisibility(
                    visible = controlsShown,
                    enter = enterTransition(),
                    exit = exitTransition(),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 播放控制
                        if (levelChange == null && seekPreview == null) {
                            Box(
                                modifier = Modifier.align(Alignment.Center),
                                contentAlignment = Alignment.Center,
                            ) {
                                PlayButtonContainer(
                                    spec = spec,
                                    player = player,
                                    playerState = state,
                                )
                            }
                        }

                        if (isFullscreen) {
                            // 返回
                            ControlButton(
                                onClick = onExitFullscreen,
                                content = IconButtonContent.DrawableIcon(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(id = R.string.btn_full_screen_exit),
                                spec = spec,
                                modifier =
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .padding(horizontal = spec.horizontalGap, vertical = spec.verticalGap),
                            )

                            Row(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(horizontal = spec.horizontalGap, vertical = spec.verticalGap),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // 画中画
                                if (pipSupported) {
                                    ControlButton(
                                        onClick = player.requestPip,
                                        content = IconButtonContent.DrawableIcon(R.drawable.ic_picture_in_picture),
                                        contentDescription = stringResource(id = R.string.btn_pip),
                                        spec = spec,
                                    )
                                }

                                // 视频信息
                                ControlButton(
                                    onClick = onVideoInfoClick,
                                    content = IconButtonContent.DrawableIcon(R.drawable.ic_info),
                                    contentDescription = stringResource(id = R.string.btn_video_info),
                                    spec = spec,
                                )
                            }
                        }

                        // 底部控制栏
                        BottomBar(
                            player = player,
                            previewPlayerProvider = previewPlayerProvider,
                            spec = spec,
                            isFullscreen = isFullscreen,
                            isLandscape = isLandscape,
                            playbackSpeed = state.playbackSpeed,
                            cachedRanges = cachedRanges,
                            progressState = progressState,
                            presentationState = presentationState,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = lockButtonVisible,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = spec.horizontalGap, vertical = spec.verticalGap),
            enter = enterTransition(),
            exit = exitTransition(),
        ) {
            Box(
                modifier =
                    Modifier.resetControlsAutoHideOnInput(
                        player = player,
                        enabled = lockButtonVisible && !isLocked,
                    ),
            ) {
                ControlButton(
                    onClick = player::toggleLock,
                    content =
                        IconButtonContent.DrawableIcon(
                            resourceId =
                                if (isLocked) {
                                    R.drawable.ic_lock
                                } else {
                                    R.drawable.ic_lock_open
                                },
                        ),
                    contentDescription =
                        stringResource(
                            id =
                                if (isLocked) {
                                    R.string.btn_unlock
                                } else {
                                    R.string.btn_lock
                                },
                        ),
                    spec = spec,
                )
            }
        }
    }
}

/** 底部控制栏：全屏模式启用独立预览播放器，内嵌模式只显示基础控制 */
@Composable
@OptIn(UnstableApi::class)
private fun BoxScope.BottomBar(
    player: PlayerHandle,
    previewPlayerProvider: () -> Player,
    spec: ControlSpec,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    playbackSpeed: Float,
    cachedRanges: List<CachedTimeRange>,
    progressState: SmoothProgressState,
    presentationState: PresentationState,
) {
    // 用按钮内边距对齐进度条和图标的视觉边缘
    val buttonGap = (spec.buttonSize - spec.iconSize) / 2
    val windowSize = LocalWindowInfo.current.containerDpSize
    Column(
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = spec.horizontalGap, vertical = spec.verticalGap / 2),
    ) {
        val durationMs = progressState.durationMs
        val positionMs = progressState.currentPositionMs
        var showSpeed by remember { mutableStateOf(false) }
        LaunchedEffect(isFullscreen) {
            if (!isFullscreen) showSpeed = false
        }
        if (durationMs > 0) {
            AnimatedVisibility(
                visible = !isFullscreen || !showSpeed,
                enter = fadeIn(animationSpec = EnterSpec),
                exit = fadeOut(animationSpec = ExitSpec),
            ) {
                val position = positionMs.coerceAtLeast(0L)
                val progress = (position.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val bufferedPosition = progressState.bufferedPositionMs.coerceAtLeast(0L)
                val bufferProgress = (bufferedPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                // 进度条
                SeekBar(
                    progress = progress,
                    trackHeight = spec.trackHeight,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = buttonGap),
                    onSeek = { progress ->
                        player.seekPreviewTo(position = (progress * durationMs).roundToLong())
                    },
                    onSeekEnd = { stoppedProgress ->
                        val targetPosition = (stoppedProgress * durationMs).roundToLong()
                        progressState.snapTo(positionMs = targetPosition)
                        player.seekTo(position = targetPosition)
                    },
                    bufferProgress = bufferProgress,
                    cachedRanges = cachedRanges,
                    durationMs = durationMs,
                    previewPopup = {
                        if (!isFullscreen) return@SeekBar
                        val videoSize = presentationState.videoSizeDp ?: return@SeekBar
                        val aspectRatio = videoSize.run { width / height }
                        val previewHeight =
                            (windowSize.height * 0.20f).coerceIn(80.dp, 160.dp)
                        PreviewSurface(
                            player = previewPlayerProvider(),
                            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                            modifier =
                                Modifier
                                    .height(previewHeight)
                                    .width(previewHeight * aspectRatio)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray),
                        )
                    },
                    timeLabelProvider = { progress ->
                        getDurationString((progress * durationMs).roundToLong())
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (durationMs > 0) {
                val durationText = remember(durationMs) { getDurationString(durationMs) }
                val positionText = remember(positionMs) { getDurationString(positionMs) }
                Text(
                    text = "$positionText / $durationText",
                    color = Color.White,
                    style = shadowTextStyle(fontSize = spec.textSize),
                    maxLines = 1,
                    modifier =
                        Modifier
                            .height(spec.buttonSize)
                            .padding(start = buttonGap)
                            .wrapContentSize(Alignment.CenterStart),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isFullscreen) {
                Box(
                    modifier =
                        Modifier
                            .width(spec.buttonSize)
                            .height(spec.buttonSize),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    SpeedSliderPopup(
                        visible = showSpeed,
                        spec = spec,
                        playbackSpeed = playbackSpeed,
                        player = player,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .wrapContentSize(
                                    align = Alignment.BottomCenter,
                                    unbounded = true,
                                ).offset(y = -spec.buttonSize),
                    )
                    // 倍速
                    SpeedButton(
                        spec = spec,
                        currentSpeed = playbackSpeed,
                        onToggleSlider = { showSpeed = !showSpeed },
                    )
                }
                // 旋转
                ControlButton(
                    onClick = { player.rotateFullscreen(isLandscape) },
                    content = IconButtonContent.DrawableIcon(R.drawable.ic_screen_rotation_alt),
                    contentDescription = stringResource(id = R.string.btn_rotate),
                    spec = spec,
                )
            } else {
                // 全屏
                ControlButton(
                    onClick = player::toggleFullscreen,
                    content = IconButtonContent.DrawableIcon(R.drawable.ic_fullscreen),
                    contentDescription = stringResource(id = R.string.btn_full_screen),
                    spec = spec,
                )
            }
        }
    }
}

@Composable
private fun SpeedSliderPopup(
    visible: Boolean,
    spec: ControlSpec,
    playbackSpeed: Float,
    player: PlayerHandle,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .width(spec.trackHeight * 3f)
                        .height(spec.iconSize * 6)
                        .wrapContentSize(unbounded = true),
                contentAlignment = Alignment.Center,
            ) {
                SpeedSlider(
                    currentSpeed = playbackSpeed,
                    onSpeedChange = {
                        player.setSpeed(it)
                    },
                    onInteracting = {
                        player.showControls()
                    },
                    trackHeight = spec.trackHeight,
                    modifier =
                        Modifier
                            .width(spec.iconSize * 6)
                            .height(spec.trackHeight * 3f)
                            .rotate(-90f),
                )
            }
            Spacer(modifier = Modifier.height(spec.verticalGap / 2))
        }
    }
}

/** 预览播放器使用独立 Surface；销毁时清理输出，避免影响主画面或下次预览 */
@Composable
@OptIn(UnstableApi::class)
private fun PreviewSurface(
    player: Player,
    surfaceType: Int,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(player) { onDispose { player.clearVideoSurface() } }

    PlayerSurface(
        player = player,
        surfaceType = surfaceType,
        modifier = modifier,
    )
}
