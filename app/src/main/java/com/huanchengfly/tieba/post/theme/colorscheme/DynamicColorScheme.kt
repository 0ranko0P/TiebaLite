package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.annotation.FloatRange
import com.google.android.material.color.utilities.Variant
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight

/**
 * Creating ColorScheme dynamically with given source color.
 *
 * @see com.google.android.material.color.utilities.DynamicScheme
 *
 * @param seed source color in ARGB
 * @param variant theme (Variant)
 * @param contrastLevel (-1 to 1, currently contrast ratio 3.0 and 7.0)
 * */
fun dynamicColorScheme(
    seed: Int,
    variant: Variant = Variant.CONTENT,
    @FloatRange(-1.0, 1.0) contrastLevel: Double = 0.0
): ColorSchemeDayNight {
    return ColorSchemeDayNight(
        lightColor = generateColorSchemeFromSeed(seed, variant, false, contrastLevel),
        darkColor = generateColorSchemeFromSeed(seed, variant, true, contrastLevel)
    )
}
