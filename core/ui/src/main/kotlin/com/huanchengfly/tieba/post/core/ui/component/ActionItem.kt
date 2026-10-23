package com.huanchengfly.tieba.post.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.PopupPositionProvider

@Composable
fun ActionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        positioning = TooltipAnchorPosition.Above,
        spacingBetweenTooltipAndAnchor = SpacingBetweenTooltipAndAnchor
    ),
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    onClick: () -> Unit
) {
    PlainTooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        contentDescription = contentDescription,
        hasAction = true,
    ) {
        IconButton(onClick = onClick, enabled = enabled, colors = colors) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@NonRestartableComposable
@Composable
fun ActionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    @StringRes contentDescription: Int,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        positioning = TooltipAnchorPosition.Above,
        spacingBetweenTooltipAndAnchor = SpacingBetweenTooltipAndAnchor
    ),
    enabled: Boolean = true,
    onClick: () -> Unit
) = ActionItem(
    modifier = modifier,
    icon = icon,
    contentDescription = stringResource(contentDescription),
    positionProvider = positionProvider,
    enabled = enabled,
    onClick = onClick
)

