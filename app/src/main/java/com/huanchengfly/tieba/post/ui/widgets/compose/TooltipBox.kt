package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

val SpacingBetweenTooltipAndAnchor = (-12).dp

/**
 * Material TooltipBox that wraps a composable with a tooltip.
 *
 * Tooltips provide a descriptive message for an anchor. It can be used to call the users attention
 * to the anchor.
 *
 * @param positionProvider [PopupPositionProvider] that will be used to place the tooltip relative
 *   to the anchor content.
 * @param state handles the state of the tooltip's visibility.
 * @param modifier the [Modifier] to be applied to the TooltipBox.
 * @param hasAction whether the associated tooltip contains an action.
 * @param content the composable that the tooltip will anchor to.
 */
@NonRestartableComposable
@Composable
fun PlainTooltipBox(
    modifier: Modifier = Modifier,
    positionProvider: PopupPositionProvider = rememberTooltipPositionProvider(
        positioning = TooltipAnchorPosition.Above,
        spacingBetweenTooltipAndAnchor = SpacingBetweenTooltipAndAnchor
    ),
    contentDescription: String,
    state: TooltipState = rememberTooltipState(),
    hasAction: Boolean = false,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = {
            PlainTooltip { Text(text = contentDescription) }
        },
        state = state,
        hasAction = hasAction,
        content = content
    )
}