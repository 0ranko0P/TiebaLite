package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.MerlotPink

/**
 * Primary:   0xFFE986A7 [MerlotPink]
 * Secondary: 0xFFE99686
 * Tertiary:  0xFFCF265E
 * Neutral:   0xFFEFA9C0
 * */
val PinkColorScheme: ColorSchemeDayNight by lazy {
    ColorSchemeDayNight(
        lightColor = lightColorScheme(
            primary = Color(0xFF8C4A60),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9E2),
            onPrimaryContainer = Color(0xFF703349),
            inversePrimary = Color(0xFFFFB0C8),
            secondary = Color(0xFF904B3E),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFDAD3),
            onSecondaryContainer = Color(0xFF733428),
            tertiary = Color(0xFF8E4958),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD9DE),
            onTertiaryContainer = Color(0xFF713341),
            background = Color(0xFFFFF8F8),
            onBackground = Color(0xFF22191C),
            surface = Color(0xFFFFF8F8),
            onSurface = Color(0xFF22191C),
            surfaceVariant = Color(0xFFEFDFE2),
            onSurfaceVariant = Color(0xFF514347),
            inverseSurface = Color(0xFF372E30),
            inverseOnSurface = Color(0xFFFDEDF0),
            outline = Color(0xFF837377),
            outlineVariant = Color(0xFFD5C2C6),
            surfaceBright = Color(0xFFFFF8F8),
            surfaceContainer = Color(0xFFFAEAED),
            surfaceContainerHigh = Color(0xFFF4E4E7),
            surfaceContainerHighest = Color(0xFFEFDFE2),
            surfaceContainerLow = Color(0xFFFFF0F3),
            surfaceContainerLowest = Color.White,
            surfaceDim = Color(0xFFE6D6D9)
        ),
        darkColor = darkColorScheme(
            primary = Color(0xFFFFB0C8),
            onPrimary = Color(0xFF541D32),
            primaryContainer = Color(0xFF703349),
            onPrimaryContainer = Color(0xFFFFD9E2),
            inversePrimary = Color(0xFF8C4A60),
            secondary = Color(0xFFFFB4A6),
            onSecondary = Color(0xFF561E14),
            secondaryContainer = Color(0xFF733428),
            onSecondaryContainer = Color(0xFFFFDAD3),
            tertiary = Color(0xFFFFB2BF),
            onTertiary = Color(0xFF561D2B),
            tertiaryContainer = Color(0xFF713341),
            onTertiaryContainer = Color(0xFFFFD9DE),
            background = Color(0xFF191114),
            onBackground = Color(0xFFEFDFE2),
            surface = Color(0xFF191114),
            onSurface = Color(0xFFEFDFE2),
            surfaceVariant = Color(0xFF3C3235),
            onSurfaceVariant = Color(0xFFD5C2C6),
            inverseSurface = Color(0xFFEFDFE2),
            inverseOnSurface = Color(0xFF372E30),
            outline = Color(0xFF9E8C90),
            outlineVariant = Color(0xFF514347),
            scrim = Color.Black,
            surfaceBright = Color(0xFF403739),
            surfaceContainer = Color(0xFF261D20),
            surfaceContainerHigh = Color(0xFF31282A),
            surfaceContainerHighest = Color(0xFF3C3235),
            surfaceContainerLow = Color(0xFF22191C),
            surfaceContainerLowest = Color(0xFF130C0E),
            surfaceDim = Color(0xFF191114)
        )
    )
}
