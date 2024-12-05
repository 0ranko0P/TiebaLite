package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.material.color.MaterialColors
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.distinctUntilChangedByKeys
import com.huanchengfly.tieba.post.getColor
import com.huanchengfly.tieba.post.theme.BlackColors
import com.huanchengfly.tieba.post.theme.CustomColors
import com.huanchengfly.tieba.post.theme.DarkAmoledColors
import com.huanchengfly.tieba.post.theme.DarkBlueColors
import com.huanchengfly.tieba.post.theme.DarkGreyColors
import com.huanchengfly.tieba.post.theme.DarkSystemBar
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.LightSystemBar
import com.huanchengfly.tieba.post.theme.PinkColors
import com.huanchengfly.tieba.post.theme.PurpleColors
import com.huanchengfly.tieba.post.theme.RedColors
import com.huanchengfly.tieba.post.theme.TranslucentDark
import com.huanchengfly.tieba.post.theme.TranslucentLight
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_TRANSLUCENT_PRIMARY_COLOR
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil.KEY_CUSTOM_PRIMARY_COLOR
import com.huanchengfly.tieba.post.utils.ThemeUtil.KEY_DARK_THEME
import com.huanchengfly.tieba.post.utils.ThemeUtil.KEY_THEME
import com.huanchengfly.tieba.post.utils.ThemeUtil.KEY_TINT_TOOLBAR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Stable
data class ExtendedColors(
    val theme: String,
    val isNightMode: Boolean,
    val name: Int,
    val primary: Color = Color.Unspecified,
    val onPrimary: Color = Color.Unspecified,
    val secondary: Color = Color.Unspecified,
    val onSecondary: Color = Color.Unspecified,
    val topBar: Color = Color.Unspecified,
    val onTopBar: Color = Color.Unspecified,
    val bottomBar: Color = Color.Unspecified,
    val text: Color = Color.Unspecified,
    val textSecondary: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val chip: Color = Color.Unspecified,
    val onChip: Color = Color.Unspecified,
    val card: Color = Color.Unspecified,
    val floorCard: Color = Color.Unspecified,
    val divider: Color = Color.Unspecified,
    val indicator: Color = Color.Unspecified,
    val windowBackground: Color = Color.Unspecified
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    throw RuntimeException("No ExtendedColors provided!")
}

private fun Color.darken(i: Float = 0.1F): Color {
    return Color(ColorUtils.getDarkerColor(toArgb(), i))
}

@SuppressLint("ConflictingOnColor")
private fun getColorPalette(darkTheme: Boolean, extendedColors: ExtendedColors): Colors {
    return if (darkTheme) {
        darkColors(
            primary = extendedColors.primary,
            primaryVariant = extendedColors.primary.darken(),
            secondary = extendedColors.secondary,
            secondaryVariant = extendedColors.secondary.darken(),
            onPrimary = extendedColors.onPrimary,
            onSecondary = extendedColors.onSecondary,
            background = extendedColors.background,
            onBackground = extendedColors.text,
            surface = extendedColors.card,
        )
    } else {
        lightColors(
            primary = extendedColors.primary,
            primaryVariant = extendedColors.primary.darken(),
            secondary = extendedColors.secondary,
            secondaryVariant = extendedColors.secondary.darken(),
            onPrimary = extendedColors.onPrimary,
            onSecondary = extendedColors.onSecondary,
            background = extendedColors.background,
            onBackground = extendedColors.text,
            surface = extendedColors.card,
        )
    }
}

private fun getDynamicColor(context: Context, dark: Boolean, tintToolbar: Boolean): ExtendedColors {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) {
            getDarkDynamicColor(tonalPalette = dynamicTonalPalette(context))
        } else {
            getLightDynamicColor(tintToolbar, tonalPalette = dynamicTonalPalette(context))
        }
    } else { // Won't happen, just make compiler happy
        throw RuntimeException("Dynamic colors requires Android R+")
    }
}

private fun getLightDynamicColor(tintToolbar: Boolean, tonalPalette: TonalPalette): ExtendedColors {
    return ExtendedColors(
        theme = ThemeUtil.THEME_DYNAMIC,
        isNightMode = false,
        name = R.string.title_dynamic_theme,
        primary = tonalPalette.primary40,
        onPrimary = tonalPalette.primary100,
        secondary = tonalPalette.secondary40,
        onSecondary = tonalPalette.secondary100,
        topBar = if (tintToolbar) tonalPalette.primary40 else tonalPalette.neutralVariant99,
        onTopBar = if (tintToolbar) tonalPalette.primary100 else tonalPalette.neutralVariant10,
        bottomBar = tonalPalette.neutralVariant99,
        text = tonalPalette.neutralVariant10,
        textSecondary = tonalPalette.neutralVariant40,
        background = tonalPalette.neutralVariant99,
        chip = tonalPalette.neutralVariant95,
        onChip = tonalPalette.neutralVariant40,
        card = tonalPalette.neutralVariant99,
        floorCard = tonalPalette.neutralVariant95,
        divider = tonalPalette.neutralVariant95,
        indicator = tonalPalette.neutralVariant95,
        windowBackground = tonalPalette.neutralVariant99
    )
}

private fun getDarkDynamicColor(tonalPalette: TonalPalette): ExtendedColors {
    return ExtendedColors(
        theme = ThemeUtil.THEME_DYNAMIC,
        isNightMode = true,
        name = R.string.title_dynamic_theme,
        primary = tonalPalette.primary80,
        onPrimary = tonalPalette.primary10,
        secondary = tonalPalette.secondary80,
        onSecondary = tonalPalette.secondary20,
        topBar = tonalPalette.neutralVariant10,
        onTopBar = tonalPalette.neutralVariant90,
        bottomBar = tonalPalette.neutralVariant10,
        text = tonalPalette.neutralVariant90,
        textSecondary = tonalPalette.neutralVariant70,
        background = tonalPalette.neutralVariant10,
        chip = tonalPalette.neutralVariant20,
        onChip = tonalPalette.neutralVariant60,
        card = tonalPalette.neutralVariant20,
        floorCard = tonalPalette.neutralVariant20,
        divider = tonalPalette.neutralVariant20,
        indicator = tonalPalette.neutralVariant10,
        windowBackground = tonalPalette.neutralVariant10
    )
}

// Theme provider for Compose Preview
@Composable
fun TiebaLiteTheme(colors: ExtendedColors = DefaultColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides colors) {
        MaterialTheme(
            colors = remember(colors) { getColorPalette(colors.isNightMode, colors) },
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

@Composable
fun TiebaLiteTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var extendedColors: ExtendedColors by ThemeUtil.themeState

    // Initialize theme colors from DataStore now
    LaunchedEffect(darkTheme) {
        savedThemeFlow(context, darkTheme).collect { extendedColors = it }
    }

    TiebaLiteTheme(extendedColors, content = content)
}

private fun savedThemeFlow(context: Context, darkMode: Boolean): Flow<ExtendedColors> {
    return context.dataStore.data
        .distinctUntilChangedByKeys(
            stringPreferencesKey(KEY_THEME),
            stringPreferencesKey(KEY_DARK_THEME),
            booleanPreferencesKey(KEY_TINT_TOOLBAR)
        )
        .map {
            val key = if (darkMode) KEY_DARK_THEME else KEY_THEME
            val theme: String? = it[stringPreferencesKey(key)]
            val tintToolbar = it[booleanPreferencesKey(KEY_TINT_TOOLBAR)] == true

            // Dynamic theme supports both light and dark mode
            val isDynamic = it[stringPreferencesKey(KEY_THEME)] == ThemeUtil.THEME_DYNAMIC
            if (isDynamic) {
                return@map getDynamicColor(context, darkMode, tintToolbar)
            }

            val colors = when (theme) {
                ThemeUtil.THEME_BLACK -> BlackColors
                ThemeUtil.THEME_PINK -> PinkColors
                ThemeUtil.THEME_RED -> RedColors
                ThemeUtil.THEME_PURPLE -> PurpleColors
                ThemeUtil.THEME_BLUE_DARK -> DarkBlueColors
                ThemeUtil.THEME_GREY_DARK -> DarkGreyColors
                ThemeUtil.THEME_AMOLED_DARK -> DarkAmoledColors
                ThemeUtil.THEME_TRANSLUCENT_LIGHT -> TranslucentLight.copy(
                    primary = it.getColor(KEY_TRANSLUCENT_PRIMARY_COLOR)!!
                )

                ThemeUtil.THEME_TRANSLUCENT_DARK -> TranslucentDark.copy(
                    primary = it.getColor(KEY_TRANSLUCENT_PRIMARY_COLOR)!!
                )

                ThemeUtil.THEME_CUSTOM -> CustomColors.copy(
                    primary = it.getColor(KEY_CUSTOM_PRIMARY_COLOR)!!
                )

                else -> if (darkMode) DefaultDarkColors else DefaultColors
            }

            // Ignore TintToolbar on translucent theme
            if (!ThemeUtil.isTranslucentTheme(colors) && tintToolbar) {
                val isLightToolbar = MaterialColors.isColorLight(colors.primary.toArgb())
                colors.copy(
                    topBar = colors.primary,
                    onTopBar = if (isLightToolbar) DarkSystemBar else LightSystemBar
                )
            } else {
                colors
            }
        }
}

object ExtendedTheme {
    val colors: ExtendedColors
        @ReadOnlyComposable
        @Composable
        get() = LocalExtendedColors.current
}
