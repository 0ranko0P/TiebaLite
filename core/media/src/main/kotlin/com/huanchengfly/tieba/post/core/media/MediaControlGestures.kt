package com.huanchengfly.tieba.post.core.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.core.media.buttons.ShadowedIcon
import com.huanchengfly.tieba.post.core.media.state.DraggingProgress
import com.huanchengfly.tieba.post.core.media.state.PlayerGestureState
import com.huanchengfly.tieba.post.core.media.state.QuickSeekDirection
import com.huanchengfly.tieba.post.core.media.state.VerticalGesture
import com.huanchengfly.tieba.post.core.media.state.VolumeAndBrightnessState
import com.huanchengfly.tieba.post.core.media.state.rememberVolumeAndBrightnessState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Formatter
import java.util.Locale

@Composable
fun MediaControlGestures(
    modifier: Modifier = Modifier,
    gestureState: PlayerGestureState,
) {
    if (!gestureState.isEnabled) return

    val volumeAndBrightnessState = rememberVolumeAndBrightnessState()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        VolumeAndBrightnessIndicator(
            modifier = Modifier.matchParentSize(),
            activeGesture = gestureState.verticalGesture,
            state = volumeAndBrightnessState
        )

        Box(Modifier.matchParentSize().windowInsetsPadding(WindowInsets.safeGestures)) {
            QuickSeekAnimation(
                quickSeekDirection = gestureState.quickSeekAction,
                onAnimationEnd = {
                    gestureState.quickSeekAction = QuickSeekDirection.None
                }
            )
            DraggingProgressText(draggingProgress = gestureState.draggingProgress)

            GestureBox(gestureState, volumeAndBrightnessState)
        }
    }
}

@Composable
private fun GestureBox(
    controller: PlayerGestureState,
    volumeAndBrightnessState: VolumeAndBrightnessState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(controller) {
            var wasPlaying = true
            var totalOffset = Offset.Zero

            var duration: Long = 0
            var currentPosition: Long = 0
            // Vertical gesture
            var startBrightness = 0f
            var startVolume = 0f

            // When this job completes, it seeks to desired position.
            // It gets cancelled if delay does not complete
            var seekJob: Job? = null

            val resetState: () -> Unit = {
                totalOffset = Offset.Zero
                startBrightness = 0f
                startVolume = 0f
                controller.draggingProgress = null
                controller.verticalGesture = VerticalGesture.None
            }

            detectMediaPlayerGesture(
                onDoubleTap = { doubleTapPosition ->
                    when {
                        doubleTapPosition.x < size.width * 0.4f -> {
                            controller.quickSeekRewind()
                        }

                        doubleTapPosition.x > size.width * 0.6f -> {
                            controller.quickSeekForward()
                        }
                    }
                },
                onTap = {
                    if (controller.controlsVisible) {
                        controller.hideControls()
                    } else {
                        controller.showControls(autoHide = controller.isPlaying)
                    }
                },
                onDragStart = {
                    wasPlaying = controller.isPlaying
                    val pos = controller.onSeekGestureStart()
                    currentPosition = pos.first
                    duration = pos.second

                    resetState()
                },
                onDragEnd = {
                    resetState()
                    controller.onSeekGestureEnd(resume = wasPlaying)
                },
                onDrag = { dragAmount: Float ->
                    seekJob?.cancel()

                    totalOffset += Offset(x = dragAmount, y = 0f)

                    val diff = totalOffset.x

                    var diffTime = if (duration <= 60_000) {
                        duration.toFloat() * diff / size.width.toFloat()
                    } else {
                        60_000.toFloat() * diff / size.width.toFloat()
                    }

                    var finalTime = currentPosition + diffTime
                    if (finalTime < 0) {
                        finalTime = 0f
                    } else if (finalTime > duration) {
                        finalTime = duration.toFloat()
                    }
                    diffTime = finalTime - currentPosition

                    controller.draggingProgress = DraggingProgress(finalTime, diffTime)

                    seekJob = coroutineScope.launch {
                        delay(200)
                        controller.onSeekGesture(finalTime.toLong())
                    }
                },
                onVerticalDragStart = { offset ->
                    if (controller.controlsVisible) {
                        controller.hideControls()
                    }
                    controller.verticalGesture = when {
                        offset.x < size.width / 2 -> VerticalGesture.BRIGHTNESS
                        else -> VerticalGesture.VOLUME
                    }
                    startBrightness = volumeAndBrightnessState.currentBrightness
                    startVolume = volumeAndBrightnessState.currentVolume
                },
                onVerticalDragEnd = resetState,
                onVerticalDrag = { dragAmount ->
                    val delta = (-dragAmount / size.height) * VERTICAL_GESTURE_SENSITIVITY
                    when (controller.verticalGesture) {
                        VerticalGesture.BRIGHTNESS -> {
                            startBrightness += delta
                            volumeAndBrightnessState.setBrightness(startBrightness)
                        }

                        VerticalGesture.VOLUME -> {
                            startVolume += delta
                            volumeAndBrightnessState.setVolume(startVolume)
                        }

                        VerticalGesture.None -> Unit
                    }
                },
            )
        }
        .then(modifier)
    )
}

private suspend fun PointerInputScope.detectMediaPlayerGesture(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onVerticalDragStart: (Offset) -> Unit,
    onVerticalDragEnd: () -> Unit,
    onVerticalDrag: (Float) -> Unit
) {
    coroutineScope {
        launch {
            detectVerticalDragGestures(
                onDragStart = onVerticalDragStart,
                onDragEnd = onVerticalDragEnd,
                onVerticalDrag = { change, dragAmount ->
                    onVerticalDrag(dragAmount)
                    if (change.positionChange() != Offset.Zero) change.consume()
                },
            )
        }

        launch {
            detectHorizontalDragGestures(
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onHorizontalDrag = { change, dragAmount ->
                    onDrag(dragAmount)
                    if (change.positionChange() != Offset.Zero) change.consume()
                },
            )
        }

        detectTapGestures(
            onTap = onTap,
            onDoubleTap = onDoubleTap
        )
    }
}

@Composable
private fun QuickSeekAnimation(
    quickSeekDirection: QuickSeekDirection,
    onAnimationEnd: () -> Unit
) {
    val alphaRewind = remember { Animatable(0f) }
    val alphaForward = remember { Animatable(0f) }

    LaunchedEffect(quickSeekDirection) {
        when (quickSeekDirection) {
            QuickSeekDirection.Rewind -> alphaRewind
            QuickSeekDirection.Forward -> alphaForward
            else -> null
        }?.let { animatable ->
            animatable.animateTo(1f)
            animatable.animateTo(0f)
            onAnimationEnd()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ShadowedIcon(
                Icons.Filled.FastRewind,
                modifier = Modifier
                    .alpha(alphaRewind.value)
                    .align(Alignment.Center)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ShadowedIcon(
                Icons.Filled.FastForward,
                modifier = Modifier
                    .alpha(alphaForward.value)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun BoxScope.DraggingProgressText(
    modifier: Modifier = Modifier,
    draggingProgress: DraggingProgress?
) {
    if (draggingProgress != null) {
        val textStyle = remember {
            TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(blurRadius = 8f, offset = Offset(2f, 2f))
            )
        }
        val (stringBuilder, formatter) = remember {
            StringBuilder().let { it to Formatter(it, Locale.getDefault()) }
        }

        Text(
            text = draggingProgress.getProgressText(stringBuilder, formatter),
            style = textStyle,
            modifier = modifier.align(Alignment.Center),
            color = Color.White,
        )
    }
}

@Composable
private fun VolumeAndBrightnessIndicator(
    activeGesture: VerticalGesture,
    state: VolumeAndBrightnessState,
    modifier: Modifier,
) {
    AnimatedVisibility(
        visible = activeGesture !== VerticalGesture.None,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(delayMillis = 1000)),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            if (activeGesture === VerticalGesture.VOLUME) {
                VerticalProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterStart),
                    value = { state.currentVolume },
                    icon = rememberVectorPainter(state.volumeIconState.value)
                )
            } else if (activeGesture === VerticalGesture.BRIGHTNESS) {
                VerticalProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    value = { state.currentBrightness },
                    icon = rememberVectorPainter(state.brightnessIconState.value)
                )
            }
        }
    }
}

private val VolumeAndBrightnessState.brightnessIconState: State<ImageVector>
    @Composable get() = remember {
        derivedStateOf {
            if (currentBrightness > 0.8f) {
                Icons.Rounded.BrightnessHigh
            } else if (currentBrightness < 0.3f) {
                Icons.Rounded.BrightnessLow
            } else {
                Icons.Rounded.BrightnessMedium
            }
        }
    }

private val VolumeAndBrightnessState.volumeIconState: State<ImageVector>
    @Composable get() = remember {
        derivedStateOf {
            if (currentVolume == 0f) {
                Icons.AutoMirrored.Rounded.VolumeOff
            } else if (currentVolume > 0.6f) {
                Icons.AutoMirrored.Rounded.VolumeUp
            } else {
                Icons.AutoMirrored.Rounded.VolumeDown
            }
        }
    }

private const val VERTICAL_GESTURE_SENSITIVITY = 1.75f