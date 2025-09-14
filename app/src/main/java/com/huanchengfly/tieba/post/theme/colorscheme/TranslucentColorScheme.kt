package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight

// TranslucentColorScheme can not switch Day/Night mode
fun translucentColorScheme(primaryColor: Color, colorMode: Boolean): ColorSchemeDayNight {
    val seed = primaryColor.toArgb()
    return generateColorSchemeFromSeed(seed, dark = !colorMode, contrastLevel = 1.0)
        .copy(
            surfaceDim = Color.Transparent,
            surface = Color.Transparent,
            surfaceContainer = Color.Transparent
        )
        .let {
            ColorSchemeDayNight(lightColor = it, darkColor = it)
        }
}