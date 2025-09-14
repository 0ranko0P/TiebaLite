package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Variant
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeViewModel.Companion.translucentBackground
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.colorscheme.dynamicColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.translucentColorScheme
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import java.io.File

@Stable
interface AppTheme {
    val name: Int

    val description: Int

    fun getColorScheme(darkMode: Boolean): ColorScheme
}

@Immutable
open class BuiltInTheme(val theme: Theme, val colors: ColorSchemeDayNight): AppTheme {

    override val name: Int = getThemeNameRes(theme)

    override val description: Int
        get() = R.string.summary_settings_custom

    override fun getColorScheme(darkMode: Boolean) = if (darkMode) colors.darkColor else colors.lightColor
}

@Immutable
class TranslucentTheme(settings: ThemeSettings, context: Context): BuiltInTheme(
    theme = Theme.TRANSLUCENT,
    colors = translucentColorScheme(settings.transColor, settings.transDarkColorMode)
) {
    val background: File? = settings.transBackground?.let { context.translucentBackground(it) }
}

@Immutable
class VariantTheme private constructor(
    val variant: Variant,
    val color: Color,
    val colors: ColorSchemeDayNight
): AppTheme {

    override val name: Int = getVariantNameRes(variant)

    override val description: Int = getVariantTitle(variant)

    constructor(color: Color, variant: Variant) : this(
        variant = variant,
        color = color,
        colors = dynamicColorScheme(seed = color.toArgb(), variant = variant)
    )

    override fun getColorScheme(darkMode: Boolean) = if (darkMode) colors.darkColor else colors.lightColor
}

private fun getThemeNameRes(theme: Theme) = when (theme) {
    Theme.TRANSLUCENT -> R.string.theme_translucent
    Theme.CUSTOM -> R.string.theme_tab_custom
    Theme.DYNAMIC -> R.string.theme_dynamic
    Theme.BLUE -> R.string.theme_blue
    Theme.GREEN -> R.string.theme_green
    Theme.ORANGE -> R.string.theme_orange
    Theme.PINK -> R.string.theme_pink
    Theme.PURPLE -> R.string.theme_purple
}

private fun getVariantNameRes(variant: Variant): Int = when (variant) {
    Variant.MONOCHROME -> R.string.variant_monochrome
    Variant.NEUTRAL -> R.string.variant_neutral
    Variant.TONAL_SPOT -> R.string.variant_tonal_spot
    Variant.VIBRANT -> R.string.variant_vibrant
    Variant.EXPRESSIVE -> R.string.variant_expressive
    Variant.FIDELITY,
    Variant.CONTENT -> R.string.variant_fidelity
    Variant.RAINBOW -> R.string.variant_rainbow
    Variant.FRUIT_SALAD -> R.string.variant_fruit_salad
}

private fun getVariantTitle(variant: Variant): Int = when (variant) {
    Variant.MONOCHROME -> R.string.variant_title_monochrome
    Variant.NEUTRAL -> R.string.variant_title_neutral
    Variant.TONAL_SPOT -> R.string.variant_title_tonal_spot
    Variant.VIBRANT -> R.string.variant_title_vibrant
    Variant.EXPRESSIVE -> R.string.variant_title_expressive
    Variant.FIDELITY,
    Variant.CONTENT -> R.string.variant_title_fidelity
    Variant.RAINBOW -> R.string.variant_title_rainbow
    Variant.FRUIT_SALAD -> R.string.variant_title_fruit_salad
}
