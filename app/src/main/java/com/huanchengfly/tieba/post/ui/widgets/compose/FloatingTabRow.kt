package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FloatingTabRow(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    elevation: Dp = 6.dp,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color,
        shadowElevation = elevation
    ) {
        ProvideTextStyle(MaterialTheme.typography.titleSmall) {
            Row(modifier = Modifier.padding(8.dp), content = content)
        }
    }
}

/**
 *
 * Tabs organize content across different screens, data sets, and other interactions.
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param icon the icon displayed in this tab
 * @param colors colors used in this tab in selected/unselected states.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this tab. You can use this to change the tab's appearance or
 *   preview the tab in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this tab
 */
@Composable
fun FloatingTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (RowScope.() -> Unit)? = null,
    colors: ButtonColors = defaultFloatingTabColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    // The color of the Ripple should always the selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
    val ripple = ripple(bounded = true, color = colors.contentColor)

    TabTransition(colors.contentColor, colors.disabledContentColor, selected) {
        Row(
            modifier = modifier
                .defaultMinSize(
                    minWidth = ButtonDefaults.MinWidth,
                    minHeight = ButtonDefaults.MinHeight
                )
                .clip(shape = CircleShape)
                .background(color = colors.containerColor(selected))
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = ripple
                )
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            content()
        }
    }
}

/**
 * Transition defining how the tint color for a tab animates, when a new tab is selected. This
 * component uses [LocalContentColor] to provide an interpolated value between [activeColor] and
 * [inactiveColor] depending on the animation status.
 */
@Composable
private fun TabTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable () -> Unit
) {
    val color by animateColorAsState(if (selected) activeColor else inactiveColor)
    CompositionLocalProvider(LocalContentColor provides color, content = content)
}

@Composable
private fun defaultFloatingTabColors() = with(MaterialTheme.colorScheme) {
    ButtonColors(
        containerColor = secondaryContainer,
        contentColor = onSecondaryContainer,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = onSurface
    )
}
