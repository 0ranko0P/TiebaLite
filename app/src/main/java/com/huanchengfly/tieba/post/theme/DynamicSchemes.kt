package com.huanchengfly.tieba.post.theme

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.DislikeAnalyzer
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MathUtils
import com.google.android.material.color.utilities.TemperatureCache
import com.google.android.material.color.utilities.TonalPalette
import com.google.android.material.color.utilities.Variant
import kotlin.math.max

/**
 * com.google.android.material:material 1.14.0-alpha04
 *
 * 0Ranko0p Changes: Merge into util object
 *   0. SchemeContent.java
 *   1. SchemeExpressive.java
 *   2. SchemeFidelity.java
 *   3. SchemeFruitSalad.java
 *   4. SchemeMonochrome.java
 *   5. SchemeNeutral.java
 *   6. SchemeRainbow.java
 *   7. SchemeTonalSpot.java
 *   8. SchemeVibrant.java
 * */
object DynamicSchemes {

    /**
     * Creating colors dynamically, and 6 color palettes with given source color.
     *
     * @see com.google.android.material.color.utilities.DynamicScheme
     *
     * @param sourceColor source color
     * @param variant theme (Variant)
     * @param isDark whether or not its dark mode
     * @param contrastLevel (-1 to 1, currently contrast ratio 3.0 and 7.0)
     */
    fun from(
        sourceColor: Hct,
        variant: Variant,
        isDark: Boolean,
        @FloatRange(-1.0, 1.0) contrastLevel: Double
    ): DynamicScheme {
        return when(variant) {
            Variant.CONTENT -> content(sourceColor, isDark, contrastLevel)

            Variant.EXPRESSIVE -> expressive(sourceColor, isDark, contrastLevel)

            Variant.FIDELITY -> fidelity(sourceColor, isDark, contrastLevel)

            Variant.FRUIT_SALAD -> fruitSalad(sourceColor, isDark, contrastLevel)

            Variant.MONOCHROME -> monochrome(sourceColor, isDark, contrastLevel)

            Variant.NEUTRAL -> neutral(sourceColor, isDark, contrastLevel)

            Variant.RAINBOW -> rainbow(sourceColor, isDark, contrastLevel)

            Variant.TONAL_SPOT -> tonalSpot(sourceColor, isDark, contrastLevel)

            Variant.VIBRANT -> vibrant(sourceColor, isDark, contrastLevel)
        }
    }

    /**
     * Creating colors dynamically, and 6 color palettes with given source color.
     *
     * @see com.google.android.material.color.utilities.DynamicScheme
     *
     * @param sourceColor source color
     * @param variant theme (Variant)
     * @param isDark whether or not its dark mode
     * @param contrastLevel (-1 to 1, currently contrast ratio 3.0 and 7.0)
     */
    fun from(
        sourceColor: Color,
        variant: Variant,
        isDark: Boolean,
        @FloatRange(-1.0, 1.0) contrastLevel: Double
    ): DynamicScheme {
        return from(Hct.fromInt(sourceColor.toArgb()), variant, isDark, contrastLevel)
    }

    /**
     * A scheme that places the source color in Scheme.primaryContainer.
     *
     *
     * Primary Container is the source color, adjusted for color relativity. It maintains constant
     * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
     * dark mode.
     *
     *
     * Tertiary Container is an analogous color, specifically, the analog of a color wheel divided
     * into 6, and the precise analog is the one found by increasing hue. This is a scientifically
     * grounded equivalent to rotating hue clockwise by 60 degrees. It also maintains constant
     * appearance.
     */
    private fun content(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.CONTENT,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma),
        TonalPalette.fromHueAndChroma(
            sourceColorHct.hue,
            max(sourceColorHct.chroma - 32.0, sourceColorHct.chroma * 0.5)
        ),
        TonalPalette.fromHct(
            DislikeAnalyzer.fixIfDisliked(
                TemperatureCache(sourceColorHct)
                    .getAnalogousColors( /* count= */3,  /* divisions= */6)
                    .get(2)
            )
        ),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma / 8.0),
        TonalPalette.fromHueAndChroma(
            sourceColorHct.hue, (sourceColorHct.chroma / 8.0) + 4.0
        )
    )

    // NOMUTANTS--arbitrary increments/decrements, correctly, still passes tests.
    private val EXPRESSIVE_HUES = doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 360.0)

    private val EXPRESSIVE_SECONDARY_ROTATIONS = doubleArrayOf(45.0, 95.0, 45.0, 20.0, 45.0, 90.0, 45.0, 45.0, 45.0)

    private val EXPRESSIVE_TERTIARY_ROTATIONS = doubleArrayOf(120.0, 120.0, 20.0, 45.0, 20.0, 15.0, 20.0, 120.0, 120.0)

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    fun expressive(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.EXPRESSIVE,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(
            MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 240.0), 40.0
        ),
        TonalPalette.fromHueAndChroma(
            DynamicScheme.getRotatedHue(sourceColorHct, EXPRESSIVE_HUES, EXPRESSIVE_SECONDARY_ROTATIONS), 24.0
        ),
        TonalPalette.fromHueAndChroma(
            DynamicScheme.getRotatedHue(sourceColorHct, EXPRESSIVE_HUES, EXPRESSIVE_TERTIARY_ROTATIONS), 32.0
        ),
        TonalPalette.fromHueAndChroma(
            MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 15.0), 8.0
        ),
        TonalPalette.fromHueAndChroma(
            MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 15.0), 12.0
        )
    )

    /**
     * A scheme that places the source color in Scheme.primaryContainer.
     *
     *
     * Primary Container is the source color, adjusted for color relativity. It maintains constant
     * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
     * dark mode.
     *
     *
     * Tertiary Container is the complement to the source color, using TemperatureCache. It also
     * maintains constant appearance.
     */
    private fun fidelity(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.FIDELITY,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma),
        TonalPalette.fromHueAndChroma(
            sourceColorHct.hue,
            max(sourceColorHct.chroma - 32.0, sourceColorHct.chroma * 0.5)
        ),
        TonalPalette.fromHct(
            DislikeAnalyzer.fixIfDisliked(TemperatureCache(sourceColorHct).getComplement())
        ),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma / 8.0),
        TonalPalette.fromHueAndChroma(
            sourceColorHct.hue, (sourceColorHct.chroma / 8.0) + 4.0
        )
    )

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    private fun fruitSalad(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.FRUIT_SALAD,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(
            MathUtils.sanitizeDegreesDouble(sourceColorHct.hue - 50.0), 48.0
        ),
        TonalPalette.fromHueAndChroma(
            MathUtils.sanitizeDegreesDouble(sourceColorHct.hue - 50.0), 36.0
        ),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 36.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 10.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0)
    )

    /**
     * A monochrome theme, colors are purely black / white / gray.
     */
    private fun monochrome(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.MONOCHROME,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
    )

    /**
     * A theme that's slightly more chromatic than monochrome, which is purely black / white / gray.
     */
    private fun neutral(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.NEUTRAL,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 12.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 8.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 2.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 2.0)
    )

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    private fun rainbow(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.RAINBOW,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 48.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0),
        TonalPalette.fromHueAndChroma(
            MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 60.0), 24.0
        ),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
    )

    /**
     * A calm theme, sedated colors that aren't particularly chromatic.
     */
    private fun tonalSpot(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.TONAL_SPOT,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 36.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0),
        TonalPalette.fromHueAndChroma(
            MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 60.0), 24.0
        ),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 6.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 8.0)
    )

    /**
     * A loud theme, colorfulness is maximum for Primary palette, increased for others.
     * */
    private fun vibrant(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) = DynamicScheme(
        sourceColorHct,
        Variant.VIBRANT,
        isDark,
        contrastLevel,
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 200.0),
        TonalPalette.fromHueAndChroma(
            DynamicScheme.getRotatedHue(sourceColorHct, VIBRANT_HUES, VIBRANT_SECONDARY_ROTATIONS), 24.0
        ),
        TonalPalette.fromHueAndChroma(
            DynamicScheme.getRotatedHue(sourceColorHct, VIBRANT_HUES, VIBRANT_TERTIARY_ROTATIONS), 32.0
        ),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 10.0),
        TonalPalette.fromHueAndChroma(sourceColorHct.hue, 12.0)
    )

    private val VIBRANT_HUES = doubleArrayOf(0.0, 41.0, 61.0, 101.0, 131.0, 181.0, 251.0, 301.0, 360.0)

    private val VIBRANT_SECONDARY_ROTATIONS = doubleArrayOf(18.0, 15.0, 10.0, 12.0, 15.0, 18.0, 15.0, 12.0, 12.0)

    private val VIBRANT_TERTIARY_ROTATIONS = doubleArrayOf(35.0, 30.0, 20.0, 25.0, 30.0, 35.0, 30.0, 25.0, 25.0)
}