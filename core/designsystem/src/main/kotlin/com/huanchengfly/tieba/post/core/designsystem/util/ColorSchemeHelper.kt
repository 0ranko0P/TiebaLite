package com.huanchengfly.tieba.post.core.designsystem.util

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils.calculateLuminance

val ColorScheme.isTranslucent: Boolean
    get() = surface == Color.Transparent

val ColorScheme.isDarkScheme: Boolean
    get() = isColorLight(onSurface)

fun ColorScheme.createTopAppBarColors(
    containerColor: Color = surface,
    scrolledContainerColor: Color = surfaceContainer,
    navigationIconContentColor: Color = onSurface,
    titleContentColor: Color = onSurface,
    actionIconContentColor: Color = onSurfaceVariant,
    subtitleContentColor: Color = onSurfaceVariant
): TopAppBarColors {
    return TopAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = scrolledContainerColor,
        navigationIconContentColor = navigationIconContentColor,
        titleContentColor = titleContentColor,
        actionIconContentColor = actionIconContentColor,
        subtitleContentColor = subtitleContentColor
    )
}

/** Determines if a color should be considered light or dark.  */
fun isColorLight(color: Color): Boolean {
    return color != Color.Transparent && calculateLuminance(color.toArgb()) > 0.5
}
