@file:androidx.annotation.OptIn(UnstableApi::class)

package com.huanchengfly.tieba.post.core.media

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridConfigurationScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiMediaScope.Posture
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.core.graphics.toRect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.SurfaceType
import androidx.media3.ui.compose.material3.buttons.MuteButton
import androidx.media3.ui.compose.material3.buttons.RepeatButton
import androidx.media3.ui.compose.material3.buttons.SeekBackButton
import androidx.media3.ui.compose.material3.buttons.SeekForwardButton
import androidx.media3.ui.compose.material3.indicator.PositionAndDurationText
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import com.huanchengfly.tieba.post.core.designsystem.layout.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.core.media.buttons.ContentScaleButton
import com.huanchengfly.tieba.post.core.media.buttons.LabeledProgressSlider
import com.huanchengfly.tieba.post.core.media.buttons.MediaFormatsButton
import com.huanchengfly.tieba.post.core.media.buttons.PlayPauseButton
import com.huanchengfly.tieba.post.core.media.buttons.PlaybackSpeedBottomSheetButton
import com.huanchengfly.tieba.post.core.media.state.LocalPlayerGestureState
import com.huanchengfly.tieba.post.core.media.state.PictureInPictureState
import com.huanchengfly.tieba.post.core.media.state.PlayerGestureState
import com.huanchengfly.tieba.post.core.media.state.rememberPictureInPictureState
import com.huanchengfly.tieba.post.core.media.state.rememberPlayerGestureState
import com.huanchengfly.tieba.post.core.media.util.MediaUtil.initialize
import com.huanchengfly.tieba.post.core.ui.component.ActionItem
import com.huanchengfly.tieba.post.core.ui.modifier.clickableNoIndication
import com.huanchengfly.tieba.post.core.ui.modifier.onCase
import com.huanchengfly.tieba.post.core.ui.modifier.onNotNull
import com.huanchengfly.tieba.post.core.ui.util.toastShort
import com.huanchengfly.tieba.post.core.ui.R as CoreUiR

internal enum class VideoPlayerArea { Player, Controller }

internal object PlayerGridConfig {
    // Default: Single area layout with overlay (controls on top of player)
    val defaultConfig: GridConfigurationScope.() -> Unit = {
        row(1f)
        column(1f)
        area(areaId = VideoPlayerArea.Player, row = 1, column = 1)
        area(areaId = VideoPlayerArea.Controller, row = 1, column = 1)
    }

    // Tabletop: Two rows splitting content across the fold
    val tabletopConfig: GridConfigurationScope.() -> Unit = {
        row(0.5f)
        row(0.5f)
        column(1f)
        area(areaId = VideoPlayerArea.Player, row = 1, column = 1) // Top half above fold
        area(areaId = VideoPlayerArea.Controller, row = 2, column = 1) // Bottom half below fold
    }
}

internal val defaultControlButtonColors: IconButtonColors
    @Composable get() = IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.primary
    )

internal val controlGesturePaddings: PaddingValues
    @ReadOnlyComposable @Composable get() = LocalWindowAdaptiveInfo.current.windowSizeClass.let {
        if (it.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) &&
            it.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
        ) {
            PaddingValues(horizontal = 48.dp, vertical = 12.dp)
        } else {
            PaddingValues.Zero
        }
    }

@Composable
fun retainVideoPlayer(
    initialMediaItem: MediaItem,
    playWhenReady: Boolean = true,
): ExoPlayer {
    val context = LocalContext.current
    val player = retain { ExoPlayer.Builder(context.applicationContext).build() }
    var wasPlaying by retain { mutableStateOf(playWhenReady) }

    LifecycleStartEffect(Unit) {
        if (wasPlaying && !player.isPlaying && player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            player.playWhenReady = true
        }

        onStopOrDispose {
            wasPlaying = player.isPlaying
            Util.handlePauseButtonAction(player)
        }
    }

    // Initialize each player only once after we retain it.
    // If the uri (and therefore the player) change, we need to dispose the old player
    // and initialize the new one. Likewise, the player needs to be disposed of when
    // it stops being retained.
    RetainedEffect(player) {
        player.initialize(context, initialMediaItem)

        onRetire {
            if (!player.isReleased) player.release()
        }
    }
    return player
}

/**
 * Video player component for displaying short video preview.
 *
 * @param player The [Player] instance to be controlled and whose content is displayed.
 * @param modifier The [Modifier] to be applied to the outer [Box].
 * @param contentScale The scaling mode to apply to the content within the [ContentFrame].
 * @param surfaceType The type of surface to use for video playback. Can be either
 *   [SURFACE_TYPE_SURFACE_VIEW] or [SURFACE_TYPE_TEXTURE_VIEW].
 * @param shutter A composable that is displayed when the video surface needs to be covered. By
 *   default, this is a black background.
 * @param bottomControls Optional composable aligned with [Alignment.BottomEnd].
 */
@Composable
fun PreviewVideoPlayer(
    player: Player?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    surfaceType: @SurfaceType Int = SURFACE_TYPE_TEXTURE_VIEW,
    shutter: @Composable () -> Unit = { Box(Modifier.fillMaxSize().background(Color.Black)) },
    bottomControls: (@Composable BoxScope.() -> Unit)? = null,
) {
    val colors = defaultControlButtonColors
   Box(modifier = modifier) {
       ContentFrame(player, surfaceType = surfaceType, contentScale = contentScale, shutter = shutter)

       Box(
           modifier = Modifier
               .align(Alignment.TopEnd)
               .padding(12.dp)
               .background(colors.containerColor, MaterialTheme.shapes.extraSmall)
               .padding(horizontal = 6.dp, vertical = 2.dp),
       ) {
           PositionAndDurationText(player, color = colors.contentColor)
       }

       if (bottomControls != null) {
           Box(modifier = Modifier.align(Alignment.BottomEnd), content = bottomControls)
       }
   }
}

/**
 * Video player component, combining a [ContentFrame] for displaying player content with
 * gesture-driven media controls and customizable top controls.
 *
 * @param player The [Player] instance to be controlled and whose content is displayed.
 * @param modifier The [Modifier] to be applied to the outer [Box].
 * @param contentScale The initial scaling mode to apply to the content within the [ContentFrame].
 * @param gestureState The [PlayerGestureState] that manages gesture interactions such as seek
 *   using dragging and tapping gestures.
 * @param pipState The [PictureInPictureState] for picture-in-picture mode button.
 * @param thumbnailUrl Optional URL to be displayed as a shutter over the content. If `null` or
 *   empty, a black [Box] is used.
 * @param topControls A composable aligned with [Alignment.TopCenter].
 */
@Composable
fun VideoPlayer(
    player: Player,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    gestureState: PlayerGestureState = rememberPlayerGestureState(player),
    pipState: PictureInPictureState = rememberPictureInPictureState(player),
    thumbnailUrl: String? = null,
    topControls: (@Composable (PictureInPictureState) -> Unit)? = {
        TopControls(player, Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets), pipState)
    },
) = LookaheadScope {
    var currentContentScale by remember { mutableStateOf(contentScale) }
    val presentationState = rememberPresentationState(player)

    val gridConfig by derivedMediaQuery {
        when {
            pipState.isInPictureInPictureMode -> PlayerGridConfig.defaultConfig
            windowPosture == Posture.Tabletop -> PlayerGridConfig.tabletopConfig
            else -> PlayerGridConfig.defaultConfig
        }
    }

    // Disable auto hide in tabletop posture
    if (gestureState.isEnabled) {
        LaunchedEffect(gridConfig === PlayerGridConfig.tabletopConfig) {
            gestureState.showControls(autoHide = gridConfig !== PlayerGridConfig.tabletopConfig)
        }
    }

    Grid(config = gridConfig, modifier = modifier) {
        Box(
            modifier = Modifier
                .gridItem(areaId = VideoPlayerArea.Player)
                .fillMaxSize()
                .onCase(!pipState.isInPictureInPictureMode) {
                    animateBounds(this@LookaheadScope)
                }
        ) {
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = Modifier
                    .resizeWithContentScale(currentContentScale, presentationState.videoSizeDp)
                    .onCase(pipState.isPipSupported) {
                        onGloballyPositioned { layoutCoordinates ->
                            val videoSize = presentationState.videoSizeDp?.roundToIntSize() ?: return@onGloballyPositioned
                            val sourceRect = layoutCoordinates.boundsInWindow().toAndroidRectF().toRect()
                            pipState.setVideoViewRect(sourceRect, videoSize)
                        }
                    }
            )
            if (presentationState.coverSurface) {
                val shutterModifier = Modifier.matchParentSize().background(Color.Black)
                if (!thumbnailUrl.isNullOrEmpty()) {
                    VideoThumbnail(
                        modifier = shutterModifier,
                        thumbnailUrl = thumbnailUrl,
                        contentScale = currentContentScale,
                        onClick = { Util.handlePlayButtonAction(player) }
                    )
                } else {
                    Box(modifier = shutterModifier)
                }
            }
        }

        Box(
            modifier = Modifier
                .gridItem(areaId = VideoPlayerArea.Controller)
                .fillMaxSize()
                .onCase(!pipState.isInPictureInPictureMode) {
                    animateBounds(this@LookaheadScope)
                }
        ) {
            if (pipState.isInPictureInPictureMode) return@Box // Disable all gestures in PIP mode

            MediaControlGestures(Modifier.matchParentSize(), gestureState)

            CompositionLocalProvider(LocalPlayerGestureState provides gestureState) {
                AnimatedVisibility(gestureState.controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (topControls != null) {
                            Box(Modifier.align(Alignment.TopStart)) {
                                topControls(pipState)
                            }
                        }

                        CenterControls(player, modifier = Modifier.align(Alignment.Center))

                        BottomControlsWithLabeledProgress(
                            player = player,
                            contentScale = currentContentScale,
                            onContentScaleSelected = { currentContentScale = it },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                                .padding(controlGesturePaddings),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Control component typically placed at the top of a video player.
 *
 * This small composable contains a horizontal row of Material3 [IconButton], providing back
 * navigation, download, mute toggle, and media info dialog button.
 *
 * @param player The [Player] to control.
 * @param modifier The [Modifier] to be applied to this top controls composable.
 * @param pipState The [PictureInPictureState] for picture-in-picture mode button.
 * @param onBack An optional callback invoked when the back button is clicked.
 * @param onDownload An optional callback invoked when the download button is clicked. If `null`,
 *   the download button is not shown.
 */
@Composable
fun TopControls(
    player: Player?,
    modifier: Modifier = Modifier,
    pipState: PictureInPictureState? = null,
    onBack: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
) {
    val topButtonColors = IconButtonDefaults.filledTonalIconButtonColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawControlsScrim()
            .padding(PaddingValues(16.dp) + controlGesturePaddings)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            ActionItem(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(CoreUiR.string.core_ui_btn_back),
                colors = IconButtonDefaults.filledIconButtonColors(),
                onClick = onBack,
            )
        }

        Spacer(modifier = Modifier.weight(1.0f))
        if (onDownload != null) {
            ActionItem(
                onClick = onDownload,
                icon = Icons.Rounded.Download,
                contentDescription = stringResource(CoreUiR.string.core_ui_btn_download),
                colors = topButtonColors,
            )
        }
        MuteButton(player, colors = topButtonColors)
        if (pipState != null && pipState.isPipSupported) {
            PictureInPictureButton(pipState, colors = topButtonColors)
        }
        MediaFormatsButton(player, colors = topButtonColors)
    }
}

/**
 * Control component typically placed at the center of a video player.
 *
 * A [androidx.media3.ui.compose.material3.PlayerDefaults.CenterControls] without previous and
 * next button.
 *
 * @param player The [Player] to control.
 * @param modifier The [Modifier] to be applied to this composable.
 * @param buttonColors [IconButtonColors] that will be used to resolve the colors used for all
 *   control button.
 */
@Composable
private fun CenterControls(
    player: Player?,
    modifier: Modifier = Modifier,
    buttonColors: IconButtonColors = defaultControlButtonColors,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeekBackButton(player, Modifier.size(50.dp), colors = buttonColors)
        PlayPauseButton(player, Modifier.size(64.dp), colors = buttonColors)
        SeekForwardButton(player, Modifier.size(50.dp), colors = buttonColors)
    }
}

@Composable
private fun BottomControlsWithLabeledProgress(
    player: Player?,
    contentScale: ContentScale,
    onContentScaleSelected: (ContentScale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gestureState = LocalPlayerGestureState.current
    val windowSize = LocalWindowAdaptiveInfo.current.windowSizeClass
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val iconButtonColors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
    val presetsContentScale = remember {
        listOf(ContentScale.Fit, ContentScale.Crop, ContentScale.FillHeight, ContentScale.FillWidth, ContentScale.FillBounds)
    }

    Column(
        modifier = Modifier
            .drawControlsScrim(reverse = true)
            .clickableNoIndication { /** Block Gestures */ }
            .then(modifier),
    ) {
        LabeledProgressSlider(
            player = player,
            modifier = Modifier
                .fillMaxWidth()
                .onNotNull(gestureState) {
                    reportPointerDown { isDown -> it.showControls(autoHide = !isDown) }
                }
                .padding(horizontal = 16.dp)
        )

        if (windowSize.isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND)) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp), // Make last button visually aligned
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PositionAndDurationText(player = player, color = contentColor)
            Spacer(Modifier.weight(1f))
            PlaybackSpeedBottomSheetButton(
                player = player,
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
            )
            ContentScaleButton(
                contentScale = contentScale,
                onContentScaleSelected = onContentScaleSelected,
                presetsContentScale = presetsContentScale,
                colors = iconButtonColors,
            )
            RepeatButton(
                player = player,
                toggleModeSequence = listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE),
                colors = iconButtonColors,
            )
            FullScreenButton(colors = iconButtonColors)
        }
    }
}

@Composable
private fun FullScreenButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {
    val activity = LocalActivity.current ?: return
    ActionItem(
        modifier = modifier,
        onClick = {
            with(activity) {
                if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                } else {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        },
        icon = Icons.Rounded.ScreenRotation,
        contentDescription = stringResource(id = R.string.btn_full_screen),
        enabled = enabled,
        colors = colors,
    )
}

@Composable
private fun PictureInPictureButton(
    pipState: PictureInPictureState,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {
    val context = LocalContext.current
    val gestureState = LocalPlayerGestureState.current
    ActionItem(
        modifier = modifier,
        onClick = {
            if (!pipState.hasPipPermission) {
                gestureState?.showControls(autoHide = false)
                context.toastShort(R.string.toast_enable_pip_from_settings)
                pipState.openPictureInPictureSettings()
            } else {
                gestureState?.hideControls()
                pipState.enterPictureInPictureMode()
            }
        },
        icon = Icons.Rounded.PictureInPictureAlt,
        contentDescription = stringResource(R.string.btn_picture_in_picture),
        colors = colors,
    )
}

private fun Modifier.drawControlsScrim(scrim: Color = Color.Black, reverse: Boolean = false): Modifier {
    val scrimGradientColors = listOf(scrim.copy(alpha = 0.8f), scrim.copy(alpha = 0.3f), Color.Transparent)
    val backgroundBrush = Brush.verticalGradient(colors = scrimGradientColors)
    return this then Modifier.drawWithCache {
        onDrawBehind {
            if (reverse) {
                rotate(degrees = 180.0f) { drawRect(brush = backgroundBrush) }
            } else {
                drawRect(brush = backgroundBrush)
            }
        }
    }
}

private fun Modifier.reportPointerDown(onPointerDownChange: (Boolean) -> Unit): Modifier =
    this.pointerInput(onPointerDownChange) {
        awaitPointerEventScope {
            var isDown = false
            while (true) {
                val event = awaitPointerEvent()
                val currentlyPressed = event.changes.any { it.pressed }
                if (isDown != currentlyPressed) {
                    isDown = currentlyPressed
                    onPointerDownChange(isDown)
                }
            }
        }
    }
