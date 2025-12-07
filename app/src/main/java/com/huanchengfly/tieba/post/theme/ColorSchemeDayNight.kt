package com.huanchengfly.tieba.post.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class ColorSchemeDayNight(
    val lightColor: ColorScheme,
    val darkColor: ColorScheme,
) {
    fun getColorScheme(isDark: Boolean, isAmoled: Boolean): ColorScheme = when {

        isDark && isAmoled -> darkColor.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceContainerLowest = Color.Black
        )

        isDark -> darkColor

        else -> lightColor
    }
}