package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.JadeGreen

/**
 * Primary:  0xFF019C74 [JadeGreen]
 * Tertiary: 0xFF01779C
 * Neutral:  0xFF9AFEE5
 * */
val GreenColorScheme: ColorSchemeDayNight by lazy {
    ColorSchemeDayNight(
        lightColor = lightColorScheme(
            primary = Color(0xFF1B6B51),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFA6F2D1),
            onPrimaryContainer = Color(0xFF00513B),
            inversePrimary = Color(0xFF8BD6B6),
            secondary = Color(0xFF4C6358),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFCFE9DA),
            onSecondaryContainer = Color(0xFF354B41),
            tertiary = Color(0xFF166684),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFC0E8FF),
            onTertiaryContainer = Color(0xFF004D66),
            background = Color(0xFFF5FBF7),
            onBackground = Color(0xFF171D1B),
            surface = Color(0xFFF5FBF7),
            onSurface = Color(0xFF171D1B),
            surfaceVariant = Color(0xFFDEE4E0),
            onSurfaceVariant = Color(0xFF404944),
            inverseSurface = Color(0xFF2B3230),
            inverseOnSurface = Color(0xFFECF2EE),
            outline = Color(0xFF707974),
            outlineVariant = Color(0xFFBFC9C2),
            surfaceBright = Color(0xFFF5FBF7),
            surfaceContainer = Color(0xFFE9EFEC),
            surfaceContainerHigh = Color(0xFFE3EAE6),
            surfaceContainerHighest = Color(0xFFDEE4E0),
            surfaceContainerLow = Color(0xFFEFF5F1),
            surfaceContainerLowest = Color.White,
            surfaceDim = Color(0xFFD5DBD8)
        ),
        darkColor = darkColorScheme(
            primary = Color(0xFF8BD6B6),
            onPrimary = Color(0xFF003828),
            primaryContainer = Color(0xFF00513B),
            onPrimaryContainer = Color(0xFFA6F2D1),
            inversePrimary = Color(0xFF1B6B51),
            secondary = Color(0xFFB3CCBF),
            onSecondary = Color(0xFF1F352B),
            secondaryContainer = Color(0xFF354B41),
            onSecondaryContainer = Color(0xFFCFE9DA),
            tertiary = Color(0xFF8DCFF1),
            onTertiary = Color(0xFF003547),
            tertiaryContainer = Color(0xFF004D66),
            onTertiaryContainer = Color(0xFFC0E8FF),
            background = Color(0xFF0E1513),
            onBackground = Color(0xFFDEE4E0),
            surface = Color(0xFF0E1513),
            onSurface = Color(0xFFDEE4E0),
            surfaceVariant = Color(0xFF303634),
            onSurfaceVariant = Color(0xFFBFC9C2),
            inverseSurface = Color(0xFFDEE4E0),
            inverseOnSurface = Color(0xFF2B3230),
            outline = Color(0xFF89938D),
            outlineVariant = Color(0xFF404944),
            scrim = Color.Black,
            surfaceBright = Color(0xFF343B38),
            surfaceContainer = Color(0xFF1B211F),
            surfaceContainerHigh = Color(0xFF252B29),
            surfaceContainerHighest = Color(0xFF303634),
            surfaceContainerLow = Color(0xFF171D1B),
            surfaceContainerLowest = Color(0xFF090F0E),
            surfaceDim = Color(0xFF0E1513)
        )
    )
}
