package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 页面宿主转发给内容层的 PiP 操作 */
sealed interface PipAction {
    data object PlayPause : PipAction

    data object Replay : PipAction
}

/** 宿主生命周期事件，由 [FullscreenPage] 观察 Activity 后发出 */
sealed interface FullscreenEvent {
    data class Pause(
        val isInPip: Boolean,
    ) : FullscreenEvent

    data object Resume : FullscreenEvent
}

private data class FullscreenPlaybackSnapshot(
    val isPlaying: Boolean,
    val wasEnded: Boolean,
)

/** 内容层依赖宿主实现的导航、方向、PiP 和播放状态回调 */
class FullscreenCallbacks(
    val onExit: () -> Unit,
    val onOrientation: (Int) -> Unit,
    val onPlaybackState: (isPlaying: Boolean, wasEnded: Boolean) -> Unit,
    val onEnterPip: (isPlaying: Boolean, wasEnded: Boolean) -> Unit,
    val getRequestedOrientation: () -> Int? = { null },
)

/** 宿主层注入给内容层的事件流与平台能力 */
class FullscreenInput(
    val pipActions: Flow<PipAction>,
    val pipState: StateFlow<FullscreenStore.PipState>,
    val fullscreenEvents: Flow<FullscreenEvent>,
    val pipSupported: Boolean,
)

/** 全屏视频播放内容层，不持有 Activity、NavHostController 或 Window；通过回调和事件流与页面宿主通信 */
@UnstableApi
@Composable
fun FullscreenContent(
    args: FullscreenArgs,
    input: FullscreenInput,
    hooks: FullscreenCallbacks,
    modifier: Modifier = Modifier,
) {
    val pipSessionId = remember(args.syncGroupId, args.videoUrl) { args.syncGroupId ?: FullscreenStore.nextPipSessionId() }
    val pipState by input.pipState.collectAsState()
    val isInPip = pipState.isInPip && pipState.sessionId == pipSessionId
    val currentHooks by rememberUpdatedState(hooks)
    val syncGroupId = args.syncGroupId
    val hasInline = args.inlineControllerId != 0L
    val fallbackVideoSize =
        remember(args.videoWidth, args.videoHeight) {
            if (args.videoWidth > 0 && args.videoHeight > 0) {
                Size(args.videoWidth.toFloat(), args.videoHeight.toFloat())
            } else {
                Size(16f, 9f)
            }
        }

    var exitCleanupDone by remember { mutableStateOf(false) }

    val source =
        remember(args) {
            PlayerSource(
                url = args.videoUrl,
                initialSpeed = 1f,
                title = args.title,
                artworkUrl = args.thumbnailUrl,
            )
        }

    val controller =
        rememberPlayerHandle(
            source = source,
            thumbnailUrl = args.thumbnailUrl,
            playWhenReady = args.playWhenReady,
            displayMode =
                if (isInPip) {
                    PlayerDisplayMode.Pip
                } else {
                    PlayerDisplayMode.Fullscreen
                },
            syncGroupId = syncGroupId,
        )

    // 退出和销毁共用同一路径，保证快照保存和前台服务清理只执行一次
    fun saveAndStopOnce() {
        if (exitCleanupDone) return
        exitCleanupDone = true
        val state = controller.state.value
        // 仅有内嵌接收方时保存一次性快照，避免留下不可消费状态
        if (hasInline) {
            controller.setProgressTransitioning(true)
            val position = controller.positionMs()
            FullscreenStore.saveRestoreState(
                url = args.videoUrl,
                position = position,
                play = state.isPlaying,
                speed = state.playbackSpeed,
                syncGroupId = controller.syncGroupId,
            )
            FullscreenStore.saveRequestedOrientation(
                url = args.videoUrl,
                orientation = currentHooks.getRequestedOrientation(),
            )
        }
        // 无内嵌播放器接管时停止服务，避免后台空跑
        if (syncGroupId == null) controller.stopMediaService()
    }

    fun exitFullscreen() {
        val cleanupWasDone = exitCleanupDone
        saveAndStopOnce()
        if (!cleanupWasDone) {
            if (hasInline) {
                // 有内嵌接收方时只释放当前全屏 owner，由恢复后的内嵌控制器按同步组主动 claim。
                controller.transferSurfaceToInline()
            } else if (syncGroupId != null) {
                // 无具体接收方时只释放 claim，避免广播给不存在的控制器
                controller.releaseSurface()
            }
        }
        currentHooks.onExit()
    }

    fun handleBack(): Boolean {
        val state = controller.state.value
        return if (state.isLocked) {
            controller.toggleLock()
            true
        } else {
            false
        }
    }

    BackHandler {
        if (!handleBack()) exitFullscreen()
    }

    // 生命周期绑定：PiP 会话、播放驱逐、全屏命令、退出清理
    DisposableEffect(pipSessionId, controller) {
        FullscreenStore.bindPipSession(pipSessionId)

        controller.onPlaybackEvicted {
            controller.pause()
            val inPip =
                input.pipState.value.let { it.isInPip && it.sessionId == pipSessionId }
            if (inPip) currentHooks.onExit()
        }

        controller.setFullscreenListener(
            object : FullscreenListener {
                override fun onFullscreenCommand(event: FullscreenCommand) {
                    when (event) {
                        is FullscreenCommand.Toggle -> exitFullscreen()
                        is FullscreenCommand.Rotate -> currentHooks.onOrientation(event.orientation)
                    }
                }
            },
        )

        onDispose {
            saveAndStopOnce()
            FullscreenStore.clearPipSession(pipSessionId)
            controller.clearPlaybackEviction()
            controller.setFullscreenListener(null)
            if (syncGroupId != null) controller.releaseSurface()
        }
    }

    // 一次性初始化 + 宿主事件 + PiP 流
    LaunchedEffect(controller) {
        if (syncGroupId != null) controller.claimSurface()
        controller.configurePip(input.pipSupported) {
            val s = controller.state.value
            currentHooks.onEnterPip(s.isPlaying, s.wasEnded)
        }
        controller.setProgressTransitioning(false)
        if (args.playWhenReady) controller.play()

        // 宿主生命周期 → 控制器
        launch {
            input.fullscreenEvents.collect { event ->
                when (event) {
                    is FullscreenEvent.Pause -> controller.pauseForLifecycle(event.isInPip || exitCleanupDone)
                    FullscreenEvent.Resume -> controller.resumeFromLifecycle()
                }
            }
        }

        // PiP 操作 → 播放器命令
        launch {
            input.pipActions.collect { action ->
                when (action) {
                    PipAction.PlayPause -> controller.togglePlaying()
                    PipAction.Replay -> controller.replay()
                }
            }
        }

        // 播放状态 → 宿主回调
        var lastSnapshot: FullscreenPlaybackSnapshot? = null
        controller.state.collect {
            val snapshot =
                FullscreenPlaybackSnapshot(
                    isPlaying = it.isPlaying,
                    wasEnded = it.wasEnded,
                )
            if (snapshot != lastSnapshot) {
                lastSnapshot = snapshot
                currentHooks.onPlaybackState(snapshot.isPlaying, snapshot.wasEnded)
            }
        }
    }

    PlayerContainer(
        player = controller,
        modifier = modifier,
        fallbackSizeDp = fallbackVideoSize,
    )
}
