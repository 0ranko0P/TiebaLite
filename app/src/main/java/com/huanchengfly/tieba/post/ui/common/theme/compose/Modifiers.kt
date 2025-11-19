package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.semantics.Role

inline fun Modifier.block(modifier: Modifier.() -> Modifier?): Modifier = this then (modifier(Modifier) ?: Modifier)

inline fun Modifier.onCase(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) this then (modifier(Modifier)) else this
}

inline fun <T> Modifier.onNotNull(obj: T?, modifier: Modifier.(T) -> Modifier): Modifier {
    return if (obj != null) this then (modifier(obj)) else this
}

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and never show any
 * indication.
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickableNoIndication(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = this then Modifier.clickable(
    interactionSource = null,
    indication = null,
    enabled = enabled,
    onClickLabel = null,
    role = role,
    onClick = onClick
)

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
