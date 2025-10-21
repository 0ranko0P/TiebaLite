package com.huanchengfly.tieba.post.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.Typography
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.theme.colorscheme.BlueColorScheme
import com.huanchengfly.tieba.post.utils.ColorUtils

val LocalExtendedColorScheme = staticCompositionLocalOf { DefaultColors }

val DefaultColors = ExtendedColorScheme(BlueColorScheme.lightColor)

val DefaultDarkColors = ExtendedColorScheme(BlueColorScheme.darkColor)

@Composable
fun TiebaLiteTheme(
    colorSchemeExt: ExtendedColorScheme = if (isSystemInDarkTheme()) DefaultDarkColors else DefaultColors,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExtendedColorScheme provides colorSchemeExt,
        LocalWindowAdaptiveInfo provides currentWindowAdaptiveInfo(),
    ) {
        MaterialTheme(colorSchemeExt.colorScheme, shapes, typography, content)
    }
}

val ColorScheme.isTranslucent: Boolean
    get() = surface == Color.Transparent

val ColorScheme.isDarkScheme: Boolean
    get() = ColorUtils.isColorLight(onSurface.toArgb())

/**
 * Contains functions to access the current theme values provided at the call site's position in the
 * hierarchy.
 */
object TiebaLiteTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current.colorScheme

    val extendedColorScheme: ExtendedColorScheme
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current

    val topAppBarColors: TopAppBarColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current.appBarColors

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
}