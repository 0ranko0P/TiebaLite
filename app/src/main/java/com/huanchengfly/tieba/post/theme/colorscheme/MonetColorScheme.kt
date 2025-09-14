/*
 * Copyright (C) 2024 Mihon and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://github.com/mihonapp/mihon/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanchengfly.tieba.post.theme.colorscheme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.Variant
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.DynamicSchemes

/**
 * 0Ranko0p changes:
 *   1. extend to ColorSchemeDayNight
 *   2. extract generateColorSchemeFromSeed to public function
 *   3. remove MonetCompatColorScheme
 * */
@RequiresApi(Build.VERSION_CODES.O_MR1)
fun monetColorScheme(context: Context): ColorSchemeDayNight {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ColorSchemeDayNight(
            lightColor = dynamicLightColorScheme(context),
            darkColor = dynamicDarkColorScheme(context)
        )
    } else {
        val seed = WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?.primaryColor
            ?.toArgb()
        if (seed != null) {
            dynamicColorScheme(seed)
        } else {
            BlueColorScheme
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toComposeColor(): Color = Color(this)

fun generateColorSchemeFromSeed(
    seed: Int,
    variant: Variant = Variant.CONTENT,
    dark: Boolean,
    @FloatRange(-1.0, 1.0) contrastLevel: Double = 0.0
): ColorScheme {
    val scheme = DynamicSchemes.from(Hct.fromInt(seed), variant, dark, contrastLevel)

    val dynamicColors = MaterialDynamicColors()
    return ColorScheme(
        primary = dynamicColors.primary().getArgb(scheme).toComposeColor(),
        onPrimary = dynamicColors.onPrimary().getArgb(scheme).toComposeColor(),
        primaryContainer = dynamicColors.primaryContainer().getArgb(scheme).toComposeColor(),
        onPrimaryContainer = dynamicColors.onPrimaryContainer().getArgb(scheme).toComposeColor(),
        inversePrimary = dynamicColors.inversePrimary().getArgb(scheme).toComposeColor(),
        secondary = dynamicColors.secondary().getArgb(scheme).toComposeColor(),
        onSecondary = dynamicColors.onSecondary().getArgb(scheme).toComposeColor(),
        secondaryContainer = dynamicColors.secondaryContainer().getArgb(scheme).toComposeColor(),
        onSecondaryContainer = dynamicColors.onSecondaryContainer().getArgb(scheme).toComposeColor(),
        tertiary = dynamicColors.tertiary().getArgb(scheme).toComposeColor(),
        onTertiary = dynamicColors.onTertiary().getArgb(scheme).toComposeColor(),
        tertiaryContainer = dynamicColors.tertiary().getArgb(scheme).toComposeColor(),
        onTertiaryContainer = dynamicColors.onTertiaryContainer().getArgb(scheme).toComposeColor(),
        background = dynamicColors.background().getArgb(scheme).toComposeColor(),
        onBackground = dynamicColors.onBackground().getArgb(scheme).toComposeColor(),
        surface = dynamicColors.surface().getArgb(scheme).toComposeColor(),
        onSurface = dynamicColors.onSurface().getArgb(scheme).toComposeColor(),
        surfaceVariant = dynamicColors.surfaceVariant().getArgb(scheme).toComposeColor(),
        onSurfaceVariant = dynamicColors.onSurfaceVariant().getArgb(scheme).toComposeColor(),
        surfaceTint = dynamicColors.surfaceTint().getArgb(scheme).toComposeColor(),
        inverseSurface = dynamicColors.inverseSurface().getArgb(scheme).toComposeColor(),
        inverseOnSurface = dynamicColors.inverseOnSurface().getArgb(scheme).toComposeColor(),
        error = dynamicColors.error().getArgb(scheme).toComposeColor(),
        onError = dynamicColors.onError().getArgb(scheme).toComposeColor(),
        errorContainer = dynamicColors.errorContainer().getArgb(scheme).toComposeColor(),
        onErrorContainer = dynamicColors.onErrorContainer().getArgb(scheme).toComposeColor(),
        outline = dynamicColors.outline().getArgb(scheme).toComposeColor(),
        outlineVariant = dynamicColors.outlineVariant().getArgb(scheme).toComposeColor(),
        scrim = Color.Black,
        surfaceBright = dynamicColors.surfaceBright().getArgb(scheme).toComposeColor(),
        surfaceDim = dynamicColors.surfaceDim().getArgb(scheme).toComposeColor(),
        surfaceContainer = dynamicColors.surfaceContainer().getArgb(scheme).toComposeColor(),
        surfaceContainerHigh = dynamicColors.surfaceContainerHigh().getArgb(scheme).toComposeColor(),
        surfaceContainerHighest = dynamicColors.surfaceContainerHighest().getArgb(scheme).toComposeColor(),
        surfaceContainerLow = dynamicColors.surfaceContainerLow().getArgb(scheme).toComposeColor(),
        surfaceContainerLowest = dynamicColors.surfaceContainerLowest().getArgb(scheme).toComposeColor(),
    )
}
