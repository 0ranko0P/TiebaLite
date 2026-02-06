package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.IntRange
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

private val DefaultThumbSize = DpSize(20.dp, 20.dp)

@Stable
internal fun SliderColors.thumbColor(enabled: Boolean): Color =
    if (enabled) thumbColor else disabledThumbColor

/**
 * The Default thumb for [Slider] and [RangeSlider]
 *
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 *   [Interaction]s for this thumb. You can create and pass in your own `remember`ed instance to
 *   observe
 * @param modifier the [Modifier] to be applied to the thumb.
 * @param colors [SliderColors] that will be used to resolve the colors used for this thumb in
 *   different states. See [SliderDefaults.colors].
 * @param enabled controls the enabled state of this slider. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param thumbSize the size of the thumb.
 */
@Composable
private fun DefaultThumb(
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,
    thumbSize: DpSize = DefaultThumbSize
) {
    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    Spacer(
        modifier
            .size(thumbSize)
            .graphicsLayer {
                scaleX = if (interactions.isNotEmpty()) 1.2f else 1.0f
                scaleY = scaleX
            }
            .hoverable(interactionSource = interactionSource)
            .drawWithCache {
                onDrawWithContent {
                    drawCircle(color = Color.Black.copy(0.2f)) // draw shadow
                    drawCircle(color = colors.thumbColor(enabled), radius = this.size.minDimension / 2.1f)
                }
            }
    )
}

/**
 *
 * Custom Slider with material design 2 style
 *
 * @see androidx.compose.material3.Slider
 *
 * @param value current value of the slider. If outside of [valueRange] provided, value will be
 *   coerced to this range.
 * @param onValueChange callback in which value should be updated
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param onValueChangeFinished called when value change has ended. This should not be used to
 *   update the slider value (use [onValueChange] instead), but rather to know when the user has
 *   completed selecting a new value by ending a drag or a click.
 * @param colors [SliderColors] that will be used to resolve the colors used for this slider in
 *   different states. See [SliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this slider. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this slider in different states.
 * @param steps if positive, specifies the amount of discrete allowable values (in addition to the
 *   endpoints of the value range). Step values are evenly distributed across the range. If 0, the
 *   slider will behave continuously and allow any value from the range. Must not be negative.
 * @param thumb the thumb to be displayed on the slider, it is placed on top of the track. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the slider, it is placed underneath the thumb. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param valueRange range of values that this slider can take. The passed [value] will be coerced
 *   to this range.
 */
@Composable
fun RoundedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.onPrimary
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = {
        DefaultThumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(
            sliderState = sliderState,
            enabled = enabled,
            colors = colors,
            drawTick = { _,_ -> /* NO-OP */ },
            thumbTrackGapSize = Dp.Hairline,
            trackInsideCornerSize = Dp.Hairline
        )
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        steps = steps,
        thumb = thumb,
        track = track,
        valueRange = valueRange
    )
}
