package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.SunsetOrange

/**
 * Primary:   0xFFFD742D [SunsetOrange]
 * Secondary: 0xFFF29421
 * Tertiary:  0xFF9E9245
 * Neutral:   0xFFFE925D
 * */
val OrangeColorScheme: ColorSchemeDayNight by lazy {
    ColorSchemeDayNight(
        lightColor = lightColorScheme(
            primary = Color(0xFF8E4D2F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDBCD),
            onPrimaryContainer = Color(0xFF71361A),
            inversePrimary = Color(0xFFFFB596),
            secondary = Color(0xFF865319),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFDCBE),
            onSecondaryContainer = Color(0xFF693C01),
            tertiary = Color(0xFF675F30),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFEFE3A9),
            onTertiaryContainer = Color(0xFF4E471B),
            background = Color(0xFFFFF8F6),
            onBackground = Color(0xFF221A16),
            surface = Color(0xFFFFF8F6),
            onSurface = Color(0xFF221A16),
            surfaceVariant = Color(0xFFF1DFD8),
            onSurfaceVariant = Color(0xFF53443D),
            inverseSurface = Color(0xFF382E2A),
            inverseOnSurface = Color(0xFFFFEDE6),
            outline = Color(0xFF85736C),
            outlineVariant = Color(0xFFD8C2BA),
            surfaceBright = Color(0xFFFFF8F6),
            surfaceContainer = Color(0xFFFCEAE3),
            surfaceContainerHigh = Color(0xFFF6E5DE),
            surfaceContainerHighest = Color(0xFFF1DFD8),
            surfaceContainerLow = Color(0xFFFFF1EC),
            surfaceContainerLowest = Color.White,
            surfaceDim = Color(0xFFE8D6D0)
        ),
        darkColor = darkColorScheme(
            primary = Color(0xFFFFB596),
            onPrimary = Color(0xFF542106),
            primaryContainer = Color(0xFF71361A),
            onPrimaryContainer = Color(0xFFFFDBCD),
            inversePrimary = Color(0xFF8E4D2F),
            secondary = Color(0xFFFDB975),
            onSecondary = Color(0xFF4A2800),
            secondaryContainer = Color(0xFF693C01),
            onSecondaryContainer = Color(0xFFFFDCBE),
            tertiary = Color(0xFFD2C78F),
            onTertiary = Color(0xFF373106),
            tertiaryContainer = Color(0xFF4E471B),
            onTertiaryContainer = Color(0xFFEFE3A9),
            background = Color(0xFF1A120E),
            onBackground = Color(0xFFF1DFD8),
            surface = Color(0xFF1A120E),
            onSurface = Color(0xFFF1DFD8),
            surfaceVariant = Color(0xFF3D332E),
            onSurfaceVariant = Color(0xFFD8C2BA),
            inverseSurface = Color(0xFFF1DFD8),
            inverseOnSurface = Color(0xFF382E2A),
            outline = Color(0xFFA08D85),
            outlineVariant = Color(0xFF53443D),
            scrim = Color.Black,
            surfaceBright = Color(0xFF423732),
            surfaceContainer = Color(0xFF271E1A),
            surfaceContainerHigh = Color(0xFF322824),
            surfaceContainerHighest = Color(0xFF3D332E),
            surfaceContainerLow = Color(0xFF221A16),
            surfaceContainerLowest = Color(0xFF140C09),
            surfaceDim = Color(0xFF1A120E)
        )
    )
}