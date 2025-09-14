package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.theme.BlueViolet
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight

/**
 * Primary:   0xFF8A2BE2 [BlueViolet]
 * Secondary: 0xFF2F2BE2
 * Tertiary:  0xFFE22BDE
 * Neutral:   0xFFB981EE
 * */
val PurpleColorScheme: ColorSchemeDayNight by lazy {
    ColorSchemeDayNight(
        lightColor = lightColorScheme(
            primary = Color(0xFF6E528A),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEFDBFF),
            onPrimaryContainer = Color(0xFF553B71),
            inversePrimary = Color(0xFFDAB9F9),
            secondary = Color(0xFF575992),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE1E0FF),
            onSecondaryContainer = Color(0xFF3F4178),
            tertiary = Color(0xFF804D79),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD7F5),
            onTertiaryContainer = Color(0xFF663560),
            background = Color(0xFFFFF7FE),
            onBackground = Color(0xFF1E1A20),
            surface = Color(0xFFFFF7FE),
            onSurface = Color(0xFF1E1A20),
            surfaceVariant = Color(0xFFE8E0E8),
            onSurfaceVariant = Color(0xFF4A454E),
            inverseSurface = Color(0xFF332F35),
            inverseOnSurface = Color(0xFFF7EEF6),
            outline = Color(0xFF7B757E),
            outlineVariant = Color(0xFFCCC4CE),
            surfaceBright = Color(0xFFFFF7FE),
            surfaceContainer = Color(0xFFF4EBF3),
            surfaceContainerHigh = Color(0xFFEEE6EE),
            surfaceContainerHighest = Color(0xFFE8E0E8),
            surfaceContainerLow = Color(0xFFF9F1F9),
            surfaceContainerLowest = Color.White,
            surfaceDim = Color(0xFFDFD8DF)
        ),
        darkColor = darkColorScheme(
            primary = Color(0xFFDAB9F9),
            onPrimary = Color(0xFF3E2459),
            primaryContainer = Color(0xFF553B71),
            onPrimaryContainer = Color(0xFFEFDBFF),
            inversePrimary = Color(0xFF6E528A),
            secondary = Color(0xFFC0C1FF),
            onSecondary = Color(0xFF292A60),
            secondaryContainer = Color(0xFF3F4178),
            onSecondaryContainer = Color(0xFFE1E0FF),
            tertiary = Color(0xFFF1B3E6),
            onTertiary = Color(0xFF4C1F48),
            tertiaryContainer = Color(0xFF663560),
            onTertiaryContainer = Color(0xFFFFD7F5),
            background = Color(0xFF151217),
            onBackground = Color(0xFFE8E0E8),
            surface = Color(0xFF151217),
            onSurface = Color(0xFFE8E0E8),
            surfaceVariant = Color(0xFF3C3235),
            onSurfaceVariant = Color(0xFFCCC4CE),
            inverseSurface = Color(0xFFE8E0E8),
            inverseOnSurface = Color(0xFF332F35),
            outline = Color(0xFF968E98),
            outlineVariant = Color(0xFF4A454E),
            scrim = Color.Black,
            surfaceBright = Color(0xFF3C383E),
            surfaceContainer = Color(0xFF221E24),
            surfaceContainerHigh = Color(0xFF2C292E),
            surfaceContainerHighest = Color(0xFF373339),
            surfaceContainerLow = Color(0xFF1E1A20),
            surfaceContainerLowest = Color(0xFF100D12),
            surfaceDim = Color(0xFF151217)
        )
    )
}
