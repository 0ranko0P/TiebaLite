package com.huanchengfly.tieba.post.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.core.designsystem.util.createTopAppBarColors
import com.huanchengfly.tieba.post.core.designsystem.util.isTranslucent

@Immutable
data class ExtendedColorScheme(
    val colorScheme: ColorScheme,
    val darkTheme: Boolean,
    val appBarColors: TopAppBarColors = colorScheme.createTopAppBarColors(),
    val navigationContainer: Color = colorScheme.surfaceContainer,
) {

    val sheetContainerColor: Color
        get() = if (colorScheme.isTranslucent) colorScheme.surfaceContainerLow else navigationContainer
}
