package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.PresentationState
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickCount
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.Grey100
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.getDurationString
import com.huanchengfly.tieba.post.utils.DisplayUtil

internal val LocalVideoPlayerController =
    compositionLocalOf<DefaultVideoPlayerController> { error("VideoPlayerController is not initialized") }

@Composable
fun retainVideoPlayerController(
    source: VideoPlayerSource? = null,
    thumbnailUrl: String? = null,
    fullScreenModeChangedListener: OnFullScreenModeChangedListener? = null,
    playWhenReady: Boolean = false,
): VideoPlayerController {
    val context = LocalContext.current
    val videoPlayerController = retain {
        DefaultVideoPlayerController(
            context = context.applicationContext,
            initialState = VideoPlayerState(
                thumbnailUrl = thumbnailUrl,
                isPlaying = playWhenReady
            ),
        ).apply {
            source?.let { setSource(it) }
        }
    }

    DisposableEffect(fullScreenModeChangedListener) {
        videoPlayerController.setFullScreenModeChangedListener(fullScreenModeChangedListener)
        onDispose {
            videoPlayerController.setFullScreenModeChangedListener(null)
        }
    }

    return videoPlayerController
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayerController: VideoPlayerController,
    contentScale: ContentScale = ContentScale.Fit,
    controlsEnabled: Boolean = true,
    gesturesEnabled: Boolean = true,
    backgroundColor: Color = Color.Black
) {
    require(videoPlayerController is DefaultVideoPlayerController) {
        "Use [rememberVideoPlayerController] to create an instance of [VideoPlayerController]"
    }

    SideEffect {
        videoPlayerController.enableControls(controlsEnabled)
        videoPlayerController.enableGestures(gesturesEnabled)
    }

    RetainedEffect(Unit) {
        videoPlayerController.initialize()
        onRetire {
            videoPlayerController.release()
        }
    }

    val presentationState = rememberPresentationState(videoPlayerController.exoPlayer)
    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalVideoPlayerController provides videoPlayerController
    ) {
        val isFullScreen = DisplayUtil.isLandscape
        if (videoPlayerController.supportFullScreen()) {

            BackHandler(enabled = isFullScreen) {
                videoPlayerController.toggleFullScreen()
            }
        }

        Box(
            modifier = Modifier
                .background(color = backgroundColor)
                .fillMaxSize()
                .then(modifier),
            contentAlignment = Alignment.Center
        ) {
            PlayerSurface(
                player = videoPlayerController.exoPlayer,
                modifier = Modifier
                    .matchParentSize()
                    .resizeWithContentScale(contentScale, presentationState.videoSizeDp),
                surfaceType = SURFACE_TYPE_SURFACE_VIEW
            )

            if (presentationState.coverSurface) {
                val thumbnailUrl by videoPlayerController.collect { thumbnailUrl }
                if (!thumbnailUrl.isNullOrEmpty()) {
                    VideoThumbnail(
                        modifier = Modifier.matchParentSize(),
                        thumbnailUrl = thumbnailUrl,
                        contentScale = if (isFullScreen) ContentScale.FillHeight else ContentScale.FillWidth,
                        onClick = { videoPlayerController.play() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                }
            }

            MediaController(presentationState)
        }
    }
}

@UnstableApi
@Composable
fun BoxScope.MediaController(
    presentationState: PresentationState,
) {
    val coroutineScope = rememberCoroutineScope()
    val videoPlayerController = LocalVideoPlayerController.current
    val progressStateWithTick = rememberProgressStateWithTickInterval(
        player = videoPlayerController.exoPlayer,
        tickIntervalMs = 500,
        scope = coroutineScope,
    )

    MediaControlGestures(
        modifier = Modifier
            .matchParentSize()
            .windowInsetsPadding(WindowInsets.safeGestures),
        durationProvider = { progressStateWithTick.durationMs },
        positionProvider = { progressStateWithTick.currentPositionMs }
    )

    MediaControlButtons(
        modifier = Modifier.align(Alignment.Center)
    )

    val controlsVisible by videoPlayerController.collect { controlsVisible }

    if (controlsVisible) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .clickableNoIndication { /** Block Gestures */ }
                .onCase(DisplayUtil.isLandscape) { padding(horizontal = 16.dp) }
                .padding(start = 16.dp, end = Dp.Hairline, bottom = 8.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.Bottom,
        ) {
            PositionAndDuration(
                modifier = Modifier.minimumInteractiveComponentSize(),
                progressStateWithTick = progressStateWithTick,
            )

            Spacer(modifier = Modifier.width(8.dp))

            ProgressIndicator(
                modifier = Modifier.minimumInteractiveComponentSize().weight(1.0f),
                state = rememberProgressStateWithTickCount(
                    player = videoPlayerController.exoPlayer,
                    totalTickCount = (progressStateWithTick.durationMs / 300).toInt().coerceAtLeast(1),
                    scope = coroutineScope
                ),
                durationMs = progressStateWithTick.durationMs,
                videoSize = presentationState.videoSizeDp,
            )

            if (videoPlayerController.supportFullScreen()) {
                FullScreenButton(onClick = videoPlayerController::toggleFullScreen)
            } else {
                Spacer(modifier = Modifier.minimumInteractiveComponentSize())
            }
        }
    }
}

@UnstableApi
@Composable
private fun PositionAndDuration(
    modifier: Modifier = Modifier,
    progressStateWithTick: ProgressStateWithTickInterval,
) {
    val positionText = getDurationString(progressStateWithTick.currentPositionMs.coerceAtLeast(0), false)
    val durationText = getDurationString(progressStateWithTick.durationMs.coerceAtLeast(0), false)
    val durationTextStyle = remember {
        TextStyle(
            shadow = Shadow(blurRadius = 8f, offset = Offset(2f, 2f))
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = "$positionText / $durationText", style = durationTextStyle)
    }
}

@Composable
private fun FullScreenButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icon = if (DisplayUtil.isLandscape) {
        Icons.Rounded.FullscreenExit
    } else {
        Icons.Rounded.Fullscreen
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = R.string.btn_full_screen)
        )
    }
}

@Composable
fun VideoThumbnail(
    modifier: Modifier = Modifier,
    thumbnailUrl: String?,
    contentScale: ContentScale = ContentScale.FillWidth,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.clickableNoIndication(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl != null) {
            GlideImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(id = R.string.btn_play),
            modifier = Modifier.size(48.dp),
            tint = Grey100
        )
    }
}