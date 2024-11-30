package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = ExtendedTheme.colors.divider,
    height: Dp = 16.dp,
    width: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .width(width)
            .background(color = color)
    )
}

@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = ExtendedTheme.colors.divider,
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp
) {
    Divider(modifier = modifier, color = color, thickness = thickness, startIndent = startIndent)
}

/**
 * Lift up Bottom Bar if enabled
 *
 * @see com.huanchengfly.tieba.post.utils.AppPreferencesUtils.KEY_LIFT_BOTTOM_BAR
 * */
@NonRestartableComposable
@Composable
fun LiftUpSpacer() {
    if (LocalContext.current.appPreferences.liftUpBottomBar) {
        Spacer( modifier = Modifier.height(16.dp))
    }
}
