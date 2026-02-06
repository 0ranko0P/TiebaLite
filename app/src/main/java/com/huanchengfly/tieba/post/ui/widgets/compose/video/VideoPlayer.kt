package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.ui.compose.PlayerSurface
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.Grey100
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.getDurationString
import com.huanchengfly.tieba.post.utils.DisplayUtil

internal val LocalVideoPlayerController =
    compositionLocalOf<DefaultVideoPlayerController> { error("VideoPlayerController is not initialized") }

@Composable
fun rememberVideoPlayerController(
    source: VideoPlayerSource? = null,
    thumbnailUrl: String? = null,
    fullScreenModeChangedListener: OnFullScreenModeChangedListener? = null,
    playWhenReady: Boolean = false,
): VideoPlayerController {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return rememberSaveable(
        context, coroutineScope,
        saver = object : Saver<DefaultVideoPlayerController, VideoPlayerState> {
            override fun restore(value: VideoPlayerState): DefaultVideoPlayerController {
                return DefaultVideoPlayerController(
                    context = context,
                    initialState = value.copy(isPlaying = playWhenReady),
                    coroutineScope = coroutineScope,
                    fullScreenModeChangedListener = fullScreenModeChangedListener
                ).apply {
                    source?.let { setSource(it) }
                }
            }

            override fun SaverScope.save(value: DefaultVideoPlayerController): VideoPlayerState {
                return value.currentState { it }
            }
        },
        init = {
            DefaultVideoPlayerController(
                context = context,
                initialState = VideoPlayerState(
                    thumbnailUrl = thumbnailUrl,
                    isPlaying = playWhenReady
                ),
                coroutineScope = coroutineScope,
                fullScreenModeChangedListener = fullScreenModeChangedListener
            ).apply {
                source?.let { setSource(it) }
            }
        }
    )
}

@Composable
fun VideoPlayer(
    videoPlayerController: VideoPlayerController,
    modifier: Modifier = Modifier,
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

    DisposableEffect(Unit) {
        videoPlayerController.initialize()
        onDispose {
            videoPlayerController.release()
        }
    }

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalVideoPlayerController provides videoPlayerController
    ) {
        val startedPlay by videoPlayerController.collect {
            startedPlay || playbackState != Player.STATE_IDLE
        }

        val aspectRatio by videoPlayerController.collect {
            (videoSize.first / videoSize.second).takeUnless { it.isNaN() || it == 0f } ?: 2f
        }

        if (videoPlayerController.supportFullScreen()) {
            val isFullScreen = DisplayUtil.isLandscape

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
            if (startedPlay) {
                PlayerSurface(
                    player = videoPlayerController.exoPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                )

                MediaController()
            } else {
                val thumbnailUrl by videoPlayerController.collect { thumbnailUrl }

                VideoThumbnail(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio),
                    thumbnailUrl = thumbnailUrl,
                    onClick = { videoPlayerController.play() }
                )
            }
        }
    }
}

@Composable
fun BoxScope.MediaController() {
    val videoPlayerController = LocalVideoPlayerController.current
    MediaControlGestures(modifier = Modifier.matchParentSize())

    MediaControlButtons(
        modifier = Modifier.matchParentSize()
    )

    val controlsVisible by videoPlayerController.collect { controlsVisible }

    if (controlsVisible) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .safeContentPadding()
                .align(Alignment.BottomCenter),
        ) {
            ProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                PositionAndDuration()
                Spacer(modifier = Modifier.weight(1f))
                if (videoPlayerController.supportFullScreen()) {
                    FullScreenButton()
                }
            }
        }
    }
}

@Composable
fun PositionAndDuration(
    modifier: Modifier = Modifier
) {
    val controller = LocalVideoPlayerController.current

    val positionText by controller.collect {
        getDurationString(currentPosition, false)
    }
    val durationText by controller.collect {
        getDurationString(duration, false)
    }

    Text(
        "$positionText/$durationText",
        style = TextStyle(
            shadow = Shadow(
                blurRadius = 8f,
                offset = Offset(2f, 2f)
            )
        ),
        modifier = modifier
    )
}

@Composable
private fun FullScreenButton() {
    val videoPlayerController = LocalVideoPlayerController.current
    val icon = if (DisplayUtil.isLandscape) {
        Icons.Rounded.FullscreenExit
    } else {
        Icons.Rounded.Fullscreen
    }
    Box(
        modifier = Modifier
            .padding(8.dp)
            .clickableNoIndication(onClick = videoPlayerController::toggleFullScreen)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = R.string.btn_full_screen)
        )
    }
}

@Composable
fun VideoThumbnail(modifier: Modifier = Modifier, thumbnailUrl: String?, onClick: () -> Unit) {
    Box(
        modifier = modifier.clickableNoIndication(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl != null) {
            GlideImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
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