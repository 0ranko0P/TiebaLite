package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.PresentationState
import androidx.media3.ui.compose.state.rememberPresentationState
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication

/** 使用 [retain] 跨配置变更保留控制器；参数变化通过独立 effect 下发，避免重组重复初始化核心资源 */
@Composable
fun rememberPlayerHandle(
    source: PlayerSource? = null,
    thumbnailUrl: String? = null,
    playWhenReady: Boolean = false,
    @Player.State initialPlaybackState: Int = Player.STATE_IDLE,
    initialWasEnded: Boolean = false,
    displayMode: PlayerDisplayMode = PlayerDisplayMode.Inline,
    syncGroupId: Long? = null,
): PlayerHandle {
    val context = LocalContext.current
    val resumePlaybackState = rememberSaveable { mutableStateOf(false) }
    val controller =
        retain {
            val controllerId = PlayerController.nextControllerId()
            PlayerController(
                context = context.applicationContext,
                initialModel =
                    PlayerUiState(
                        thumbnail = thumbnailUrl,
                        isPlaying = playWhenReady,
                        playbackState = initialPlaybackState,
                        wasEnded = initialWasEnded || initialPlaybackState == Player.STATE_ENDED,
                    ),
                initialDisplayMode = displayMode,
                controllerId = controllerId,
                syncGroupId = syncGroupId ?: controllerId,
                resumePlaybackState = resumePlaybackState,
            )
        }

    LaunchedEffect(source) {
        source?.let { controller.setSource(it) }
    }

    LaunchedEffect(thumbnailUrl) {
        controller.setThumbnailUrl(thumbnailUrl)
    }

    LaunchedEffect(Unit) {
        controller.observeSettings()
    }

    LaunchedEffect(displayMode) {
        controller.setDisplayMode(displayMode)
    }

    return controller
}

/** 协调平台集成与渲染布局；[RetainedEffect] 保证控制器按 retain 生命周期成对初始化和释放 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerContainer(
    modifier: Modifier = Modifier,
    player: PlayerHandle,
    contentScale: ContentScale = ContentScale.Fit,
    backgroundColor: Color = Color.Black,
    fallbackSizeDp: Size? = null,
) {
    if (player.isReleased) return
    val renderer =
        player as? PlayerRenderer
            ?: error("PlayerContainer requires a handle created by rememberPlayerHandle")

    val displayMode by player.displayMode.collectAsStateWithLifecycle()
    val state by player.state.collectAsStateWithLifecycle()

    BindWindowEffect(player)
    SystemUiEffect(displayMode)
    InlinePlaybackEffect(player, displayMode)
    InlineKeepScreenOnEffect(displayMode, state.isPlaying)

    val exoPlayer = renderer.player
    val presentationState = rememberPresentationState(exoPlayer)

    PlayerContent(
        modifier = modifier,
        player = player,
        renderer = renderer,
        exoPlayer = exoPlayer,
        presentationState = presentationState,
        backgroundColor = backgroundColor,
        coverUrl = state.thumbnail,
        contentScale = contentScale,
        fallbackSizeDp = fallbackSizeDp,
        displayMode = displayMode,
    )

    RetainedEffect(Unit) {
        player.initialize()
        onRetire { player.release() }
    }
}

@Composable
private fun BindWindowEffect(player: PlayerHandle) {
    val context = LocalContext.current
    DisposableEffect(player) {
        (context.findActivity() as? ComponentActivity)?.window?.let { player.bindWindow(it) }
        onDispose { player.bindWindow(null) }
    }
}

/** 全屏模式接管沉浸式系统栏；离开全屏或组件移除时恢复，避免影响宿主页面 */
@Composable
private fun SystemUiEffect(displayMode: PlayerDisplayMode) {
    val context = LocalContext.current
    val isFullscreen = displayMode == PlayerDisplayMode.Fullscreen
    val window = (context.findActivity() as? ComponentActivity)?.window
    val insetsController =
        remember(window) {
            window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        }

    DisposableEffect(isFullscreen, insetsController) {
        if (!isFullscreen) {
            onDispose { }
        } else {
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/** 内联模式随 Activity 生命周期暂停/恢复；开启后台播放时跳过暂停 */
@Composable
private fun InlinePlaybackEffect(
    player: PlayerHandle,
    displayMode: PlayerDisplayMode,
) {
    val context = LocalContext.current

    DisposableEffect(context, player, displayMode) {
        val activity = context.findActivity() as? ComponentActivity
        if (activity == null || displayMode != PlayerDisplayMode.Inline) {
            onDispose { }
        } else {
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> player.pauseForLifecycle(false)
                        Lifecycle.Event.ON_RESUME -> player.resumeFromLifecycle()
                        else -> Unit
                    }
                }
            activity.lifecycle.addObserver(observer)
            onDispose { activity.lifecycle.removeObserver(observer) }
        }
    }
}

/** 内联播放时通过 Window flag 保持常亮；离开内联或停止播放时直接清理 */
@Composable
private fun InlineKeepScreenOnEffect(
    displayMode: PlayerDisplayMode,
    isPlaying: Boolean,
) {
    val context = LocalContext.current
    val window = (context.findActivity() as? ComponentActivity)?.window
    val shouldKeepScreenOn = displayMode == PlayerDisplayMode.Inline && isPlaying

    DisposableEffect(window, shouldKeepScreenOn) {
        if (window != null && shouldKeepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** 根据显示模式调整布局：内联监听 Surface 交接，全屏显示控制层，PiP 只保留视频渲染 */
@UnstableApi
@Composable
private fun PlayerContent(
    modifier: Modifier,
    player: PlayerHandle,
    renderer: PlayerRenderer,
    exoPlayer: ExoPlayer,
    presentationState: PresentationState,
    backgroundColor: Color,
    coverUrl: String?,
    contentScale: ContentScale,
    fallbackSizeDp: Size?,
    displayMode: PlayerDisplayMode,
) {
    val isInline = displayMode == PlayerDisplayMode.Inline
    val isPip = displayMode == PlayerDisplayMode.Pip
    val isFullscreen = !isInline
    var showInfo by remember { mutableStateOf(false) }
    var resumeAfterInfo by remember { mutableStateOf(false) }

    if (showInfo && !isPip) {
        val videoInfo = rememberVideoInfo(player, exoPlayer)
        VideoInfoSheet(
            info = videoInfo,
            onDismiss = {
                showInfo = false
                if (resumeAfterInfo) player.play()
            },
        )
    }

    // 仅在显示模式切换时补一次控制层显示；Surface handoff 由控制器同步切换 owner
    LaunchedEffect(player, isInline, isPip) {
        if (isInline) {
            player.showControls()
        } else if (!isPip && !player.selectState { it.isPlaying }) {
            player.showControls(autoHide = false)
        }
    }

    val containerModifier =
        if (isFullscreen) {
            Modifier
                .background(color = backgroundColor)
                .fillMaxSize()
                .then(modifier)
        } else {
            modifier.background(color = backgroundColor)
        }
    val contentAlignment = if (isFullscreen) Alignment.Center else Alignment.TopStart
    val coverScale = if (isFullscreen) contentScale else ContentScale.FillWidth

    Box(
        modifier = containerModifier,
        contentAlignment = contentAlignment,
    ) {
        VideoSurface(
            renderer = renderer,
            player = player,
            presentationState = presentationState,
            coverUrl = coverUrl,
            contentScale = contentScale,
            fallbackSizeDp = fallbackSizeDp,
            coverScale = coverScale,
            useTexture = isInline,
            toggleOnTap = isInline,
        )

        if (!isPip) {
            ControlsOverlay(
                player = player,
                previewPlayerProvider = { renderer.previewPlayer },
                exoPlayer = exoPlayer,
                isFullscreen = isFullscreen,
                presentationState = presentationState,
                onVideoInfoClick = {
                    resumeAfterInfo = player.selectState { it.isPlaying }
                    showInfo = true
                    player.pause()
                },
                onExitFullscreen = { player.toggleFullscreen() },
            )
        }
    }
}

/** 内联使用 TextureView 支持列表动画；全屏使用 SurfaceView 降低渲染开销 */
@OptIn(UnstableApi::class)
@Composable
private fun BoxScope.VideoSurface(
    renderer: PlayerRenderer,
    player: PlayerHandle,
    presentationState: PresentationState,
    coverUrl: String?,
    contentScale: ContentScale,
    fallbackSizeDp: Size?,
    coverScale: ContentScale,
    useTexture: Boolean = false,
    toggleOnTap: Boolean = false,
) {
    val playerState by player.state.collectAsStateWithLifecycle()
    val surfaceOwnerId by renderer.surfaceOwnerId.collectAsStateWithLifecycle()
    val surfaceRenderingEnabled by renderer.surfaceRenderingEnabled.collectAsStateWithLifecycle()
    val videoSizeDp = presentationState.videoSizeDp ?: fallbackSizeDp
    val surfaceType =
        if (useTexture) {
            SURFACE_TYPE_TEXTURE_VIEW
        } else {
            SURFACE_TYPE_SURFACE_VIEW
        }
    val shouldRenderSurface = surfaceRenderingEnabled && (surfaceOwnerId == null || surfaceOwnerId == player.controllerId)

    if (shouldRenderSurface) {
        PlayerSurface(
            player = renderer.player,
            surfaceType = surfaceType,
            modifier =
                Modifier
                    .matchParentSize()
                    .resizeWithContentScale(contentScale, videoSizeDp),
        )
    }

    if (presentationState.coverSurface && !coverUrl.isNullOrBlank()) {
        VideoCover(
            modifier = Modifier.matchParentSize(),
            url = coverUrl,
            contentScale = coverScale,
            showPlay = !playerState.controlsShown && !playerState.isPlaying && playerState.playbackState != Player.STATE_BUFFERING,
        )
    }

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .then(
                    if (toggleOnTap) {
                        Modifier.clickableNoIndication {
                            if (player.selectState { it.controlsShown }) {
                                player.hideControls()
                            } else {
                                player.showControls()
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
    )
}
