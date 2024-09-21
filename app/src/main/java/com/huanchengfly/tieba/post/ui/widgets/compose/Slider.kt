package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.slider.ColorfulSlider
import com.smarttoolfactory.slider.MaterialSliderDefaults
import com.smarttoolfactory.slider.SliderBrushColor
import com.smarttoolfactory.slider.ui.ActiveTrackColor
import com.smarttoolfactory.slider.ui.InactiveTrackColor

val SliderColorNone = SliderBrushColor(Color.Transparent)

@Composable
fun RoundedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    @IntRange(from = 0) steps: Int = 0,
    trackHeight: Dp = 6.dp,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    color: Color = ActiveTrackColor,
    trackColor: Color = InactiveTrackColor
) {
    ColorfulSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        trackHeight = trackHeight,
        thumbRadius = trackHeight * 0.65f,
        colors = MaterialSliderDefaults.materialColors(
            disabledThumbColor = SliderColorNone,
            activeTrackColor = SliderBrushColor(color),
            inactiveTrackColor = SliderBrushColor(trackColor),
            activeTickColor = SliderColorNone,
            inactiveTickColor = SliderColorNone,
            disabledActiveTickColor = SliderColorNone,
            disabledInactiveTickColor = SliderColorNone
        )
    )
}