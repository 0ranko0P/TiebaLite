package com.huanchengfly.tieba.post.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val colorScheme: ColorScheme,
    val appBarColors: TopBarColors = TopBarColors(colorScheme),
    val navigationContainer: Color = colorScheme.surfaceContainer,
) {
    val sheetContainerColor: Color
        get() = if (colorScheme.isTranslucent) colorScheme.surfaceContainerLow else navigationContainer
}

// TODO: Remove when TopAppBar is stable
class TopBarColors(
    val containerColor: Color,
    val scrolledContainerColor: Color,
    val contentColor: Color
) {
    constructor(colorScheme: ColorScheme): this(
        containerColor = colorScheme.surface,
        scrolledContainerColor = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface
    )

    @OptIn(ExperimentalMaterial3Api::class)
    val topAppBarColors = TopAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = scrolledContainerColor,
        navigationIconContentColor = contentColor,
        titleContentColor = contentColor,
        actionIconContentColor = contentColor
    )

    fun copy(
        containerColor: Color = this.containerColor,
        scrolledContainerColor: Color = this.scrolledContainerColor,
        contentColor: Color = this.contentColor
    ): TopBarColors =
        TopBarColors(containerColor, scrolledContainerColor,  contentColor)
}
