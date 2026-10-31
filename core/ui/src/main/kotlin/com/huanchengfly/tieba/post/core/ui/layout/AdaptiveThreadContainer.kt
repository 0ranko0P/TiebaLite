package com.huanchengfly.tieba.post.core.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.WidthSizeClasses
import kotlin.math.max

@Composable
fun AdaptiveThreadContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = AdaptiveMeasurePolicy
    )
}

private object AdaptiveMeasurePolicy : MeasurePolicy {
    val ContentMinWidth = 360.dp
    val ContentMaxWidth = 524.dp

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        if (measurables.isEmpty()) {
            return layout(constraints.minHeight, constraints.minWidth) {}
        }
        require(measurables.size == 1) { "AdaptiveContainer expects exactly one child" }
        val measurable = measurables.first()
        val placeable = if (!constraints.hasBoundedWidth) {
            measurable.measure(
                constraints.constrain(Constraints.fixedHeight(height = constraints.maxHeight))
            )
        } else {
            val widthDp = constraints.maxWidth.toDp()
            val contentWidth = when {
                widthDp >= WindowSizeClass.WidthSizeClasses.Expanded -> 0.63f * widthDp
                widthDp >= WindowSizeClass.WidthSizeClasses.Medium -> 0.87f * widthDp
                else -> widthDp
            }.coerceIn(ContentMinWidth, ContentMaxWidth).roundToPx()
            measurable.measure(
                constraints.copy(
                    minWidth = max(constraints.minWidth, ContentMinWidth.roundToPx()),
                    maxWidth = contentWidth,
                )
            )
        }
        val layoutHeight = placeable.height.coerceIn(
            constraints.minHeight,
            if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
        )
        val layoutWidth = constraints.maxWidth.coerceAtLeast(ContentMinWidth.roundToPx())

        return layout(layoutWidth, layoutHeight) {
            placeable.placeRelative(x = (layoutWidth - placeable.width) / 2, y = 0)
        }
    }
}
