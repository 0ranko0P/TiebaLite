package com.huanchengfly.tieba.post.core.ui.modifier

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline

/**
 * Draws [shape] with animated [color] behind the content without causing recompose.
 * */
fun Modifier.animateBackground(
    color: Color,
    shape: Shape = RectangleShape,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessMedium),
) = composed(
    inspectorInfo = {
        name = "animateBackground"
        properties["color"] = color
        properties["shape"] = shape
    }
) {
    if (color == Color.Transparent) return@composed this
    val backgroundAni by animateColorAsState(targetValue = color, animationSpec = animationSpec)
    drawBehind {
        if (shape == RectangleShape) {
            drawRect(backgroundAni)
        } else  {
            drawOutline(shape.createOutline(size, layoutDirection, this), backgroundAni)
        }
    }
}
