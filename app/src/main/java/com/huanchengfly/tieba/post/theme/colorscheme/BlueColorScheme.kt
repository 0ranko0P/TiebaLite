package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.TiebaBlue

/**
 * Primary:  0xFF4477E0 [TiebaBlue]
 * Tertiary: 0xFF5F44E0
 * Neutral:  0xFF739AE8
 * */
val BlueColorScheme: ColorSchemeDayNight = ColorSchemeDayNight(
    lightColor = lightColorScheme(
        primary = Color(0xFF475D92),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD9E2FF),
        onPrimaryContainer = Color(0xFF2F4578),
        inversePrimary = Color(0xFFB0C6FF),
        secondary = Color(0xFF575E71),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE2F9),
        onSecondaryContainer = Color(0xFF404659),
        tertiary = Color(0xFF5F5791),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE5DEFF),
        onTertiaryContainer = Color(0xFF473F77),
        background = Color(0xFFF9F9FF),
        onBackground = Color(0xFF1A1B20),
        surface = Color(0xFFF9F9FF),
        onSurface = Color(0xFF1A1B20),
        surfaceVariant = Color(0xFFE2E2E9),
        onSurfaceVariant = Color(0xFF44464F),
        inverseSurface = Color(0xFF2F3036),
        inverseOnSurface = Color(0xFFF0F0F7),
        outline = Color(0xFF757780),
        outlineVariant = Color(0xFFC5C6D0),
        surfaceBright = Color(0xFFF9F9FF),
        surfaceContainer = Color(0xFFEDEDF4),
        surfaceContainerHigh = Color(0xFFE8E7EE),
        surfaceContainerHighest = Color(0xFFE2E2E9),
        surfaceContainerLow = Color(0xFFF3F3FA),
        surfaceContainerLowest = Color.White,
        surfaceDim = Color(0xFF111318)
    ),
    darkColor = darkColorScheme(
        primary = Color(0xFFB0C6FF),
        onPrimary = Color(0xFF152E60),
        primaryContainer = Color(0xFF2F4578),
        onPrimaryContainer = Color(0xFFD9E2FF),
        inversePrimary = Color(0xFF475D92),
        secondary = Color(0xFFC0C6DC),
        onSecondary = Color(0xFF293042),
        secondaryContainer = Color(0xFF404659),
        onSecondaryContainer = Color(0xFFDCE2F9),
        tertiary = Color(0xFFC8BFFF),
        onTertiary = Color(0xFF30285F),
        tertiaryContainer = Color(0xFF473F77),
        onTertiaryContainer = Color(0xFFE5DEFF),
        background = Color(0xFF111318),
        onBackground = Color(0xFFE2E2E9),
        surface = Color(0xFF111318),
        onSurface = Color(0xFFE2E2E9),
        surfaceVariant = Color(0xFF33353A),
        onSurfaceVariant = Color(0xFFC5C6D0),
        inverseSurface = Color(0xFFE2E2E9),
        inverseOnSurface = Color(0xFF2F3036),
        outline = Color(0xFF8F9099),
        outlineVariant = Color(0xFF44464F),
        scrim = Color.Black,
        surfaceBright = Color(0xFF37393E),
        surfaceContainer = Color(0xFF1E1F25),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353A),
        surfaceContainerLow = Color(0xFF1A1B20),
        surfaceContainerLowest = Color(0xFF0C0E13),
        surfaceDim = Color(0xFF111318)
    )
)