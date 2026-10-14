package com.huanchengfly.tieba.post.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.Typography
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.huanchengfly.tieba.post.core.designsystem.layout.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.core.designsystem.theme.colorscheme.BlueColorScheme

val DefaultColors = ExtendedColorScheme(BlueColorScheme.lightColor, darkTheme = false)

val DefaultDarkColors = ExtendedColorScheme(BlueColorScheme.darkColor, darkTheme = true)

val LocalExtendedColorScheme = staticCompositionLocalOf { DefaultColors }

/**
 * Tieba Lite theme.
 *
 * @param colorSchemeExt A complete definition of the Material Color theme ext for this hierarchy
 * @param motionScheme A complete definition of the Material motion theme for this hierarchy
 * @param shapes A set of corner shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param content The content inheriting this theme
 */
@Composable
fun TiebaLiteTheme(
    colorSchemeExt: ExtendedColorScheme = if (isSystemInDarkTheme()) DefaultDarkColors else DefaultColors,
    motionScheme: MotionScheme = MotionScheme.expressive(),
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExtendedColorScheme provides colorSchemeExt,
        LocalWindowAdaptiveInfo provides currentWindowAdaptiveInfoV2(),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorSchemeExt.colorScheme,
            motionScheme = motionScheme,
            shapes = shapes,
            typography = typography,
            content = content
        )
    }
}

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