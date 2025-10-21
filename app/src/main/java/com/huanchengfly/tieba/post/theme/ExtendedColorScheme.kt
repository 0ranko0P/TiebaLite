package com.huanchengfly.tieba.post.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val colorScheme: ColorScheme,
    val appBarColors: TopAppBarColors = colorScheme.createTopAppBarColors(),
    val navigationContainer: Color = colorScheme.surfaceContainer,
) {

    val sheetContainerColor: Color
        get() = if (colorScheme.isTranslucent) colorScheme.surfaceContainerLow else navigationContainer
}

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
