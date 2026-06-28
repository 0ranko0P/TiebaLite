package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlin.math.roundToLong

@Stable
internal class SmoothProgressState(
    private val player: Player,
) {
    var durationMs by mutableLongStateOf(0L)
        private set

    // 实际渲染给 UI 的当前位置，会在采样点之间做线性补间。
    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var bufferedPositionMs by mutableLongStateOf(0L)
        private set

    // 低频轮询得到的播放器真实位置，作为每段动画的目标值。
    var sampledPositionMs by mutableLongStateOf(0L)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    private var snapSequence by mutableIntStateOf(0)

    fun updateIsPlaying(playing: Boolean) {
        isPlaying = playing
    }

    fun refresh() {
        val duration = player.duration.takeIf { it > 0L } ?: 0L
        durationMs = duration
        sampledPositionMs = player.currentPosition.coercePosition(duration)
        bufferedPositionMs = player.bufferedPosition.coercePosition(duration)
    }

    fun snapTo(positionMs: Long) {
        val duration = durationMs.takeIf { it > 0L } ?: (player.duration.takeIf { it > 0L } ?: 0L)
        durationMs = duration
        val snappedPosition = positionMs.coercePosition(duration)
        sampledPositionMs = snappedPosition
        currentPositionMs = snappedPosition
        bufferedPositionMs = player.bufferedPosition.coercePosition(duration)
        // 位置突变时强制终止当前补间，避免 seek 后又被旧动画拉回。
        snapSequence++
    }

    fun syncAnimatedPosition(animatedProgress: Float) {
        currentPositionMs =
            if (durationMs > 0L) {
                (durationMs * animatedProgress.coerceIn(0f, 1f)).roundToLong()
            } else {
                0L
            }
    }

    fun animationSnapshot(): ProgressAnimationSnapshot =
        ProgressAnimationSnapshot(
            sampledPositionMs = sampledPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            snapSequence = snapSequence,
        )

    data class ProgressAnimationSnapshot(
        val sampledPositionMs: Long,
        val durationMs: Long,
        val isPlaying: Boolean,
        val snapSequence: Int,
    ) {
        val targetProgress: Float
            get() =
                if (durationMs > 0L) {
                    (sampledPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
    }
}

internal fun Long.coercePosition(durationMs: Long): Long =
    if (durationMs > 0L) {
        coerceIn(0L, durationMs)
    } else {
        coerceAtLeast(0L)
    }

private fun Player.Events.shouldSnapProgress(): Boolean =
    contains(Player.EVENT_POSITION_DISCONTINUITY) || contains(Player.EVENT_MEDIA_ITEM_TRANSITION)

private fun Player.Events.shouldRefreshProgress(): Boolean =
    contains(Player.EVENT_POSITION_DISCONTINUITY) ||
        contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
        contains(Player.EVENT_TIMELINE_CHANGED) ||
        contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
        contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)

@Composable
internal fun rememberSmoothProgressState(
    player: Player,
    frameUpdatesEnabled: Boolean,
): SmoothProgressState {
    val progressState = remember(player) { SmoothProgressState(player) }

    DisposableEffect(player, progressState) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    progressState.updateIsPlaying(playing)
                }

                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    if (events.shouldSnapProgress()) {
                        progressState.snapTo(player.currentPosition)
                    } else if (events.shouldRefreshProgress()) {
                        // 状态变化后尽快刷新一次，减少 UI 与播放器状态短暂不同步。
                        progressState.refresh()
                    }
                }
            }
        player.addListener(listener)
        progressState.updateIsPlaying(player.isPlaying)
        progressState.refresh()
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(progressState, frameUpdatesEnabled, progressState.isPlaying) {
        val pollIntervalMs =
            if (frameUpdatesEnabled) {
                200L
            } else {
                1000L
            }
        progressState.refresh()

        while (isActive && progressState.isPlaying) {
            delay(pollIntervalMs)
            progressState.refresh()
        }
    }

    LaunchedEffect(progressState, frameUpdatesEnabled) {
        var initialized = false
        var lastSnapSequence = progressState.animationSnapshot().snapSequence

        snapshotFlow { progressState.animationSnapshot() }.collectLatest { snapshot ->
            val forceSnap = snapshot.snapSequence != lastSnapSequence
            val shouldAnimate = frameUpdatesEnabled && snapshot.isPlaying && snapshot.durationMs > 0L && !forceSnap
            lastSnapSequence = snapshot.snapSequence

            if (!initialized || !shouldAnimate) {
                // 首次进入、暂停、隐藏控件或发生 seek 时，直接同步到目标位置。
                progressState.syncAnimatedPosition(snapshot.targetProgress)
                initialized = true
                return@collectLatest
            }

            val startProgress =
                if (snapshot.durationMs > 0L) {
                    progressState.currentPositionMs.toFloat() / snapshot.durationMs.toFloat()
                } else {
                    0f
                }

            // 每次采样之间用线性动画补齐，消除低频轮询带来的跳动感。
            Animatable(startProgress.coerceIn(0f, 1f)).animateTo(
                targetValue = snapshot.targetProgress,
                animationSpec =
                    tween(
                        durationMillis = 200,
                        easing = LinearEasing,
                    ),
            ) {
                progressState.syncAnimatedPosition(value)
            }
        }
    }

    return progressState
}
