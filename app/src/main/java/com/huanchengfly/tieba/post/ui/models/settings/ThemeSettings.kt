package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.Variant
import com.huanchengfly.tieba.post.theme.TiebaBlue

enum class Theme {
    TRANSLUCENT, CUSTOM, DYNAMIC, BLUE, GREEN, ORANGE, PINK, PURPLE
}

/**
 * App theme settings
 *
 * @param theme Theme
 * @param customColor seed color of [Theme.CUSTOM]
 * @param customVariant theme variant of [Theme.CUSTOM]
 * @param transColor seed color of [Theme.TRANSLUCENT]
 * @param transDarkColorMode color mode of [Theme.TRANSLUCENT]
 * @param transBackground background image file name of [Theme.TRANSLUCENT]
 * */
data class ThemeSettings(
    val theme: Theme = Theme.BLUE,
    val customColor: Color? = null,
    val customVariant: Variant? = null,
    val transColor: Color = TiebaBlue,
    val transAlpha: Float,
    val transBlur: Float,
    val transDarkColorMode: Boolean = false,
    val transBackground: String? = null
)