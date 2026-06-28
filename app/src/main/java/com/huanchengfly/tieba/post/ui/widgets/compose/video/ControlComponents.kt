package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.TiebaUtil
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal fun getDurationString(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"

    return durationMs.milliseconds.toComponents { hours, minutes, seconds, _ ->
        if (hours > 0) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}

private val BaseTextStyle =
    TextStyle(
        fontFamily = FontFamily.Monospace,
        shadow =
            Shadow(
                color = Color.Black.copy(alpha = 0.5f),
                offset = Offset(2f, 2f),
                blurRadius = 8f,
            ),
    )

/** 为文本添加播放器控制层阴影 */
@Composable
internal fun shadowTextStyle(
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
): TextStyle =
    BaseTextStyle.copy(
        fontSize = fontSize,
        fontWeight = fontWeight,
    )

/** 为图标添加播放器控制层阴影 */
@Composable
internal fun ShadowedIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    foregroundColor: Color = Color.White,
) {
    Box(modifier = modifier) {
        Icon(
            modifier =
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = 2f
                        translationY = 2f
                        renderEffect =
                            BlurEffect(
                                8f,
                                8f,
                                TileMode.Clamp,
                            )
                        clip = false
                    },
            painter = painter,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.5f),
        )

        Icon(
            modifier = Modifier.matchParentSize(),
            painter = painter,
            contentDescription = null,
            tint = foregroundColor,
        )
    }
}

/** 播放按钮的可视状态 */
@Immutable
sealed interface PlayButtonState {
    data object Play : PlayButtonState

    data object Pause : PlayButtonState

    data object Replay : PlayButtonState

    data object Loading : PlayButtonState
}

/** 播放/暂停/重播按钮 */
@Composable
internal fun PlayButton(
    state: PlayButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    spec: ControlSpec,
) {
    val buttonSize = spec.centerButtonSize
    val iconSize = spec.centerIconSize
    val loadingStrokeWidthPx = with(LocalDensity.current) { 8.dp.toPx() }
    val loadingStroke = remember(loadingStrokeWidthPx) { Stroke(width = loadingStrokeWidthPx, cap = StrokeCap.Round) }
    val (buttonContent, contentDescription) =
        when (state) {
            PlayButtonState.Play -> {
                IconButtonContent.PlaybackStateIcon(state) to stringResource(R.string.btn_play)
            }

            PlayButtonState.Pause -> {
                IconButtonContent.PlaybackStateIcon(state) to stringResource(R.string.btn_pause)
            }

            PlayButtonState.Replay -> {
                IconButtonContent.PlaybackStateIcon(state) to stringResource(R.string.btn_replay)
            }

            PlayButtonState.Loading -> {
                null to stringResource(R.string.btn_buffering)
            }
        }

    if (state == PlayButtonState.Loading) {
        Box(
            modifier =
                modifier
                    .size(buttonSize)
                    .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(iconSize),
                stroke = loadingStroke,
                trackStroke = loadingStroke,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
            )
        }
        return
    }

    ControlButton(
        onClick = onClick,
        content = requireNotNull(buttonContent),
        contentDescription = contentDescription,
        spec = spec,
        modifier = modifier,
        buttonSize = buttonSize,
        iconSize = iconSize,
    )
}

/** 媒体控制区容器 */
@Composable
internal fun PlayButtonContainer(
    modifier: Modifier = Modifier,
    spec: ControlSpec,
    player: PlayerHandle,
    playerState: PlayerUiState,
) {
    val buttonState =
        when {
            playerState.isPlaying -> PlayButtonState.Pause
            playerState.wasEnded || playerState.hasPlaybackError -> PlayButtonState.Replay
            playerState.playbackState == Player.STATE_BUFFERING -> PlayButtonState.Loading
            else -> PlayButtonState.Play
        }

    PlayButton(
        state = buttonState,
        onClick = { if (buttonState != PlayButtonState.Loading) player.togglePlaying() },
        modifier = modifier,
        spec = spec,
    )
}

/** 控制按钮内容：图标或文本 */
@Immutable
internal sealed interface IconButtonContent {
    data class DrawableIcon(
        @param:DrawableRes val resourceId: Int,
    ) : IconButtonContent

    data class PlaybackStateIcon(
        val state: PlayButtonState,
    ) : IconButtonContent

    data class TextLabel(
        val value: String,
    ) : IconButtonContent
}

/** 通用控制按钮，支持阴影图标或文本 */
@Composable
internal fun ControlButton(
    onClick: () -> Unit,
    content: IconButtonContent,
    contentDescription: String?,
    spec: ControlSpec,
    modifier: Modifier = Modifier,
    buttonSize: Dp = spec.buttonSize,
    iconSize: Dp = spec.iconSize,
    textSize: TextUnit = spec.textSize,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(buttonSize)
                .semantics { contentDescription?.let { this.contentDescription = it } },
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.3f),
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (content) {
                is IconButtonContent.DrawableIcon -> {
                    ShadowedIcon(
                        painter = painterResource(content.resourceId),
                        modifier = Modifier.size(iconSize),
                    )
                }

                is IconButtonContent.PlaybackStateIcon -> {
                    StateIcon(
                        targetState = content.state,
                        icons = PlayIcons,
                        modifier = Modifier.size(iconSize),
                        label = "playbackIcon",
                    )
                }

                is IconButtonContent.TextLabel -> {
                    Text(
                        text = content.value,
                        color = Color.White,
                        maxLines = 1,
                        style = shadowTextStyle(fontSize = textSize),
                    )
                }
            }
        }
    }
}

@Immutable
private data class IconSpec<T>(
    val state: T,
    @param:DrawableRes val resourceId: Int,
)

private val PlayIcons: List<IconSpec<PlayButtonState>> =
    listOf(
        IconSpec(PlayButtonState.Play, R.drawable.ic_play_arrow),
        IconSpec(PlayButtonState.Pause, R.drawable.ic_pause),
        IconSpec(PlayButtonState.Replay, R.drawable.ic_replay),
    )

/** 状态切换动画：当前图标淡入，其余图标淡出并绕 Z 轴翻转 */
@Composable
private fun <T> StateIcon(
    targetState: T,
    icons: List<IconSpec<T>>,
    modifier: Modifier = Modifier,
    label: String = "stateIcon",
) {
    val transition = updateTransition(targetState = targetState, label = label)
    val iconStateIndexMap =
        remember(icons) {
            icons.mapIndexed { index, icon -> icon.state to index }.toMap()
        }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        icons.forEachIndexed { index, icon ->
            key(icon.state) {
                val alpha by transition.animateFloat(
                    transitionSpec = {
                        tween(
                            durationMillis = 100,
                            delayMillis = if (targetState == icon.state) 60 else 0,
                            easing = FastOutSlowInEasing,
                        )
                    },
                    label = "$label-alpha-$index",
                ) { state ->
                    if (state == icon.state) 1f else 0f
                }
                val rotation by transition.animateFloat(
                    transitionSpec = { tween(durationMillis = 200, easing = LinearOutSlowInEasing) },
                    label = "$label-rotation-$index",
                ) { state ->
                    val activeIndex = iconStateIndexMap[state] ?: 0
                    ((index - activeIndex).coerceIn(-1, 1) * -90).toFloat()
                }

                ShadowedIcon(
                    painter = painterResource(icon.resourceId),
                    modifier =
                        Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                this.alpha = alpha
                                this.rotationZ = rotation
                            },
                )
            }
        }
    }
}

/** 视频信息弹窗：展示分辨率、时长、文件大小和URL */
@Composable
internal fun VideoInfoSheet(
    info: VideoInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val valueTextColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.title_video_info)) },
        text = {
            Column {
                InfoItem(label = stringResource(id = R.string.label_resolution)) {
                    Text(
                        text = info.resolution,
                        color = valueTextColor,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                InfoItem(label = stringResource(id = R.string.label_duration)) {
                    Text(
                        text = info.duration,
                        color = valueTextColor,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                InfoItem(label = stringResource(id = R.string.label_file_size)) {
                    Text(
                        text = info.fileSize,
                        color = valueTextColor,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                InfoItem(label = stringResource(id = R.string.label_video_url)) {
                    Text(
                        text = stringResource(id = R.string.label_click_to_copy),
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline,
                        modifier =
                            Modifier.clickable {
                                TiebaUtil.copyText(context, info.videoUrl)
                            },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_close))
            }
        },
    )
}

/** 单行信息项 */
@Composable
private fun InfoItem(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
}

private val SpeedOptions = listOf(0.25f, 0.5f, 1f, 1.25f, 1.5f, 2f, 4f)
internal const val SPEED_OFF = "disabled"
internal const val SPEED_AUTO = "auto_double"
internal const val SPEED_NORMAL = 1f

/** 格式化倍速显示文本 */
internal fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) {
        speed.toInt().toString()
    } else {
        speed.toString()
    }

/** 倍速按钮 */
@Composable
internal fun SpeedButton(
    spec: ControlSpec,
    currentSpeed: Float,
    onToggleSlider: () -> Unit,
) {
    val speedLabel = formatSpeed(currentSpeed)
    val contentDescription = "${stringResource(R.string.btn_playback_speed)} ${speedLabel}X"
    val content =
        if (currentSpeed == SPEED_NORMAL) {
            IconButtonContent.DrawableIcon(R.drawable.ic_speed)
        } else {
            IconButtonContent.TextLabel("${speedLabel}X")
        }

    ControlButton(
        onClick = onToggleSlider,
        content = content,
        contentDescription = contentDescription,
        spec = spec,
    )
}

/** 倍速调节滑块 */
@Composable
fun SpeedSlider(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onInteracting: () -> Unit = {},
    trackHeight: Dp,
) {
    val selectedIndex = remember(currentSpeed) { SpeedOptions.indexOf(currentSpeed).takeIf { it >= 0 } ?: 2 }
    var sliderValue by remember(selectedIndex) { mutableFloatStateOf(selectedIndex.toFloat()) }
    var lastHapticIndex by remember(selectedIndex) { mutableIntStateOf(selectedIndex) }
    val interactionSource = remember { MutableInteractionSource() }
    val thumbSize = DpSize(SliderThumbWidth, trackHeight * 3f)
    val hapticFeedback = LocalHapticFeedback.current

    Slider(
        value = sliderValue,
        onValueChange = {
            onInteracting()
            sliderValue = it
            val optionIndex = it.roundToInt().coerceIn(0, SpeedOptions.lastIndex)
            if (optionIndex != lastHapticIndex) {
                lastHapticIndex = optionIndex
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onSpeedChange(SpeedOptions[optionIndex])
            }
        },
        onValueChangeFinished = {
            val optionIndex = sliderValue.roundToInt().coerceIn(0, SpeedOptions.lastIndex)
            sliderValue = optionIndex.toFloat()
            lastHapticIndex = optionIndex
            onSpeedChange(SpeedOptions[optionIndex])
        },
        valueRange = 0f..SpeedOptions.lastIndex.toFloat(),
        steps = SpeedOptions.size - 2,
        modifier = modifier,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = thumbSize,
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(trackHeight),
                drawTick = { _, _ -> },
            )
        },
    )
}
